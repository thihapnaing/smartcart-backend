import os
from datetime import datetime, timedelta
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
from dotenv import load_dotenv
#os.environ["TAVILY_API_KEY"] 
load_dotenv()

trend_router = APIRouter()

# Define the In-Memory Cache variables
CACHE_DURATION_HOURS = 3
lookbook_cache = {
    "html": None,
    "last_updated": None,
    "theme": None
}

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
        "You are an expert fashion stylist and premium content creator for SmartCart. "
        "Your task is to write an engaging, highly detailed, and descriptive 'Outfit of the Week' article for the home page.\n\n"
        
        "STEPS TO FOLLOW:\n"
        "1. Use Tavily web search to find current real-world minimalist fashion trends.\n"
        "2. Use 'search_smartcart_inventory' to retrieve matching items from the SmartCart catalog.\n"
        "3. Recommend a complete outfit (top, bottom, shoes) fitting the specified budget.\n\n"
        
        "CONTENT WRITING RULES (CRITICAL FOR LENGTH):\n"
        "- Introduction: Write a compelling opening paragraph setting the scene for the trend and explaining why this look works (at least 3-4 sentences).\n"
        "- Product Details: For EACH recommended product, write a rich, descriptive paragraph explaining its fabric, fit, styling potential, and how it pairs with the rest of the outfit.\n"
        "- Conclusion: End with a final styling tip (e.g., how to transition the outfit from day to night, or how to accessorize).\n"
        "- Tone: Use evocative, premium fashion magazine copywriting.\n\n"
        
        "OUTPUT FORMATTING RULES:\n"
        "- Return ONLY raw, clean HTML content (using <h3>, <h4>, <p>, <ul>, and <li> tags).\n"
        "- DO NOT wrap the output in markdown code blocks like ```html or ```.\n"
        "- DO NOT include conversational commentary or explanations before or after the HTML.\n"
        "- Ensure fashion references focus on timeless or current seasonal styles without referencing specific past years.\n"
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
    global lookbook_cache
    now = datetime.now()

    # Check if we have a valid, unexpired cache for the requested theme
    is_same_theme = lookbook_cache["theme"] == request.theme
    is_cache_valid = lookbook_cache["last_updated"] and (now - lookbook_cache["last_updated"] < timedelta(hours=CACHE_DURATION_HOURS))

    if is_same_theme and is_cache_valid:
        print("\n[CACHE HIT] Returning cached Lookbook (Saved Tavily credits!)\n")
        return {
            "status": "success",
            "theme_analyzed": request.theme,
            "generated_article_html": lookbook_cache["html"],
            "cached_response": True # Optional flag for debugging
        }
	
	# If no cache or expired, run the LangChain Agent
    print("\n[CACHE MISS] Generating new Lookbook via LLM & Tavily...\n")
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
            
        # Save the newly generated content into the cache
        lookbook_cache["html"] = clean_html
        lookbook_cache["last_updated"] = now
        lookbook_cache["theme"] = request.theme

        return {
            "status": "success",
            "theme_analyzed": request.theme,
            "generated_article_html": clean_html,
            "cached_response": False
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))