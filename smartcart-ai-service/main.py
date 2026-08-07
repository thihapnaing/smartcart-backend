import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from dotenv import load_dotenv

load_dotenv()   # loads .env from the working directory before anything else

from services.agent_service import AgentService
from routers import chat as chat_router

from services.cnn_service import CNNService
from routers import image_search as image_router

#
#   Author: Htet Nandar (Grace)
#

agent = AgentService()
cnn_service = CNNService()

@asynccontextmanager
async def lifespan(_app: FastAPI):
    print("Starting up - connecting to MCP tools server...")

    await agent.start()

    cnn_service.load()                          # load CNN

    chat_router.agent_service = agent           # inject into routers
    image_router.cnn_service = cnn_service

    print("SmartCart AI agent ready.")
    yield
    print("Shutting down.")


app = FastAPI(title="SmartCart AI Service", lifespan=lifespan)

app.include_router(chat_router.router)
app.include_router(image_router.router)


@app.get("/api/health")
def health():
    return {"status": "ok", "service": "smartcart-ai-service"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=int(os.getenv("PORT", 8001)), reload=True)
