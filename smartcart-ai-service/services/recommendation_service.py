import json
from pathlib import Path
import profile

from models.request_models import CustomerProfile, ProductRecommendation, RecommendationRequest, RecommendationResponse
from services.llm_service import ask_recommendation_agent, openai_available
from services.vector_store import ProductCatalog, ROOT
from tools.product_tools import build_product_search_tool


class RecommendationService:
    _instance = None

    def __init__(self) -> None:
        self.catalog = ProductCatalog()
        self.product_search = build_product_search_tool(self.catalog)

    @classmethod
    def get_instance(cls):
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def _default_profile(self) -> CustomerProfile:
        return CustomerProfile.model_validate(json.loads((ROOT / "data" / "customer_profile.json").read_text(encoding="utf-8")))

    @staticmethod
    def _query(profile: CustomerProfile) -> str:
        return " ".join(profile.interests + profile.recently_viewed + profile.cart + profile.purchase_history + profile.preferred_categories)

    def _offline(self, profile: CustomerProfile, candidates: list[dict], top_k: int) -> list[ProductRecommendation]:
        excluded = {x.lower() for x in profile.cart + profile.recently_viewed}
        signals = self._query(profile).lower().split()
        preferred_cats = [c.lower() for c in profile.preferred_categories]
    
        picks = []
        for product in candidates:
            prod_name = product["name"].lower()
            prod_cat = product.get("category", "").lower()
            price = float(product["price"])

            # Exclude items in cart/recently viewed or over budget
            if prod_name in excluded or (profile.budget and price > profile.budget):
                continue

            # Combine text fields for keyword overlap evaluation
            description = product.get("description", "").lower()
            tags = product.get("tags", "").lower()
            product_text = f"{prod_name} {prod_cat} {description} {tags}"

            # Base score calculation from signal overlap
            overlap = sum(word in product_text for word in signals if len(word) > 2)
            score = 0.40 + (overlap * 0.05)

            # Apply category priority boost
            is_preferred_cat = prod_cat in preferred_cats
            if is_preferred_cat:
                score += 0.35  # Boost matching preferred categories
                reason = f"Fits your preferred category '{product['category']}' and matches your style."
            else:
                reason = f"Matches your interest in {product.get('category', 'general items').lower()}."

            final_score = min(0.98, max(0.10, score))
            picks.append((final_score, product, reason))

        # 1. Sort candidate items descending by score
        picks.sort(key=lambda row: row[0], reverse=True)

        # 2. Group sorted candidates by category
        picks_by_cat = {}
        for score, product, reason in picks:
            cat = product.get("category", "General").lower()
            picks_by_cat.setdefault(cat, []).append((score, product, reason))

        # 3. Interleave candidates across preferred categories first
        balanced_picks = []
        while len(balanced_picks) < top_k and any(picks_by_cat.values()):
            added_in_round = False
            # Pull top item sequentially from each preferred category
            for cat in preferred_cats:
                if cat in picks_by_cat and picks_by_cat[cat]:
                    balanced_picks.append(picks_by_cat[cat].pop(0))
                    added_in_round = True
                    if len(balanced_picks) == top_k:
                        break
        
            # Fill remaining slots with remaining highest-scoring non-preferred items if needed
            if not added_in_round or len(balanced_picks) < top_k:
                for cat, items in list(picks_by_cat.items()):
                    if items and len(balanced_picks) < top_k:
                        balanced_picks.append(items.pop(0))

        # 4. Return formatted ProductRecommendation list
        return [
            ProductRecommendation(
                product_id=p["product_id"],
                product=p["name"],
                category=p.get("category", "General"),
                price=float(p["price"]),
                reason=reason,
                score=round(score, 2)
            )
            for score, p, reason in balanced_picks[:top_k]
        ]

    def recommend(self, request: RecommendationRequest) -> RecommendationResponse:
        self.ensure_catalog_indexed()
        profile = request.customer_profile or self._default_profile()
        
        candidates = []
        existing_ids = set()

        # Step 1: Priority fetch for preferred categories
        if profile.preferred_categories:
            category_query = " ".join(profile.preferred_categories)
            pref_candidates = self.product_search.invoke({"query": category_query, "limit": 1})
            for c in pref_candidates:
                if c["product_id"] not in existing_ids:
                    candidates.append(c)
                    existing_ids.add(c["product_id"])

        # Step 2: Fill remaining candidate pool with general signals
        general_query = self._query(profile)
        general_candidates = self.product_search.invoke({"query": general_query, "limit": 18})
        
        for c in general_candidates:
            if c["product_id"] not in existing_ids:
                candidates.append(c)
                existing_ids.add(c["product_id"])

        # Step 3: Route candidates to LLM or Offline fallback
        use_openai = request.mode == "openai" or (request.mode == "auto" and openai_available())
        if use_openai and not openai_available():
            raise ValueError("OPENAI_API_KEY is required when mode is 'openai'.")
            
        if use_openai:
            agent_result = ask_recommendation_agent(profile.model_dump(), candidates, request.top_k)
            by_id = {p["product_id"]: p for p in candidates}
            recs = []
            excluded = {x.lower() for x in profile.cart + profile.recently_viewed}
            
            for item in agent_result.get("recommendations", []):
                product = by_id.get(item.get("product_id"))
                if product and product["name"].lower() not in excluded:
                    recs.append(ProductRecommendation(
                        product_id=product["product_id"], 
                        product=product["name"], 
                        category=product["category"], 
                        price=float(product["price"]), 
                        reason=item.get("reason", "Matches your shopping profile."), 
                        score=max(0, min(1, float(item.get("score", 0.7))))
                    ))
            if recs:
                return RecommendationResponse(
                    customer_id=profile.customer_id, 
                    mode_used="openai", 
                    recommendations=recs[:request.top_k], 
                    agent_summary=agent_result.get("agent_summary", "Selected from the merchant catalog.")
                )
                
        recs = self._offline(profile, candidates, request.top_k)
        return RecommendationResponse(
            customer_id=profile.customer_id, 
            mode_used="offline", 
            recommendations=recs, 
            agent_summary="Offline ranking used catalog similarity, browsing signals, cart context, and budget."
        )

    def ensure_catalog_indexed(self) -> None:
        self.catalog.ensure_indexed()
