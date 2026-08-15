"""
SmartCart MCP Server
Exposes the two agent tools (get_order_history, search_products) over stdio
so any MCP client can use them - here, the smartcart-ai-service's
AgentService via langchain-mcp-adapters' MultiServerMCPClient.

Run standalone:  python smartcart_mcp_server.py
Or via client:   MultiServerMCPClient launches this as a subprocess automatically.

Author: Htet Nandar
"""
import sys
import os
from typing import Optional

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from mcp.server.fastmcp import FastMCP
from services import smartcart_tools as st

mcp = FastMCP("SmartCart Tools")


@mcp.tool()
def get_order_history(user_id: int) -> str:
    """Look up a SmartCart user's past orders. Returns their most-bought product
    category, the raw list of categories purchased (most recent first), total
    amount spent, order count, and a short list of recent orders with status
    and totals. Use this to answer questions like 'how much have I spent' or
    to personalize product recommendations.

    user_id: the numeric SmartCart user ID."""
    return st.get_order_history(user_id)


@mcp.tool()
def get_spending_summary(user_id: int) -> str:
    """Look up a SmartCart user's total spending summary - total amount spent, order count,
    most-bought category, and all purchased categories. Does NOT include individual recent
    orders, so no order cards render for this tool's results - use it for pure spending-total
    questions like "how much have I spent" or "show my spending". For "where's my order" /
    order-tracking questions, use get_order_history instead so order cards render.

    user_id: the numeric SmartCart user ID."""
    return st.get_spending_summary(user_id)


@mcp.tool()
def search_products(category: Optional[str] = None, max_price: Optional[float] = None,
                     query: Optional[str] = None, limit: int = 4,
                     newest_first: bool = False) -> str:
    """Search SmartCart's in-stock product catalog. All filters are optional
    and combine with AND. Returns up to `limit` matching products (id, name,
    price, image URL, category). Use this to find real products to show the
    user - never invent product names or prices.

    category: category name to filter by. Ask the user or use search results if
      you're unsure what categories SmartCart currently carries.
    max_price: maximum price in SGD.
    query: free-text search against product name/description.
    limit: max results to return (default 4).
    newest_first: set True when the user asks for "new arrivals", "what's new",
      "latest", etc. - sorts by most recently added instead of default order."""
    return st.search_products(category, max_price, query, limit, newest_first)

@mcp.tool()
def get_cart(user_id: int) -> str:
    """Look up what's currently in a SmartCart user's shopping cart. Returns each
    item (product name, size, quantity, unit price, subtotal) plus the cart total
    and item count. Use this to answer "what's in my cart" / "how much is my cart".

    user_id: the numeric SmartCart user ID."""
    return st.get_cart(user_id)


if __name__ == "__main__":
    mcp.run(transport="stdio")
