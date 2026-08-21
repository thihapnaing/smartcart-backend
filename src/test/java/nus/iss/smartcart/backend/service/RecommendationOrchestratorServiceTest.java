package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.*;
import nus.iss.smartcart.backend.model.*;
import nus.iss.smartcart.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationOrchestratorServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RecommendationOrchestratorService recommendationOrchestratorService;

    private UserProfile userProfile;
    private Product product;

    @BeforeEach
    void setUp() {
        userProfile = new UserProfile();
        userProfile.setInterests("Linen,Summer");
        userProfile.setPreferredCategories("Tops,Bottoms");
        userProfile.setBudget(new BigDecimal("200.00"));

        Category category = new Category();
        category.setName("Tops");

        product = new Product();
        ReflectionTestUtils.setField(product, "id", 2L);
        product.setName("Linen Shirt");
        product.setCategory(category);
        product.setPrice(new BigDecimal("39.90"));
        product.setImageUrl("/assets/products/linen-shirt.jpg");
        product.setStatus(ProductStatus.ACTIVE);

        // Inject the mocked RestTemplate into the service
        ReflectionTestUtils.setField(recommendationOrchestratorService, "restTemplate", restTemplate);
    }

    @Test
    void testBuildCustomerProfile_Success() {
        when(userProfileRepository.findByUserId(2L)).thenReturn(Optional.of(userProfile));
        when(cartRepository.findProductNamesByUserId(2L)).thenReturn(List.of("Classic Crew Tee"));
        when(orderRepository.findPurchasedProductNamesByUserId(2L)).thenReturn(List.of("Chino Shorts"));

        CustomerProfileDTO profile = recommendationOrchestratorService.buildCustomerProfile(2L);

        assertNotNull(profile);
        assertEquals("customer-2", profile.getCustomerId());
        assertEquals(200.00, profile.getBudget());
        assertTrue(profile.getCart().contains("Classic Crew Tee"));
        assertTrue(profile.getPurchaseHistory().contains("Chino Shorts"));
    }

    @Test
    void testGetRecommendationsForUser_Success() {
        when(userProfileRepository.findByUserId(2L)).thenReturn(Optional.of(userProfile));
        when(cartRepository.findProductNamesByUserId(2L)).thenReturn(List.of());
        when(orderRepository.findPurchasedProductNamesByUserId(2L)).thenReturn(List.of());

        RecommendationResponseDTO responseDTO = new RecommendationResponseDTO();
        responseDTO.setAgentSummary("AI agent summary test");

        RecommendationResponseDTO.RecommendationItem item = new RecommendationResponseDTO.RecommendationItem();
        item.setProductId("2");
        item.setReason("Fits style");
        item.setScore(0.9);
        responseDTO.setRecommendations(List.of(item));

        when(restTemplate.postForObject(anyString(), any(), eq(RecommendationResponseDTO.class)))
                .thenReturn(responseDTO);
        when(productRepository.findAllById(List.of(2L))).thenReturn(List.of(product));

        RecommendationResultDTO result = recommendationOrchestratorService.getRecommendationsForUser(2L);

        assertNotNull(result);
        assertEquals("AI agent summary test", result.getAgentSummary());
        assertEquals(1, result.getProducts().size());
        assertEquals("Linen Shirt", result.getProducts().get(0).getName());
    }

    @Test
    void testGetRecommendationsForUser_FallbackOnException() {
        when(userProfileRepository.findByUserId(2L)).thenReturn(Optional.of(userProfile));
        when(restTemplate.postForObject(anyString(), any(), eq(RecommendationResponseDTO.class)))
                .thenThrow(new RuntimeException("FastAPI connection error"));
        when(productRepository.findAll()).thenReturn(List.of(product));

        RecommendationResultDTO result = recommendationOrchestratorService.getRecommendationsForUser(2L);

        assertNotNull(result);
        assertTrue(result.getAgentSummary().contains("Fallback recommendations used"));
        assertEquals(1, result.getProducts().size());
    }
}