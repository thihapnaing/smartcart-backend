import os
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from langchain_openai import ChatOpenAI
from langchain_community.tools.tavily_search import TavilySearchResults
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_classic.agents import create_tool_calling_agent, AgentExecutor
from langchain_core.tools import tool

# Import ProductCatalog from vector_store
from services.vector_store import ProductCatalog

# Set your API Keys (In production, load these from .env)
os.environ["TAVILY_API_KEY"] = "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"

trend_router = APIRouter()

# Define the Request Data Model
class TrendRequest(BaseModel):
    theme: str = "summer minimalist fashion"
    target_audience_budget: str = "$100 - $200"

# Custom Tool for SmartCart Inventory
@tool
def search_smartcart_inventory(query: str) -> str:
    """Searches the SmartCart product catalog for real items matching a fashion trend or description."""
    try:
        catalog = ProductCatalog()
        # Adjust method name if your vector store uses .search() or .query()
        results = catalog.search(query, limit=5)
        return str(results) if results else "No matching products found in SmartCart inventory."
    except Exception as e:
        return f"Could not search SmartCart inventory due to error: {e}"

# Setup AI Agent Tools and LLM
search_tool = TavilySearchResults(max_results=5)
tools = [search_tool, search_smartcart_inventory]

llm = ChatOpenAI(
    model="openai/gpt-4o-mini",
    api_key=os.getenv("OPENROUTER_API_KEY"),
    base_url="https://openrouter.ai/api/v1",
    temperature=0.7
)

# Updated System Prompt (Guides the agent to use BOTH tools)
prompt = ChatPromptTemplate.from_messages([
    (
       "system",
        "You are an expert fashion stylist and content creator for SmartCart. "
        "Your task is to write an engaging 'Outfit of the Week' article for the home page.\n\n"
        "STEPS TO FOLLOW:\n"
        "1. Use Tavily web search to find current real-world minimalist fashion trends.\n"
        "2. Use 'search_smartcart_inventory' to retrieve matching items from the SmartCart catalog.\n"
        "3. Recommend a complete outfit (top, bottom, shoes) fitting the specified budget.\n\n"
        "OUTPUT FORMATTING RULES:\n"
        "- Return ONLY raw, clean HTML content (using <h3>, <h4>, <p>, <ul>, and <li> tags).\n"
        "- DO NOT wrap the output in markdown code blocks like ```html or ```.\n"
        "- DO NOT include conversational commentary or explanations before or after the HTML.\n"
        "- Ensure fashion references focus on timeless or current seasonal styles without referencing specific past years (e.g., avoid mentioning 2023).\n"
        "- Highlight product names in bold and include their exact price."
    ),
    ("human", "Theme: {theme} | Budget: {target_audience_budget}"),
    MessagesPlaceholder(variable_name="agent_scratchpad"),
])

# Compile the Agent
agent = create_tool_calling_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

@trend_router.post("/api/v1/trends/lookbook")
async def generate_lookbook(request: TrendRequest):
    try:
        result = agent_executor.invoke({
            "theme": request.theme,
            "target_audience_budget": request.target_audience_budget
        })
        
        # Clean up any potential LLM backtick wrapping
        clean_html = result["output"]
        if "```html" in clean_html:
            clean_html = clean_html.split("```html")[1].split("```")[0].strip()
        elif "```" in clean_html:
            clean_html = clean_html.split("```")[1].split("```")[0].strip()

        return {
            "status": "success",
            "theme_analyzed": request.theme,
            "generated_article_html": clean_html
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))