package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.CustomerProfileDTO;
import nus.iss.smartcart.backend.model.UserProfile;
import nus.iss.smartcart.backend.repository.CartRepository;
import nus.iss.smartcart.backend.repository.OrderRepository;
import nus.iss.smartcart.backend.repository.UserProfileRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationService {

    private final UserProfileRepository userProfileRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    // Add this constructor
    public RecommendationService(UserProfileRepository userProfileRepository,
                                 CartRepository cartRepository,
                                 OrderRepository orderRepository) {
        this.userProfileRepository = userProfileRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
    }

    public CustomerProfileDTO buildCustomerProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        List<String> cartItems = cartRepository.findProductNamesByUserId(userId);
        List<String> purchaseHistory = orderRepository.findPurchasedProductNamesByUserId(userId);

        return new CustomerProfileDTO(
            "customer-" + userId,
            profile.getInterestsList(),
            cartItems,
            List.of(), // recently_viewed
            purchaseHistory,
            profile.getPreferredCategoriesList(),
            profile.getBudget() != null ? profile.getBudget().doubleValue() : 200.00
        );
    }
}