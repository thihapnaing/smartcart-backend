package nus.iss.smartcart.backend.service;

// Author - Cecil

import nus.iss.smartcart.backend.dto.CustomerProfileDTO;
import nus.iss.smartcart.backend.dto.RecommendationRequestDTO;
import nus.iss.smartcart.backend.dto.RecommendationResponseDTO;
import nus.iss.smartcart.backend.dto.RecommendedProductResponseDTO;
import nus.iss.smartcart.backend.dto.RecommendationResultDTO;

import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.model.UserProfile;
import nus.iss.smartcart.backend.repository.CartRepository;
import nus.iss.smartcart.backend.repository.OrderRepository;
import nus.iss.smartcart.backend.repository.ProductRepository;
import nus.iss.smartcart.backend.repository.UserProfileRepository;


import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecommendationOrchestratorService {
	
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RecommendationOrchestratorService.class);

    private final UserProfileRepository userProfileRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    // Replaced RestClient with RestTemplate
    private final RestTemplate restTemplate;

    public RecommendationOrchestratorService(UserProfileRepository userProfileRepository,
                                             CartRepository cartRepository,
                                             OrderRepository orderRepository,
                                             ProductRepository productRepository) {
        this.userProfileRepository = userProfileRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        
        // Initialize RestTemplate
        this.restTemplate = new RestTemplate();
    }

    public CustomerProfileDTO buildCustomerProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found for ID: " + userId));

        List<String> cartItems = cartRepository.findProductNamesByUserId(userId);
        List<String> purchaseHistory = orderRepository.findPurchasedProductNamesByUserId(userId);

        return new CustomerProfileDTO(
                "customer-" + userId,
                profile.getInterestsList(),
                cartItems,
                List.<String>of(),
                purchaseHistory,
                profile.getPreferredCategoriesList(),
                profile.getBudget() != null ? profile.getBudget().doubleValue() : 200.00
        );
    } 

    public RecommendationResultDTO getRecommendationsForUser(Long userId) {
        CustomerProfileDTO profile = buildCustomerProfile(userId);

        RecommendationRequestDTO requestPayload = new RecommendationRequestDTO(
                7,
                "auto",
                profile
        );

        try {
        	 // Execute request using RestTemplate (Forces HTTP/1.1)
            RecommendationResponseDTO aiResponse = restTemplate.postForObject(
                    "http://127.0.0.1:8001/api/v1/recommendations",
                    requestPayload,
                    RecommendationResponseDTO.class
            );

            if (aiResponse == null || aiResponse.getRecommendations() == null) {
                return new RecommendationResultDTO("Fallback recommendations used due to empty AI response.", getFallbackProducts());
            }

            List<Long> productIds = aiResponse.getRecommendations().stream()
                    .map(rec -> Long.parseLong(rec.getProductId()))
                    .toList();

            Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            List<RecommendedProductResponseDTO> recommendedProducts = aiResponse.getRecommendations().stream()
                    .map(rec -> {
                        Long id = Long.parseLong(rec.getProductId());
                        Product product = productMap.get(id);
                        
                        // --- THE GATEKEEPER ---
                        // Drop the product if it doesn't exist OR if it is deactivated
                        if (product == null || product.getStatus() != ProductStatus.ACTIVE) {
                            return null;
                        }
                        
                        return new RecommendedProductResponseDTO(
                                product.getId(),
                                product.getName(),
                                product.getCategory() != null ? product.getCategory().getName() : "",
                                product.getPrice(),
                                product.getImageUrl(),
                                rec.getReason(),
                                rec.getScore()
                        );
                    })
                    .filter(java.util.Objects::nonNull) // This automatically cleans up the nulls!
                    .toList();

            // Return the combined object containing the summary and the products
            return new RecommendationResultDTO(aiResponse.getAgentSummary(), recommendedProducts);

        } catch (Exception e) {
        	logger.warn("[Warning] Failed to fetch AI recommendations from FastAPI: {}", e.getMessage());
            return new RecommendationResultDTO("Fallback recommendations used due to connection error.", getFallbackProducts());
        }
    }
    

    private List<RecommendedProductResponseDTO> getFallbackProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE) // <-- Added Gatekeeper filter
                .limit(5)
                .map(p -> new RecommendedProductResponseDTO(
                        p.getId(),
                        p.getName(),
                        p.getCategory() != null ? p.getCategory().getName() : "",
                        p.getPrice(),
                        p.getImageUrl(),
                        "Popular item in store",
                        1.0
                ))
                .toList();
    }
}