package nus.iss.smartcart.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RecommendationResponseDTO {

    @JsonProperty("recommendations")
    private List<RecommendationItem> recommendations;
    
    @JsonProperty("agent_summary")
    private String agentSummary;

    public RecommendationResponseDTO() { /* Intentionally left empty */ }

    public List<RecommendationItem> getRecommendations() { return recommendations; }
    public void setRecommendations(List<RecommendationItem> recommendations) { this.recommendations = recommendations; }

    public String getAgentSummary() { return agentSummary; }
    public void setAgentSummary(String agentSummary) { this.agentSummary = agentSummary; }
    
    
    public static class RecommendationItem {

        @JsonProperty("product_id")
        private String productId;

        @JsonProperty("score")
        private Double score;

        @JsonProperty("reason")
        private String reason;

        public RecommendationItem() { /* Intentionally left empty - required by Jackson for JSON deserialization */ }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}