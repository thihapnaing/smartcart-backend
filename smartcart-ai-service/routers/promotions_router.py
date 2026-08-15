import os
import json
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate

promotions_router = APIRouter()

# Data models representing records sent from Spring Boot
class ProductInput(BaseModel):
    id: int
    name: str
    category: str
    price: float
    description: str

class MerchantSpotlightRequest(BaseModel):
    merchant_id: int
    merchant_name: str
    brand_story_summary: Optional[str] = "Featured seller on SmartCart"
    products: List[ProductInput]

# Initialize LLM via OpenRouter
llm = ChatOpenAI(
    model="openai/gpt-4o-mini",
    api_key=os.getenv("OPENROUTER_API_KEY"),
    base_url="https://openrouter.ai/api/v1",
    temperature=0.8
)

prompt = ChatPromptTemplate.from_messages([
    (
        "system",
        "You are an expert e-commerce copywriter for SmartCart. "
        "Your task is to write a high-converting 'Merchant Spotlight' feature. "
        "You MUST return valid JSON matching this exact structure:\n"
        "{{\n"
        '  "headline": "Punchy 5-7 word title",\n'
        '  "subheadline": "Engaging 10-12 word tagline",\n'
        '  "brand_story": "A compelling 2-3 sentence story about the merchant",\n'
        '  "product_highlights": [\n'
        '    {{\n'
        '      "product_id": 101,\n'
        '      "badge": "e.g., Sustainable Choice / Best Seller",\n'
        '      "sales_pitch": "2 short sentences explaining why the customer needs this item"\n'
        "    }}\n"
        "  ]\n"
        "}}\n"
        "Do not wrap in markdown quotes. Output raw JSON only."
    ),
    (
        "human",
        "Merchant: {merchant_name}\n"
        "Brand Story Context: {brand_story_summary}\n"
        "Products to Feature: {products}"
    )
])

@promotions_router.post("/api/v1/promotions/spotlight")
async def generate_merchant_spotlight(request: MerchantSpotlightRequest):
    try:
        formatted_prompt = prompt.format_messages(
            merchant_name=request.merchant_name,
            brand_story_summary=request.brand_story_summary,
            products=[p.model_dump() for p in request.products]
        )
        
        # Enforce JSON output format
        response = llm.invoke(formatted_prompt, response_format={"type": "json_object"})
        
        # Parse output string to guarantee valid JSON structure
        content_json = json.loads(response.content)
        
        return {
            "status": "success",
            "merchant_id": request.merchant_id,
            "merchant_name": request.merchant_name,
            "spotlight": content_json
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))