# Author: Htet Nandar (Grace)
import asyncio
from unittest.mock import AsyncMock

from services.agent_service import AgentService


def test_chat_delegates_to_workflow_and_returns_reply_and_products():
    async def _run():
        agent = AgentService()
        agent._workflow = AsyncMock()
        agent._workflow.ainvoke.return_value = {"reply": "Hi!", "products": [{"productId": 1}]}

        reply, products = await agent.chat(
            message="hello", history=[{"role": "user", "content": "hi"}], user_id=7
        )

        assert reply == "Hi!"
        assert products == [{"productId": 1}]
        agent._workflow.ainvoke.assert_awaited_once_with({
            "mode": "chat",
            "message": "hello",
            "history": [{"role": "user", "content": "hi"}],
            "user_id": 7,
            "reply": "", "products": None, "based_on": None,
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


def test_recommend_builds_personalized_message_and_defaults_products_to_empty_list():
    async def _run():
        agent = AgentService()
        agent._workflow = AsyncMock()
        agent._workflow.ainvoke.return_value = {"reply": "Picks for you!", "products": None, "based_on": None}

        reply, products, based_on = await agent.recommend(user_id=9)

        assert reply == "Picks for you!"
        assert products == []
        assert based_on is None
        called_state = agent._workflow.ainvoke.await_args.args[0]
        assert called_state["mode"] == "recommend"
        assert "user ID 9" in called_state["message"]

    asyncio.run(_run())


def test_recommend_returns_products_and_based_on_when_workflow_provides_them():
    async def _run():
        agent = AgentService()
        agent._workflow = AsyncMock()
        agent._workflow.ainvoke.return_value = {
            "reply": "Picks for you!",
            "products": [{"productId": 2}],
            "based_on": 'Your past orders skew toward "Shoes" - showing similar in-stock items.',
        }

        reply, products, based_on = await agent.recommend(user_id=9)

        assert products == [{"productId": 2}]
        assert based_on == 'Your past orders skew toward "Shoes" - showing similar in-stock items.'

    asyncio.run(_run())
