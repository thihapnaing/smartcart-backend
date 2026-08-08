import json
import os

from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

from prompts.recommendation_prompt import SYSTEM_PROMPT

from pathlib import Path

env_path = Path(__file__).parent.parent / ".env"
load_dotenv(dotenv_path=env_path)

print("Current working directory:", os.getcwd())
print("This file:", __file__)
print("Expected .env:", env_path)
print("Exists:", env_path.exists())


def openai_available() -> bool:
    return bool(os.getenv("OPENROUTER_API_KEY"))


def ask_recommendation_agent(profile: dict, candidates: list[dict], top_k: int) -> dict:
    """Ask an OpenAI/LangChain agent to choose from already-found catalog candidates."""
    model = ChatOpenAI(model="openai/gpt-4o-mini", 
                       api_key=os.getenv("OPENROUTER_API_KEY"),
                       base_url="https://openrouter.ai/api/v1",  
                       temperature=0)
    prompt = f"{SYSTEM_PROMPT}\nCustomer profile:\n{json.dumps(profile)}\nCandidate products:\n{json.dumps(candidates)}\nReturn {top_k} recommendations."
    response = model.invoke(prompt)
    content = response.content if isinstance(response.content, str) else str(response.content)
    # Some models wrap JSON in Markdown fences.
    content = content.replace("```json", "").replace("```", "").strip()
    return json.loads(content)
