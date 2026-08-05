"""
SmartCart Agent Workflow

Single-node LangGraph loop: given `mode` ("chat" or "recommend"), pick the
right system prompt, run the OpenAI tool-calling loop against the MCP tools
(get_order_history, search_products) obtained via langchain-mcp-adapters, and
return a reply plus any product results found along the way.

Must be async throughout: langchain-mcp-adapters' MCP-backed tools only
support .ainvoke() (confirmed by testing - .invoke() raises
"StructuredTool does not support sync invocation"), so the LangGraph node,
the graph invocation, and the OpenAI client all need to be async.

Author: Htet Nandar (Grace)
"""
import json
from typing import Any, TypedDict, Optional, cast

from openai import AsyncOpenAI
from langgraph.graph import StateGraph, START, END
from langchain_core.utils.function_calling import convert_to_openai_function


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
    mode: str                      # "chat" | "recommend"
    message: str
    history: list
    user_id: Optional[int]
    reply: str
    products: Optional[list]
    based_on: Optional[str]


SYSTEM_PROMPTS = {
    "chat": """\
You are SmartCart AI, a friendly, concise shopping assistant inside the SmartCart marketplace app.
Prices are in SGD (S$). SmartCart currently only carries three categories: Tops, Bottoms, Shoes.

Always use the available tools instead of guessing: call get_order_history when the user asks
about spending, past orders, or wants recommendations based on their history; call
search_products before recommending anything - never invent product names or prices.

search_products argument rules - get these right or you will wrongly conclude nothing exists, or
wrongly narrow results to one category the user never asked for:
- `category` must ONLY be set to "Tops", "Bottoms", or "Shoes" when the user's message names or
  clearly implies that specific category (e.g. "sneakers", "a top", "jeans" -> Bottoms). For any
  other request - a price limit ("under $50"), "new arrivals", "what's new", a vague browse request,
  or anything not naming a category - leave `category` unset entirely so results span the whole
  catalog. Do NOT default to "Shoes" (or any single category) just because it was the last one
  listed above; picking a category the user did not ask for is a bug, not a helpful narrowing.
- `query` is a free-text match against product name/description only. Only pass it when the user
  named a specific item, material, or style (e.g. "jeans", "sweater", "sandals"). NEVER pass
  generic words like "outfit", "clothes", "clothing", or the price phrase itself as `query` - doing
  so will incorrectly return zero results even when matching products exist.
- Price limits like "under $50" or "cheaper than $30" go in `max_price`, never in `query`, and by
  themselves do NOT imply any particular category.
- Only when the user explicitly asks for a full "outfit" or "a look" (multiple items, not a single
  product) should you call search_products once per category (category="Tops", "Bottoms", "Shoes")
  - issue those calls together in the SAME turn (multiple tool calls in one response) rather than
  one at a time waiting for each result.
- If the user asks for "best rated", "picks for me", "recommended for me", or similar personalized
  suggestions WITHOUT naming a specific category, call get_order_history first to see what category
  they buy most (topCategory), then call search_products with category set to that topCategory. If
  there is no order history (no user logged in, or no past orders), fall back to leaving `category`
  unset so results span the whole catalog - never default this to "Shoes" or any other fixed guess.
- IMPORTANT special case: the message "Best shoes for me" is a fixed UI suggestion-chip label
  meaning "act like my personal shopping assistant and recommend shoes for me" - always call
  search_products with category="Shoes" (the products shown must be real shoes, never swapped for
  another category). ALSO call get_order_history first so you know what tops/bottoms the user has
  bought before, and write your one-sentence reply so it connects the two - e.g. "Since you've been
  loving our tees, here are some shoes that would pair nicely!" If there's no order history, just
  give a friendly generic shoes reply without that personal touch.

The app shows matching products as cards below your reply automatically (name, price, image) -
you do not need to and must not list them yourself. Do not use markdown, bullet points, numbered
lists, bold text, or image links in your reply. Just write one short, plain-text conversational
sentence or two (no formatting at all) - e.g. "Here are a few tees and shorts under S$50 for you!"
If a tool returns no results after following the rules above, say so honestly in the same plain
style - do not blame the store's stock without having actually tried category-specific searches.""",

    "recommend": """\
You are SmartCart AI's recommendation engine. Given a user ID, call get_order_history to see
what category they buy most, then call search_products (filtered to that category, limit 4) to
find real in-stock items to recommend. Prices are SGD (S$). Reply with exactly one short,
upbeat sentence introducing the picks - do not list the products yourself, the caller already
has them as structured data.""",
}


def _extract_products(tool_result_text: str) -> Optional[list]:
    try:
        data = json.loads(tool_result_text)
        products = data.get("products")
        return products if isinstance(products, list) and products else None
    except Exception:
        return None


def _extract_based_on(tool_result_text: str) -> Optional[str]:
    try:
        data = json.loads(tool_result_text)
        top_category = data.get("topCategory")
        if top_category:
            return f'Your past orders skew toward "{top_category}" - showing similar in-stock items.'
    except Exception:
        pass
    return None


async def _run_agent(state: SmartCartState, client: AsyncOpenAI, model: str, openai_tools: list[dict], tool_registry: dict) -> dict:
    mode = state.get("mode", "chat")
    system_prompt = SYSTEM_PROMPTS.get(mode, SYSTEM_PROMPTS["chat"])

    if state.get("user_id") is not None:
        system_prompt += f"\n\nThe current user's ID is {state['user_id']} - use it when calling get_order_history."
    else:
        system_prompt += "\n\nNo user is logged in - skip get_order_history and just help with product search."

    # Any-valued list - later holds both plain role/content dicts and raw
    # ChatCompletionMessage objects (appended straight from the API response,
    # which the OpenAI SDK accepts as a message param), so a narrower dict-only
    # type would misflag those appends as errors.
    messages: list[Any] = [{"role": "system", "content": system_prompt}]
    for msg in state.get("history") or []:
        messages.append(msg if isinstance(msg, dict) else {"role": msg.role, "content": msg.content})
    messages.append({"role": "user", "content": state["message"]})

    products = None
    based_on = None

    for _ in range(MAX_ITERATIONS):
        response = await client.chat.completions.create(
            model=model,
            messages=messages,
            # cast: openai_tools is built by _to_openai_tools() as plain dicts shaped exactly
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
            return {"reply": assistant_msg.content, "products": products, "based_on": based_on}

        messages.append(assistant_msg)

        for tc in assistant_msg.tool_calls:
            fn = tc.function.name
            try:
                if fn in tool_registry:
                    args = json.loads(tc.function.arguments or "{}")
                    print(f"[Tool]   calling {fn}({args})")
                    raw_result = await tool_registry[fn].ainvoke(args)
                    tool_result = _tool_result_to_text(raw_result)

                    if fn == "search_products":
                        found = _extract_products(tool_result)
                        if found:
                            # Accumulate across calls rather than overwrite - an "outfit" or
                            # "under $50" request issues one search_products call per category
                            # in the same turn, and every category's results should show up
                            # together, not just whichever call happened to run last.
                            if products is None:
                                products = []
                            seen_ids = {p.get("productId") for p in products}
                            for p in found:
                                if p.get("productId") not in seen_ids:
                                    products.append(p)
                                    seen_ids.add(p.get("productId"))
                    if fn == "get_order_history":
                        found = _extract_based_on(tool_result)
                        if found:
                            based_on = found
                else:
                    tool_result = f"Tool '{fn}' not available."
            except Exception as e:
                tool_result = json.dumps({"error": str(e)})
            messages.append({"role": "tool", "tool_call_id": tc.id, "content": tool_result})

    messages.append({"role": "user", "content": "Please give your final answer based on what you've found."})
    fallback = await client.chat.completions.create(model=model, messages=messages, temperature=0.7, max_tokens=200)
    return {"reply": fallback.choices[0].message.content, "products": products, "based_on": based_on}


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
                "based_on": None,
            }
        return await _run_agent(state, client, model, openai_tools, tool_registry)

    builder = StateGraph(SmartCartState)
    builder.add_node("agent", agent_node)
    builder.add_edge(START, "agent")
    builder.add_edge("agent", END)
    return builder.compile()
