from typing import Literal

from pydantic import BaseModel, Field


class CustomerProfile(BaseModel):
    customer_id: str = "demo-customer-001"
    age: int | None = Field(default=None, ge=13, le=120)
    gender: str | None = None
    interests: list[str] = Field(default_factory=list)
    recently_viewed: list[str] = Field(default_factory=list)
    cart: list[str] = Field(default_factory=list)
    purchase_history: list[str] = Field(default_factory=list)
    preferred_categories: list[str] = Field(default_factory=list)
    budget: float | None = Field(default=None, gt=0)


class RecommendationRequest(BaseModel):
    customer_profile: CustomerProfile | None = None
    top_k: int = Field(default=3, ge=1, le=10)
    mode: Literal["auto", "offline", "openai"] = "auto"


class ProductRecommendation(BaseModel):
    product_id: str
    product: str
    category: str
    price: float
    reason: str
    score: float = Field(ge=0, le=1)


class RecommendationResponse(BaseModel):
    customer_id: str
    mode_used: Literal["offline", "openai"]
    recommendations: list[ProductRecommendation]
    agent_summary: str
