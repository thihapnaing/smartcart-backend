package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.RecommendationResultDTO;
import nus.iss.smartcart.backend.dto.RecommendedProductResponseDTO;
import nus.iss.smartcart.backend.service.RecommendationOrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

// 1. Updated import for WebMvcTest in Spring Boot 4+
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
// 2. Updated import for the new MockitoBean 
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecommendationController.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // 3. Swapped the removed @MockBean for @MockitoBean
    @MockitoBean
    private RecommendationOrchestratorService recommendationService;

    @Test
    void testGetRecommendations_Returns200() throws Exception {
        RecommendedProductResponseDTO productDTO = new RecommendedProductResponseDTO(
                2L, "Linen Shirt", "Tops", new BigDecimal("39.90"),
                "/assets/products/linen-shirt.jpg", "Great fit", 0.9
        );
        RecommendationResultDTO resultDTO = new RecommendationResultDTO("Summary text", List.of(productDTO));

        when(recommendationService.getRecommendationsForUser(2L)).thenReturn(resultDTO);

        mockMvc.perform(get("/api/v1/recommendations/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agent_summary").value("Summary text"))
                .andExpect(jsonPath("$.products[0].id").value(2))
                .andExpect(jsonPath("$.products[0].name").value("Linen Shirt"));
    }
}