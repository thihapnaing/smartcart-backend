import os
import math
from pathlib import Path
import requests
import chromadb
from dotenv import load_dotenv

load_dotenv()

ROOT = Path(__file__).parent.parent

SPRING_BOOT_URL = os.getenv("SPRING_BOOT_URL", "http://127.0.0.1:8080")
PRODUCTS_ENDPOINT = f"{SPRING_BOOT_URL}/api/v1/products/vector-export"


class ProductCatalog:
    def __init__(self, db_dir="chroma_db"):
        self.db_dir = ROOT / db_dir
        self.client = chromadb.PersistentClient(path=str(self.db_dir))
        self.collection = self.client.get_or_create_collection(name="products")

    def _fetch_products_from_backend(self) -> list[dict]:
        try:
            response = requests.get(PRODUCTS_ENDPOINT, timeout=10)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            raise RuntimeError(f"Failed to fetch products from Spring Boot backend: {e}")

    def _index_products(self):
        """Internal helper to fetch product data from Spring Boot and populate ChromaDB."""
        products = self._fetch_products_from_backend()
        if not products:
            print("[VectorStore] No products returned from Spring Boot backend.")
            return

        documents, metadatas, ids = [], [], []

        for item in products:
            product_id = str(item.get("productId"))
            product_name = str(item.get("productName", ""))
            description = str(item.get("description", ""))
            category = str(item.get("category", "General"))
            gender = str(item.get("gender", ""))
            price = float(item.get("price", 0.0))
            available_sizes = str(item.get("availableSizes", "N/A"))
            total_stock = int(item.get("totalStock", 0))

            # Clean semantic document text: NO image_url included
            content = (
                f"Product: {product_name}\n"
                f"Category: {category}\n"
                f"Description: {description}\n"
                f"Gender: {gender}\n"
                f"Price: ${price:.2f}\n"
                f"Available Sizes: {available_sizes}\n"
                f"In Stock: {total_stock}"
            )

            documents.append(content)
            metadatas.append({
                "product_id": product_id,
                "name": product_name,
                "category": category,
                "description": description,
                "price": price,
                "gender": gender
            })
            ids.append(product_id)

        if ids:
            self.collection.add(
                documents=documents,
                metadatas=metadatas,
                ids=ids
            )
            print(f"[VectorStore] Successfully indexed {len(ids)} products into ChromaDB.")

    def ensure_indexed(self):
        """Indexes products if the collection is currently empty."""
        if self.collection.count() > 0:
            return
        self._index_products()

    def reindex(self):
        """Forces a fresh pull from Spring Boot and rebuilds the ChromaDB collection."""
        try:
            self.client.delete_collection(name="products")
        except ValueError:
            pass

        self.collection = self.client.get_or_create_collection(name="products")
        self._index_products()

    def search(self, query: str, limit: int = None) -> list[dict]:
        """Queries ChromaDB using dynamic candidate scaling: K = max(8, min(25, 20% of total count))."""
        total_count = self.collection.count()
        if total_count == 0:
            return []

        # Dynamic candidate retrieval calculation
        if limit is None:
            calculated_k = math.ceil(total_count * 0.20)
            limit = max(8, min(25, calculated_k))

        fetch_limit = min(limit, total_count)

        results = self.collection.query(
            query_texts=[query],
            n_results=fetch_limit
        )

        candidates = []
        if results and results.get("metadatas") and results["metadatas"]:
            metadatas = results["metadatas"][0]
            for meta in metadatas:
                category = str(meta.get("category", "General"))
                gender = str(meta.get("gender", ""))
                
                candidates.append({
                    "product_id": str(meta.get("product_id", "")),
                    "name": str(meta.get("name", "")),
                    "category": category,
                    "description": str(meta.get("description", "")),
                    "price": float(meta.get("price", 0.0)),
                    "gender": gender,
                    "tags": f"{gender} {category}".strip()
                })

        return candidates