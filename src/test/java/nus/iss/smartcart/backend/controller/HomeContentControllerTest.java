package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.service.HomeContentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

// NEW Spring Boot 4 Import for WebMvcTest
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
// NEW Spring Boot 4 Import for MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import nus.iss.smartcart.backend.security.JwtAuthenticationFilter; 
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;


import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeContentController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeContentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // Replaced @MockBean with the new Spring 4 @MockitoBean
    @MockitoBean
    private HomeContentService homeContentService;

    @Test
    void testGetLookbook() throws Exception {
        // Setup mock response from the service
        Map<String, Object> mockResponse = Map.of(
                "status", "success",
                "theme_analyzed", "summer minimalist fashion"
        );
        when(homeContentService.getLookbookTrends()).thenReturn(mockResponse);

        // Perform HTTP GET and assert expectations
        mockMvc.perform(get("/api/home/trends/lookbook"))
                .andExpect(status().isOk()) 
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.theme_analyzed").value("summer minimalist fashion"));
    }

    @Test
    void testGetMerchantSpotlight() throws Exception {
        // Setup mock response from the service
        Map<String, Object> mockResponse = Map.of(
                "title", "Merchant Spotlight",
                "merchants", new String[]{"Merchant A", "Merchant B"}
        );
        when(homeContentService.getMerchantSpotlight()).thenReturn(mockResponse);

        // Perform HTTP GET and assert expectations
        mockMvc.perform(get("/api/home/merchants/spotlight"))
                .andExpect(status().isOk()) 
                .andExpect(jsonPath("$.title").value("Merchant Spotlight"))
                .andExpect(jsonPath("$.merchants[0]").value("Merchant A"))
                .andExpect(jsonPath("$.merchants[1]").value("Merchant B"));
    }
}