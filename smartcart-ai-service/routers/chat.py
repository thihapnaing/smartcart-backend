from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from services.agent_service import AgentService
#
#   Author: Htet Nandar (Grace)
#

router = APIRouter(prefix="/api/chat", tags=["chat"])

# Injected by main.py after startup
agent_service: Optional[AgentService] = None


# ── Schemas ────────────────────────────────────────────────────────────────

class Message(BaseModel):
    role: str       # "user" | "assistant"
    content: str


class ChatRequest(BaseModel):
    session_id: Optional[str] = None
    message: str
    history: list[Message] = []
    user_id: Optional[int] = None


class Product(BaseModel):
    productId: Optional[int] = None
    name: Optional[str] = None
    price: Optional[float] = None
    imageUrl: Optional[str] = None
    category: Optional[str] = None
    # First variant's id - lets the Angular "+ Add" button skip size selection.
    # Comes from ToolDataService.searchProducts on the Java side; must be
    # declared here or pydantic silently drops it when building ChatResponse.
    defaultVariantId: Optional[int] = None


class OrderItem(BaseModel):
    name: Optional[str] = None
    price: Optional[float] = None
    imageUrl: Optional[str] = None
    quantity: Optional[int] = None
    # Lets Angular's "Buy again" action call POST /api/cart/items directly.
    productVariantId: Optional[int] = None


class Order(BaseModel):
    orderId: Optional[int] = None
    # Real trackingNo if the order has one, else a zero-padded "SC-######" fallback - see
    # ToolDataService.getOrderHistory on the Java side. Must be declared here or pydantic
    # silently drops it when building ChatResponse, same as Product.defaultVariantId below.
    orderNumber: Optional[str] = None
    totalAmount: Optional[float] = None
    status: Optional[str] = None
    orderDate: Optional[str] = None
    items: Optional[list[OrderItem]] = None


class ChatResponse(BaseModel):
    reply: str
    session_id: Optional[str] = None
    products: Optional[list[Product]] = None
    orders: Optional[list[Order]] = None


# ── Endpoints ──────────────────────────────────────────────────────────────

@router.post("", response_model=ChatResponse)
async def send_message(request: ChatRequest):
    if agent_service is None:
        raise HTTPException(status_code=503, detail="Service not ready")
    try:
        reply, products, orders = await agent_service.chat(
            message=request.message,
            history=[m.model_dump() for m in request.history],
            user_id=request.user_id,
        )
        return ChatResponse(reply=reply, session_id=request.session_id, products=products, orders=orders)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
