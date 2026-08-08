from langchain_core.tools import tool

from services.vector_store import ProductCatalog


def build_product_search_tool(catalog: ProductCatalog):
    @tool
    def product_search(query: str, limit: int = 8) -> list[dict]:
        """Search the merchant catalog. Use this before selecting products."""
        return catalog.search(query, limit=limit)

    return product_search
