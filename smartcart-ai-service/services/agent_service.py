"""
SmartCart AI Service - Agentic entry point.

Flow:
  chat(message, history, user_id)
    │
    └─ [LangGraph Workflow] (services/workflow.py)
         agent_node → OpenAI tool-calling loop, filtered to the two MCP tools
                     → reply (+ products / orders / based_on when tools were used)

The MCP tools (get_order_history, search_products) are loaded once at
startup from smartcart_mcp_server.py, spawned as a subprocess via
langchain-mcp-adapters' MultiServerMCPClient.

Author: Htet Nandar (Grace)
"""
import os
import sys


class AgentService:
    """SmartCart AI agent backed by a single-node LangGraph tool-calling loop."""

    def __init__(self):
        self._workflow = None

    async def start(self):
        from services.workflow import build_smartcart_workflow, OPENROUTER_BASE

        # ── Resolve API key / base URL ─────────────────────────────────────
        openrouter_key = os.getenv("OPENROUTER_API_KEY")
        openai_key = os.getenv("OPENAI_API_KEY")
        if openrouter_key:
            api_key = openrouter_key
            base_url = OPENROUTER_BASE
            print(f"[AI] Using OpenRouter key: {openrouter_key[:8]}...{openrouter_key[-4:]}")
        elif openai_key:
            api_key = openai_key
            base_url = None
            print(f"[AI] Using OpenAI key: {openai_key[:8]}...{openai_key[-4:]}")
        else:
            api_key = None
            base_url = None
            print("[AI] WARNING: No API key found - set OPENAI_API_KEY or OPENROUTER_API_KEY in .env")

        # ── Load tools from the SmartCart MCP server ────────────────────────
        from langchain_mcp_adapters.client import MultiServerMCPClient
        server_path = os.path.abspath(
            os.path.join(os.path.dirname(__file__), "..", "smartcart_mcp_server.py")
        )
        print("[DEBUG] python command:", sys.executable)
        print("[DEBUG] server path:", server_path, "| exists:", os.path.exists(server_path))

        mcp_client = MultiServerMCPClient({
            "smartcart": {
                "command": sys.executable,
                "args": [server_path],
                "transport": "stdio",
            }
        })

        mcp_tools = await mcp_client.get_tools()
        print(f"[AI] MCP tools available: {[t.name for t in mcp_tools]}")

        self._workflow = build_smartcart_workflow(
            api_key=api_key, base_url=base_url, mcp_tools=mcp_tools
        )

    # ── Public entry points (chat) ────────────────────────────────

    async def chat(self, message: str, history: list, user_id: int | None) -> tuple[str, list | None, list | None]:
        result = await self._workflow.ainvoke({
            "message": message,
            "history": history or [],
            "user_id": user_id,
            "reply": "", "products": None, "orders": None, "based_on": None,
        })
        return result["reply"], result.get("products"), result.get("orders")
