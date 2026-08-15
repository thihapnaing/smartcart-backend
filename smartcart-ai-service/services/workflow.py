"""
SmartCart Agent Workflow

Single-node LangGraph loop: run the OpenAI tool-calling loop against the MCP tools
(get_order_history, search_products) obtained via langchain-mcp-adapters, and
return a reply plus any product results found along the way.

Must be async throughout: langchain-mcp-adapters MCP-backed tools only
support .ainvoke() (confirmed by testing - .invoke() raises
"StructuredTool does not support sync invocation"), so the LangGraph node,
the graph invocation, and the OpenAI client all need to be async.

Author: Htet Nandar (Grace)
"""
import asyncio
import json
from typing import Any, TypedDict, Optional, cast

from openai import AsyncOpenAI
from langgraph.graph import StateGraph, START, END
from langchain_core.utils.function_calling import convert_to_openai_function

from prompts.chat_prompt import SYSTEM_PROMPT


def _to_openai_tools(lc_tools: list) -> list[dict]:
    result: list[dict] = []
    for tool in lc_tools:
        schema = convert_to_openai_function(tool)
        result.append({"type": "function", "function": schema})
    return result


def _tool_result_to_text(result) -> str:
    """MCP tool calls via langchain-mcp-adapters return a list of content
    blocks (e.g. [{'type': 'text', 'text': '...'}]), not a plain string -
    confirmed by testing against a real MCP server round trip."""
    if isinstance(result, str):
        return result
    if isinstance(result, list):
        texts = [b.get("text", "") for b in result if isinstance(b, dict) and b.get("type") == "text"]
        if texts:
            return "\n".join(texts)
        return json.dumps(result)
    return json.dumps(result)


OPENROUTER_BASE = "https://openrouter.ai/api/v1"
CHAT_MODEL_OPENROUTER = "openai/gpt-4o-mini"
CHAT_MODEL_OPENAI = "gpt-4o-mini"
MAX_ITERATIONS = 5


class SmartCartState(TypedDict):
    message: str
    history: list
    user_id: Optional[int]
    reply: str
    products: Optional[list]
    orders: Optional[list]
    based_on: Optional[str]


def _extract_products(tool_result_text: str) -> Optional[list]:
    try:
        data = json.loads(tool_result_text)
        products = data.get("products")
        return products if isinstance(products, list) and products else None
    except Exception:
        return None


def _extract_orders(tool_result_text: str) -> Optional[list]:
    """Pulls get_order_history's recentOrders list out for the chat UI to render as order
    cards (orderId, totalAmount, status, orderDate) - same pattern as _extract_products."""
    try:
        data = json.loads(tool_result_text)
        orders = data.get("recentOrders")
        return orders if isinstance(orders, list) and orders else None
    except Exception:
        return None


def _visible_orders(products: Optional[list], orders: Optional[list]) -> Optional[list]:
    """get_order_history gets called for two different reasons: to actually answer an
    order-tracking/spending question (orders SHOULD show as cards), and just to read
    topCategory for personalizing product picks or the "Best shoes for me" special case
    (orders should stay invisible - the user asked for products, not their order status).
    None of the prompt's rules ever want both shown at once, so: if products were also
    found in this reply, the order data was only fetched for context - suppress the cards."""
    return orders if not products else None


def _extract_based_on(tool_result_text: str) -> Optional[str]:
    try:
        data = json.loads(tool_result_text)
        top_category = data.get("topCategory")
        if top_category:
            return f'Your past orders skew toward "{top_category}" - showing similar in-stock items.'
    except Exception:
        pass
    return None


def _reply_promises_order_cards(reply: Optional[str]) -> bool:
    """The prompt tells the model to answer order-tracking questions with a short line like
    "Here's what I found!" or "You've got 2 recent orders - check them out below" and rely on
    the app to render order cards underneath. Conversation history is plain text, so on a
    follow-up turn ("wherer", "I want to know my latest order") the model sometimes writes that
    same canned reply from memory without actually re-invoking get_order_history THIS turn -
    there's no structured data behind it, and no card renders.

    "Here's what I found!" alone (no "order" mention) is one of only two example replies the
    prompt gives for get_order_history, so it's checked as a standalone signal, not just
    "order" + below/check-them-out - an earlier version of this check missed exactly this
    phrasing. Curly apostrophes (the model often writes '’' rather than a plain "'") are
    normalized first so the match isn't accidentally case-sensitive to punctuation style."""
    if not reply:
        return False
    text = reply.lower().replace("’", "'").replace("‘", "'")
    if "here's what i found" in text:
        return True
    return "order" in text and ("below" in text or "check them out" in text)


async def _ensure_orders_if_promised(
    reply: Optional[str],
    products: Optional[list],
    orders: Optional[list],
    based_on: Optional[str],
    user_id: Optional[int],
    tool_registry: dict,
) -> tuple[Optional[list], Optional[str]]:
    """Safety net for the failure mode _reply_promises_order_cards() describes: if nothing was
    fetched this turn (no products either - that path already renders its own cards and orders
    would be intentionally suppressed by _visible_orders) but the reply promises order cards
    anyway, fetch get_order_history directly - once, deterministically - rather than let the
    promise silently go unfulfilled. Leaves the reply text untouched; it already reads fine."""
    if products is not None or orders is not None or user_id is None:
        return orders, based_on
    if not _reply_promises_order_cards(reply):
        return orders, based_on

    tool = tool_registry.get("get_order_history")
    if tool is None:
        return orders, based_on

    try:
        print(f"[Workflow] Reply promised order cards but none were fetched this turn - "
              f"calling get_order_history({user_id}) as a fallback.")
        raw_result = await tool.ainvoke({"user_id": user_id})
        tool_result = _tool_result_to_text(raw_result)
    except Exception:
        return orders, based_on

    return _extract_orders(tool_result) or orders, _extract_based_on(tool_result) or based_on


async def _call_tool(tc, tool_registry: dict) -> tuple[str, str, str]:
    """Runs a single tool call and returns (tool_call_id, function_name, result_text).
    Errors are caught per-call so one failing/slow tool doesn't take down the others
    running alongside it via asyncio.gather in _run_agent()."""
    fn = tc.function.name
    try:
        if fn in tool_registry:
            args = json.loads(tc.function.arguments or "{}")
            print(f"[Tool]   calling {fn}({args})")
            raw_result = await tool_registry[fn].ainvoke(args)
            tool_result = _tool_result_to_text(raw_result)
        else:
            tool_result = f"Tool '{fn}' not available."
    except Exception as e:
        tool_result = json.dumps({"error": str(e)})
    return tc.id, fn, tool_result


async def _run_agent(state: SmartCartState, client: AsyncOpenAI, model: str, openai_tools: list[dict], tool_registry: dict) -> dict:
    system_prompt = SYSTEM_PROMPT

    if state.get("user_id") is not None:
        system_prompt += f"\n\nThe current user's ID is {state['user_id']} - use it when calling get_order_history."
    else:
        system_prompt += "\n\nNo user is logged in - skip get_order_history and just help with product search."

    # Any-valued list - later holds both plain role/content dicts and raw
    # ChatCompletionMessage objects (appended straight from the API response,
    # which the OpenAI SDK accepts as a message param), so a narrower dict-only
    # type would misflag as errors.
    messages: list[Any] = [{"role": "system", "content": system_prompt}]
    for msg in state.get("history") or []:
        messages.append(msg if isinstance(msg, dict) else {"role": msg.role, "content": msg.content})
    messages.append({"role": "user", "content": state["message"]})

    user_id = state.get("user_id")
    products = None
    orders = None
    based_on = None

    for _ in range(MAX_ITERATIONS):
        response = await client.chat.completions.create(
            model=model,
            messages=messages,
            # cast: OpenAI_tools is built by _to_OpenAI_tools() as plain dicts shaped exactly
            # like ChatCompletionToolParam ({"type": "function", "function": {...}}), and passing
            # None here (vs. omitting the kwarg) is already verified working against both the
            # OpenAI and OpenRouter APIs - this only silences a structural-typing mismatch between
            # our plain-dict tool schemas and the SDK's exact TypedDict/Omit-sentinel stub.
            tools=cast(Any, openai_tools if openai_tools else None),
            tool_choice="auto" if openai_tools else "none",
            temperature=0.7,
            max_tokens=200,  # replies are meant to be one short sentence - caps tail latency
        )
        assistant_msg = response.choices[0].message

        if not assistant_msg.tool_calls:
            orders, based_on = await _ensure_orders_if_promised(
                assistant_msg.content, products, orders, based_on, user_id, tool_registry,
            )
            return {
                "reply": assistant_msg.content, "products": products,
                "orders": _visible_orders(products, orders), "based_on": based_on,
            }

        messages.append(assistant_msg)

        # Run every tool call from this turn concurrently rather than one at a time. An
        # "outfit" or "under $50" request alone issues 3 independent search_products calls
        # (Tops/Bottoms/Shoes) - awaiting them sequentially was adding several seconds of
        # pure round-trip latency per chat message for calls that don't depend on each other.
        results = await asyncio.gather(*(_call_tool(tc, tool_registry) for tc in assistant_msg.tool_calls))

        for tc_id, fn, tool_result in results:
            if fn == "search_products":
                found = _extract_products(tool_result)
                if found:
                    # Accumulate across calls rather than overwrite - every category's
                    # results should show up together, not just whichever call finished last.
                    if products is None:
                        products = []
                    seen_ids = {p.get("productId") for p in products}
                    for p in found:
                        if p.get("productId") not in seen_ids:
                            products.append(p)
                            seen_ids.add(p.get("productId"))
            if fn == "get_order_history":
                found_orders = _extract_orders(tool_result)
                if found_orders:
                    orders = found_orders
                found_based_on = _extract_based_on(tool_result)
                if found_based_on:
                    based_on = found_based_on
            messages.append({"role": "tool", "tool_call_id": tc_id, "content": tool_result})

    messages.append({"role": "user", "content": "Please give your final answer based on what you've found."})
    fallback = await client.chat.completions.create(model=model, messages=messages, temperature=0.7, max_tokens=200)
    orders, based_on = await _ensure_orders_if_promised(
        fallback.choices[0].message.content, products, orders, based_on, user_id, tool_registry,
    )
    return {
        "reply": fallback.choices[0].message.content, "products": products,
        "orders": _visible_orders(products, orders), "based_on": based_on,
    }


def build_smartcart_workflow(api_key: Optional[str], base_url: Optional[str] = None, mcp_tools: Optional[list] = None):
    # Don't let a missing key crash the whole service at startup (the MCP
    # tool connection should still come up so /api/health and tool listing
    # work) - just fail clearly, per-request, when the workflow actually runs.
    client = AsyncOpenAI(api_key=api_key, base_url=base_url) if api_key else None
    model = CHAT_MODEL_OPENROUTER if base_url else CHAT_MODEL_OPENAI

    openai_tools = _to_openai_tools(mcp_tools or [])
    tool_registry = {t.name: t for t in (mcp_tools or [])}
    print(f"[Workflow] MCP tools loaded: {list(tool_registry.keys())}")

    async def agent_node(state: SmartCartState) -> dict:
        if client is None:
            return {
                "reply": "SmartCart AI isn't configured yet - set OPENAI_API_KEY (or "
                         "OPENROUTER_API_KEY) in smartcart-ai-service's .env file and "
                         "restart it.",
                "products": None,
                "orders": None,
                "based_on": None,
            }
        return await _run_agent(state, client, model, openai_tools, tool_registry)

    builder = StateGraph(SmartCartState)
    builder.add_node("agent", agent_node)
    builder.add_edge(START, "agent")
    builder.add_edge("agent", END)
    return builder.compile()
