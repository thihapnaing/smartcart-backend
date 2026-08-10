package nus.iss.smartcart.backend.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationDTOTest {

    @Test
    void testRecommendationResultDTO() {
        RecommendedProductResponseDTO product = new RecommendedProductResponseDTO(
                1L, "Linen Shirt", "Tops", new BigDecimal("39.90"),
                "/assets/products/linen-shirt.jpg", "Great choice", 0.95
        );

        RecommendationResultDTO resultDTO = new RecommendationResultDTO("Summary test", List.of(product));

        assertEquals("Summary test", resultDTO.getAgentSummary());
        assertEquals(1, resultDTO.getProducts().size());
        assertEquals("Linen Shirt", resultDTO.getProducts().get(0).getName());

        // Default constructor & setters
        RecommendationResultDTO emptyResult = new RecommendationResultDTO();
        emptyResult.setAgentSummary("Updated summary");
        emptyResult.setProducts(List.of());

        assertEquals("Updated summary", emptyResult.getAgentSummary());
        assertTrue(emptyResult.getProducts().isEmpty());
    }

    @Test
    void testCustomerProfileDTO() {
        CustomerProfileDTO profile = new CustomerProfileDTO(
                "customer-1",
                List.of("Linen"),
                List.of("Tee"),
                List.of("Shorts"),
                List.of("Shoes"),
                List.of("Tops"),
                150.00
        );

        assertEquals("customer-1", profile.getCustomerId());
        assertEquals(List.of("Linen"), profile.getInterests());
        assertEquals(List.of("Tee"), profile.getCart());
        assertEquals(List.of("Shorts"), profile.getRecentlyViewed());
        assertEquals(List.of("Shoes"), profile.getPurchaseHistory());
        assertEquals(List.of("Tops"), profile.getPreferredCategories());
        assertEquals(150.00, profile.getBudget());

        // Default constructor & setters
        CustomerProfileDTO emptyProfile = new CustomerProfileDTO();
        emptyProfile.setCustomerId("customer-2");
        emptyProfile.setInterests(List.of("Cotton"));
        emptyProfile.setCart(List.of("Pants"));
        emptyProfile.setRecentlyViewed(List.of("Socks"));
        emptyProfile.setPurchaseHistory(List.of("Jacket"));
        emptyProfile.setPreferredCategories(List.of("Bottoms"));
        emptyProfile.setBudget(200.00);

        assertEquals("customer-2", emptyProfile.getCustomerId());
        assertEquals(List.of("Cotton"), emptyProfile.getInterests());
        assertEquals(List.of("Pants"), emptyProfile.getCart());
        assertEquals(List.of("Socks"), emptyProfile.getRecentlyViewed());
        assertEquals(List.of("Jacket"), emptyProfile.getPurchaseHistory());
        assertEquals(List.of("Bottoms"), emptyProfile.getPreferredCategories());
        assertEquals(200.00, emptyProfile.getBudget());
    }

    @Test
    void testRecommendationRequestDTO() {
        CustomerProfileDTO profile = new CustomerProfileDTO();
        RecommendationRequestDTO request = new RecommendationRequestDTO(5, "auto", profile);

        assertEquals(5, request.getTopK());
        assertEquals("auto", request.getMode());
        assertEquals(profile, request.getCustomerProfile());

        // Default constructor & setters
        RecommendationRequestDTO emptyRequest = new RecommendationRequestDTO();
        emptyRequest.setTopK(10);
        emptyRequest.setMode("manual");
        emptyRequest.setCustomerProfile(null);

        assertEquals(10, emptyRequest.getTopK());
        assertEquals("manual", emptyRequest.getMode());
        assertNull(emptyRequest.getCustomerProfile());
    }

    @Test
    void testRecommendationResponseDTOAndItem() {
        RecommendationResponseDTO.RecommendationItem item = new RecommendationResponseDTO.RecommendationItem();
        item.setProductId("10");
        item.setScore(0.88);
        item.setReason("High match");

        assertEquals("10", item.getProductId());
        assertEquals(0.88, item.getScore());
        assertEquals("High match", item.getReason());

        RecommendationResponseDTO response = new RecommendationResponseDTO();
        response.setAgentSummary("FastAPI summary");
        response.setRecommendations(List.of(item));

        assertEquals("FastAPI summary", response.getAgentSummary());
        assertEquals(1, response.getRecommendations().size());
        assertEquals("10", response.getRecommendations().get(0).getProductId());
    }

    @Test
    void testRecommendedProductResponseDTO() {
        RecommendedProductResponseDTO dto = new RecommendedProductResponseDTO();
        dto.setId(1L);
        dto.setName("Cargo Pants");
        dto.setCategory("Bottoms");
        dto.setPrice(new BigDecimal("49.90"));
        dto.setImageUrl("/img.jpg");
        dto.setReason("Matches style");
        dto.setScore(0.8);

        assertEquals(1L, dto.getId());
        assertEquals("Cargo Pants", dto.getName());
        assertEquals("Bottoms", dto.getCategory());
        assertEquals(new BigDecimal("49.90"), dto.getPrice());
        assertEquals("/img.jpg", dto.getImageUrl());
        assertEquals("Matches style", dto.getReason());
        assertEquals(0.8, dto.getScore());
    }
}