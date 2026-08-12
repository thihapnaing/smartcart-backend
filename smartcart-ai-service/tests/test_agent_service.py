# Author: Htet Nandar (Grace)
import asyncio
from unittest.mock import AsyncMock

from services.agent_service import AgentService


def test_chat_delegates_to_workflow_and_returns_reply_and_products():
    async def _run():
        agent = AgentService()
        agent._workflow = AsyncMock()
        agent._workflow.ainvoke.return_value = {
            "reply": "Hi!", "products": [{"productId": 1}], "orders": [{"orderId": 5}]
        }

        reply, products, orders = await agent.chat(
            message="hello", history=[{"role": "user", "content": "hi"}], user_id=7
        )

        assert reply == "Hi!"
        assert products == [{"productId": 1}]
        assert orders == [{"orderId": 5}]
        agent._workflow.ainvoke.assert_awaited_once_with({
            "message": "hello",
            "history": [{"role": "user", "content": "hi"}],
            "user_id": 7,
            "reply": "", "products": None, "orders": None, "based_on": None,
        })

    asyncio.run(_run())


def test_chat_defaults_history_to_empty_list_when_none_given():
    async def _run():
        agent = AgentService()
        agent._workflow = AsyncMock()
        agent._workflow.ainvoke.return_value = {"reply": "Hi!", "products": None}

        await agent.chat(message="hello", history=None, user_id=None)

        called_state = agent._workflow.ainvoke.await_args.args[0]
        assert called_state["history"] == []

    asyncio.run(_run())
