package nus.iss.smartcart.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class HomeContentService {

    private final RestTemplate restTemplate;

    // FIX 1: Matched the exact property name from application.properties
    @Value("${ai.python-service.base-url}")
    private String pythonApiBaseUrl;

    public HomeContentService() {
        // Instantiate RestTemplate directly
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> getLookbookTrends() {
        String url = pythonApiBaseUrl + "/api/v1/trends/lookbook";
        
        // FIX 2: Create the payload expected by your FastAPI TrendRequest model
        Map<String, String> requestPayload = Map.of(
            "theme", "summer minimalist fashion",
            "target_audience_budget", "$100 - $200"
        );

        // FIX 3: Use postForObject instead of getForObject
        return restTemplate.postForObject(url, requestPayload, Map.class);
    }

    public Map<String, Object> getMerchantSpotlight() {
        return Map.of(
            "title", "Merchant Spotlight",
            "merchants", new String[]{"Merchant A", "Merchant B"}
        );
    }
}