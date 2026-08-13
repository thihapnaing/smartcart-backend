import os
from dotenv import load_dotenv

# Load .env variables
load_dotenv()

from services.vector_store import ProductCatalog

catalog = ProductCatalog()

print("Triggering indexing from MySQL database...")
# Force fetching from MySQL and indexing into ChromaDB
catalog.ensure_indexed()

total_count = catalog.collection.count()
print(f"\nTotal products in ChromaDB: {total_count}")

if total_count > 0:
    sample_data = catalog.collection.get(limit=5)
    print("\nSample Indexed Documents:")
    for i in range(len(sample_data["ids"])):
        print(f"\nID: {sample_data['ids'][i]}")
        print(f"Metadata: {sample_data['metadatas'][i]}")
        print(f"Content Preview:\n{sample_data['documents'][i]}")