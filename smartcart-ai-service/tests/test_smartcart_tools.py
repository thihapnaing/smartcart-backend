# Author: Htet Nandar (Grace)
import json
from unittest.mock import patch

from services import smartcart_tools as st


class _FakeResponse:
    def __init__(self, json_data, status_code=200):
        self._json_data = json_data
        self.status_code = status_code

    def raise_for_status(self):
        if self.status_code >= 400:
            raise RuntimeError(f"HTTP {self.status_code}")

    def json(self):
        return self._json_data


@patch("services.smartcart_tools.requests.get")
def test_get_order_history_returns_json_string_of_backend_response(mock_get):
    mock_get.return_value = _FakeResponse({"orderCount": 3, "topCategory": "Tops"})

    result = st.get_order_history(42)

    assert json.loads(result) == {"orderCount": 3, "topCategory": "Tops"}
    mock_get.assert_called_once_with(
        "http://localhost:8080/internal/tools/order-history",
        params={"userId": 42},
        timeout=10,
    )


@patch("services.smartcart_tools.requests.get")
def test_get_order_history_returns_error_json_when_backend_call_fails(mock_get):
    mock_get.side_effect = ConnectionError("backend down")

    result = st.get_order_history(42)

    assert json.loads(result) == {"error": "backend down"}


@patch("services.smartcart_tools.requests.get")
def test_get_spending_summary_strips_recent_orders_from_backend_response(mock_get):
    mock_get.return_value = _FakeResponse({
        "orderCount": 2, "topCategory": "Tops", "totalSpent": 189.30,
        "recentOrders": [{"orderId": 1, "totalAmount": 144.50}],
    })

    result = st.get_spending_summary(42)

    assert json.loads(result) == {"orderCount": 2, "topCategory": "Tops", "totalSpent": 189.30}
    mock_get.assert_called_once_with(
        "http://localhost:8080/internal/tools/order-history",
        params={"userId": 42},
        timeout=10,
    )


@patch("services.smartcart_tools.requests.get")
def test_get_spending_summary_returns_error_json_when_backend_call_fails(mock_get):
    mock_get.side_effect = ConnectionError("backend down")

    result = st.get_spending_summary(42)

    assert json.loads(result) == {"error": "backend down"}


@patch("services.smartcart_tools.requests.get")
def test_search_products_only_includes_provided_filters(mock_get):
    mock_get.return_value = _FakeResponse({"products": []})

    st.search_products(category=None, max_price=None, query=None, limit=4, newest_first=False)

    mock_get.assert_called_once_with(
        "http://localhost:8080/internal/tools/products/search",
        params={"limit": 4},
        timeout=10,
    )


@patch("services.smartcart_tools.requests.get")
def test_search_products_includes_all_filters_when_provided(mock_get):
    mock_get.return_value = _FakeResponse({"products": []})

    st.search_products(category="Tops", max_price=50.0, query="tee", limit=2, newest_first=True)

    mock_get.assert_called_once_with(
        "http://localhost:8080/internal/tools/products/search",
        params={"limit": 2, "category": "Tops", "maxPrice": 50.0, "query": "tee", "newestFirst": "true"},
        timeout=10,
    )


@patch("services.smartcart_tools.requests.get")
def test_search_products_returns_error_json_when_backend_call_fails(mock_get):
    mock_get.side_effect = ConnectionError("backend down")

    result = st.search_products()

    assert json.loads(result) == {"error": "backend down"}
