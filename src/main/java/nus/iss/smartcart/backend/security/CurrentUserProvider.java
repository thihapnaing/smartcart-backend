package nus.iss.smartcart.backend.security;

import nus.iss.smartcart.backend.exception.ForbiddenException;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// AUTHOR: Htet Nandar(Grace)
/**
 * Resolves the caller's User entity.
 *
 * All three - getCurrentAdmin(), getCurrentMerchant(), getCurrentCustomer() - read the real
 * JwtAuthenticationFilter SecurityContext and enforce the matching role, throwing
 * ForbiddenException if no one is authenticated or the authenticated account has a different
 * role. Customer/merchant login shares one endpoint (POST /api/auth/login) with admin - see
 * AuthController/AuthService - so the same real-auth path works for all three roles.
 */
@Component
public class CurrentUserProvider {
    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentMerchant() {
        return getCurrentUserWithRole(UserRole.MERCHANT);
    }

    public User getCurrentCustomer() {
        return getCurrentUserWithRole(UserRole.CUSTOMER);
    }

    public User getCurrentAdmin() {
        return getCurrentUserWithRole(UserRole.ADMIN);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Not authenticated.");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ForbiddenException("Authenticated user no longer exists."));
    }

    private User getCurrentUserWithRole(UserRole expectedRole) {
        User user = getCurrentUser();

        if (user.getRole() != expectedRole) {
            throw new ForbiddenException("This action requires a " + expectedRole + " account.");
        }

        return user;
    }
}
