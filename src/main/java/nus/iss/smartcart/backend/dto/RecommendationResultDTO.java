package nus.iss.smartcart.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RecommendationResultDTO {

    @JsonProperty("agent_summary")
    private String agentSummary;

    @JsonProperty("products")
    private List<RecommendedProductResponseDTO> products;

    public RecommendationResultDTO() {}

    public RecommendationResultDTO(String agentSummary, List<RecommendedProductResponseDTO> products) {
        this.agentSummary = agentSummary;
        this.products = products;
    }

    public String getAgentSummary() { return agentSummary; }
    public void setAgentSummary(String agentSummary) { this.agentSummary = agentSummary; }

    public List<RecommendedProductResponseDTO> getProducts() { return products; }
    public void setProducts(List<RecommendedProductResponseDTO> products) { this.products = products; }
}