package nus.iss.smartcart.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeContentServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private HomeContentService homeContentService;

    @BeforeEach
    void setUp() {
    	// 1. Instantiate using the no-argument constructor
        homeContentService = new HomeContentService();
        
        // 2. Inject the mocked RestTemplate using Reflection
        ReflectionTestUtils.setField(homeContentService, "restTemplate", restTemplate);
        
        // 3. Inject the @Value property URL
        ReflectionTestUtils.setField(homeContentService, "pythonApiBaseUrl", "http://localhost:8001");
    }

    @Test
    void testGetLookbookTrends() {
        // 1. Setup Mock Data & Expected Variables
        String expectedUrl = "http://localhost:8001/api/v1/trends/lookbook";
        Map<String, String> expectedPayload = Map.of(
                "theme", "summer minimalist fashion",
                "target_audience_budget", "$100 - $200"
        );
        Map<String, Object> mockResponse = Map.of(
                "status", "success",
                "generated_article_html", "<h3>Outfit of the Week</h3>"
        );

        // 2. Define Mockito Behavior
        when(restTemplate.postForObject(expectedUrl, expectedPayload, Map.class))
                .thenReturn(mockResponse);

        // 3. Execute the Method
        Map<String, Object> actualResponse = homeContentService.getLookbookTrends();

        // 4. Assertions
        assertNotNull(actualResponse, "Response should not be null");
        assertEquals("success", actualResponse.get("status"));
        assertEquals("<h3>Outfit of the Week</h3>", actualResponse.get("generated_article_html"));

        // Verify the RestTemplate was called exactly once with correct parameters
        verify(restTemplate).postForObject(expectedUrl, expectedPayload, Map.class);
    }

    @Test
    void testGetMerchantSpotlight() {
        // Execute the Method
        Map<String, Object> actualResponse = homeContentService.getMerchantSpotlight();

        // Assertions
        assertNotNull(actualResponse);
        assertEquals("Merchant Spotlight", actualResponse.get("title"));
        
        String[] merchants = (String[]) actualResponse.get("merchants");
        assertEquals(2, merchants.length);
        assertEquals("Merchant A", merchants[0]);
    }
}