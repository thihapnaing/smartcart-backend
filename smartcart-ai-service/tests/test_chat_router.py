# Author: Htet Nandar (Grace)
from unittest.mock import AsyncMock

from fastapi import FastAPI
from fastapi.testclient import TestClient

from routers import chat as chat_router


def _make_client():
    app = FastAPI()
    app.include_router(chat_router.router)
    return TestClient(app)


def test_send_message_returns_503_when_agent_service_not_ready():
    chat_router.agent_service = None
    client = _make_client()

    response = client.post("/api/chat", json={"message": "hi"})

    assert response.status_code == 503


def test_send_message_returns_reply_and_products_from_agent_service():
    fake_agent = AsyncMock()
    fake_agent.chat.return_value = (
        "Here you go!", [{"productId": 1, "name": "Tee"}], [{"orderId": 9, "status": "PACKED"}]
    )
    chat_router.agent_service = fake_agent
    client = _make_client()

    response = client.post("/api/chat", json={
        "session_id": "session-1", "message": "show me tees", "history": [], "user_id": None,
    })

    assert response.status_code == 200
    body = response.json()
    assert body["reply"] == "Here you go!"
    assert body["session_id"] == "session-1"
    assert body["products"][0]["productId"] == 1
    assert body["products"][0]["name"] == "Tee"
    assert body["orders"][0]["orderId"] == 9
    assert body["orders"][0]["status"] == "PACKED"
    fake_agent.chat.assert_awaited_once_with(message="show me tees", history=[], user_id=None)


def test_send_message_forwards_conversation_history_to_agent_service():
    fake_agent = AsyncMock()
    fake_agent.chat.return_value = ("Sure!", None, None)
    chat_router.agent_service = fake_agent
    client = _make_client()

    client.post("/api/chat", json={
        "message": "and cheaper ones?",
        "history": [{"role": "user", "content": "show me tees"}, {"role": "assistant", "content": "Sure!"}],
    })

    fake_agent.chat.assert_awaited_once_with(
        message="and cheaper ones?",
        history=[{"role": "user", "content": "show me tees"}, {"role": "assistant", "content": "Sure!"}],
        user_id=None,
    )


def test_send_message_returns_500_when_agent_service_raises():
    fake_agent = AsyncMock()
    fake_agent.chat.side_effect = RuntimeError("LLM call failed")
    chat_router.agent_service = fake_agent
    client = _make_client()

    response = client.post("/api/chat", json={"message": "hi"})

    assert response.status_code == 500
    assert "LLM call failed" in response.json()["detail"]
