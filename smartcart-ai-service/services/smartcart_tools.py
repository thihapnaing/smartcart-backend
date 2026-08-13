"""
SmartCart agent tools - thin proxies to the Spring Boot backend's
/internal/tools/** endpoints (real JPA queries over the live schema).

Author: Htet Nandar (Grace)
"""
import json
import os
from typing import Any, Optional

import requests

BACKEND_URL = os.getenv("SMARTCART_BACKEND_URL", "http://localhost:8080")


def get_order_history(user_id: int) -> str:
    try:
        resp = requests.get(
            f"{BACKEND_URL}/internal/tools/order-history",
            params={"userId": user_id},
            timeout=10,
        )
        resp.raise_for_status()
        return json.dumps(resp.json())
    except Exception as e:
        return json.dumps({"error": str(e)})


def search_products(category: Optional[str] = None, max_price: Optional[float] = None,
                     query: Optional[str] = None, limit: int = 4,
                     newest_first: bool = False) -> str:
    # Explicit Any-valued type - the literal below infers as dict[str, int] from "limit" alone,
    # which then flags every str/float/bool value assigned to it further down as a type mismatch.
    params: dict[str, Any] = {"limit": limit}
    if category:
        params["category"] = category
    if max_price is not None:
        params["maxPrice"] = max_price
    if query:
        params["query"] = query
    if newest_first:
        params["newestFirst"] = "true"
    try:
        resp = requests.get(
            f"{BACKEND_URL}/internal/tools/products/search",
            params=params,
            timeout=10,
        )
        resp.raise_for_status()
        return json.dumps(resp.json())
    except Exception as e:
        return json.dumps({"error": str(e)})


def get_cart(user_id: int) -> str:
    try:
        resp = requests.get(
            f"{BACKEND_URL}/internal/tools/cart",
            params={"userId": user_id},
            timeout=10,
        )
        resp.raise_for_status()
        return json.dumps(resp.json())
    except Exception as e:
        return json.dumps({"error": str(e)})