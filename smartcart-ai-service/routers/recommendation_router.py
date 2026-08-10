from fastapi import APIRouter, HTTPException

from models.request_models import RecommendationRequest, RecommendationResponse
from services.recommendation_service import RecommendationService

router = APIRouter(prefix="/api/v1", tags=["Recommendations"])


@router.post("/recommendations", response_model=RecommendationResponse)
def get_recommendations(request: RecommendationRequest) -> RecommendationResponse:
    """Recommend merchant products for a supplied customer profile."""
    try:
        return RecommendationService.get_instance().recommend(request)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Recommendation failed: {exc}") from exc
