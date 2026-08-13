# Author: Htet Nandar (Grace)
import asyncio
import json
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock

from services.workflow import (
    MAX_ITERATIONS,
    _extract_based_on,
    _extract_products,
    _run_agent,
    _tool_result_to_text,
    build_smartcart_workflow,
)


# ── _tool_result_to_text ─────────────────────────────────────────────────────

def test_tool_result_to_text_passes_through_plain_strings():
    assert _tool_result_to_text("already text") == "already text"


def test_tool_result_to_text_joins_text_blocks_from_content_list():
    result = _tool_result_to_text([
        {"type": "text", "text": "line one"},
        {"type": "text", "text": "line two"},
    ])
    assert result == "line one\nline two"


def test_tool_result_to_text_falls_back_to_json_when_no_text_blocks():
    blocks = [{"type": "image", "data": "..."}]
    assert _tool_result_to_text(blocks) == json.dumps(blocks)


def test_tool_result_to_text_json_dumps_other_types():
    assert _tool_result_to_text({"a": 1}) == json.dumps({"a": 1})


# ── _extract_products / _extract_based_on ────────────────────────────────────

def test_extract_products_returns_list_when_present_and_nonempty():
    text = json.dumps({"products": [{"productId": 1}]})
    assert _extract_products(text) == [{"productId": 1}]


def test_extract_products_returns_none_when_products_list_is_empty():
    assert _extract_products(json.dumps({"products": []})) is None


def test_extract_products_returns_none_on_invalid_json():
    assert _extract_products("not json") is None


def test_extract_based_on_formats_top_category_message():
    text = json.dumps({"topCategory": "Tops"})
    assert _extract_based_on(text) == 'Your past orders skew toward "Tops" - showing similar in-stock items.'


def test_extract_based_on_returns_none_when_no_top_category():
    assert _extract_based_on(json.dumps({})) is None


def test_extract_based_on_returns_none_on_invalid_json():
    assert _extract_based_on("not json") is None


# ── _run_agent (the OpenAI tool-calling loop) ────────────────────────────────

class _FakeToolCall:
    def __init__(self, call_id, name, arguments):
        self.id = call_id
        self.function = SimpleNamespace(name=name, arguments=arguments)


class _FakeMessage:
    def __init__(self, content=None, tool_calls=None):
        self.content = content
        self.tool_calls = tool_calls or []


class _FakeCompletion:
    def __init__(self, message):
        self.choices = [SimpleNamespace(message=message)]


def _base_state(**overrides):
    state = {"message": "hi", "history": [], "user_id": None}
    state.update(overrides)
    return state


def test_run_agent_returns_reply_directly_when_model_makes_no_tool_calls():
    async def _run():
        client = Mock()
        client.chat.completions.create = AsyncMock(
            return_value=_FakeCompletion(_FakeMessage(content="Hello there!"))
        )

        result = await _run_agent(_base_state(), client, "gpt-4o-mini", [], {})

        assert result == {"reply": "Hello there!", "products": None, "orders": None, "based_on": None}
        client.chat.completions.create.assert_awaited_once()

    asyncio.run(_run())


def test_run_agent_accumulates_products_from_search_products_tool_call():
    async def _run():
        client = Mock()
        first_call = _FakeCompletion(_FakeMessage(tool_calls=[
            _FakeToolCall("call_1", "search_products", json.dumps({"category": "Tops"}))
        ]))
        second_call = _FakeCompletion(_FakeMessage(content="Here are some tees!"))
        client.chat.completions.create = AsyncMock(side_effect=[first_call, second_call])

        search_tool = Mock()
        search_tool.ainvoke = AsyncMock(return_value=json.dumps({
            "products": [{"productId": 1, "name": "Tee"}]
        }))
        tool_registry = {"search_products": search_tool}

        result = await _run_agent(
            _base_state(message="show me tops"), client, "gpt-4o-mini", [{"type": "function"}], tool_registry
        )

        assert result["reply"] == "Here are some tees!"
        assert result["products"] == [{"productId": 1, "name": "Tee"}]
        search_tool.ainvoke.assert_awaited_once_with({"category": "Tops"})

    asyncio.run(_run())


def test_run_agent_sets_based_on_from_get_order_history_tool_call():
    async def _run():
        client = Mock()
        first_call = _FakeCompletion(_FakeMessage(tool_calls=[
            _FakeToolCall("call_1", "get_order_history", json.dumps({"user_id": 5}))
        ]))
        second_call = _FakeCompletion(_FakeMessage(content="Based on your history..."))
        client.chat.completions.create = AsyncMock(side_effect=[first_call, second_call])

        history_tool = Mock()
        history_tool.ainvoke = AsyncMock(return_value=json.dumps({"topCategory": "Shoes"}))
        tool_registry = {"get_order_history": history_tool}

        result = await _run_agent(
            _base_state(message="recommend", user_id=5), client, "gpt-4o-mini", [{}], tool_registry
        )

        assert result["based_on"] == 'Your past orders skew toward "Shoes" - showing similar in-stock items.'

    asyncio.run(_run())


def test_run_agent_handles_unregistered_tool_gracefully():
    async def _run():
        client = Mock()
        first_call = _FakeCompletion(_FakeMessage(tool_calls=[
            _FakeToolCall("call_1", "unknown_tool", "{}")
        ]))
        second_call = _FakeCompletion(_FakeMessage(content="Sorry, I can't do that."))
        client.chat.completions.create = AsyncMock(side_effect=[first_call, second_call])

        result = await _run_agent(_base_state(message="do something weird"), client, "gpt-4o-mini", [{}], {})

        assert result["reply"] == "Sorry, I can't do that."

    asyncio.run(_run())


def test_run_agent_falls_back_after_max_iterations_of_tool_calls():
    async def _run():
        client = Mock()
        looping_call = _FakeCompletion(_FakeMessage(tool_calls=[
            _FakeToolCall("call_1", "search_products", "{}")
        ]))
        fallback_call = _FakeCompletion(_FakeMessage(content="Here's my best guess."))
        client.chat.completions.create = AsyncMock(
            side_effect=[looping_call] * MAX_ITERATIONS + [fallback_call]
        )

        search_tool = Mock()
        search_tool.ainvoke = AsyncMock(return_value=json.dumps({"products": []}))
        tool_registry = {"search_products": search_tool}

        result = await _run_agent(_base_state(message="keep going"), client, "gpt-4o-mini", [{}], tool_registry)

        assert result["reply"] == "Here's my best guess."
        assert client.chat.completions.create.await_count == MAX_ITERATIONS + 1

    asyncio.run(_run())


# ── build_smartcart_workflow ──────────────────────────────────────────────────

def test_build_smartcart_workflow_returns_setup_message_when_no_api_key_configured():
    async def _run():
        graph = build_smartcart_workflow(api_key=None)

        result = await graph.ainvoke({
            "message": "hi", "history": [], "user_id": None,
            "reply": "", "products": None, "based_on": None,
        })

        assert "isn't configured yet" in result["reply"]
        assert result["products"] is None

    asyncio.run(_run())
