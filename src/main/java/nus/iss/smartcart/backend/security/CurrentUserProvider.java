package nus.iss.smartcart.backend.security;

import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
   //TODO: once JWT auth is implemented, replace with SecurityContextHolder lookup, and verify the token's role is MERCHANT.
    public User getCurrentMerchant() {
        return userRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed data missing: expected merchant id=1 (smartcart_offical)"
                ));
    }
    //TODO: once JWT auth is implemented, replace with SecurityContextHolder lookup, and verify the token's role is CUSTOMER.
    public User getCurrentCustomer() {
        return userRepository.findById(2L)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed data missing: expected customer id=2 (grace)"
                ));
    }
}
