import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from dotenv import load_dotenv

load_dotenv()   # loads .env from the working directory before anything else

from services.agent_service import AgentService
from services.vector_store import ProductCatalog
from routers import chat as chat_router, recommendation_router
#
#   Author: Htet Nandar (Grace)
#

agent: AgentService = AgentService()

@asynccontextmanager
async def lifespan(_app: FastAPI):
    print("Starting up - connecting to MCP tools server...")
    await agent.start()
    chat_router.agent_service = agent          # inject into routers
    recommendation_router.agent_service = agent  # inject into routers
    print("SmartCart AI agent ready.")

    # --- Sync ChromaDB with Spring Boot MySQL on startup ---
    try:
        print("[Startup] Syncing product catalog with ChromaDB...")
        #catalog = ProductCatalog()
        #catalog.reindex()
    except Exception as e:
        print(f"[Startup Warning] Could not sync ChromaDB: {e}")
    # --------------------------------------------------------

    yield
    print("Shutting down.")


app = FastAPI(title="SmartCart AI Service", lifespan=lifespan)

app.include_router(chat_router.router)
app.include_router(recommendation_router.router)


@app.get("/api/health")
def health():
    return {"status": "ok", "service": "smartcart-ai-service"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=int(os.getenv("PORT", 8001)), reload=True)
