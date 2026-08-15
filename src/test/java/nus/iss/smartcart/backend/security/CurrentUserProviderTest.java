package nus.iss.smartcart.backend.security;

// AUTHOR: Htet Nandar(Grace)

import nus.iss.smartcart.backend.exception.ForbiddenException;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderTest {

    @Mock
    private UserRepository userRepository;

    private CurrentUserProvider provider() {
        return new CurrentUserProvider(userRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email) {
        var authentication = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        email, "irrelevant", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User user(long id, String email, UserRole role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    // ── getCurrentAdmin ─────────────────────────────────────────────────

    @Test
    void getCurrentAdmin_returnsTheAuthenticatedUser_whenTheirRoleIsAdmin() {
        authenticateAs("admin@smartcart.com");
        User admin = user(4L, "admin@smartcart.com", UserRole.ADMIN);
        when(userRepository.findByEmail("admin@smartcart.com")).thenReturn(Optional.of(admin));

        User result = provider().getCurrentAdmin();

        assertSame(admin, result);
    }

    @Test
    void getCurrentAdmin_throwsForbidden_whenTheAuthenticatedUserIsNotAnAdmin() {
        authenticateAs("customer@smartcart.com");
        User customer = user(2L, "customer@smartcart.com", UserRole.CUSTOMER);
        when(userRepository.findByEmail("customer@smartcart.com")).thenReturn(Optional.of(customer));
        CurrentUserProvider provider = provider();

        assertThrows(ForbiddenException.class, provider::getCurrentAdmin);
    }

    @Test
    void getCurrentAdmin_throwsForbidden_whenNoOneIsAuthenticated() {
        CurrentUserProvider provider = provider();

        assertThrows(ForbiddenException.class, provider::getCurrentAdmin);
    }

    @Test
    void getCurrentAdmin_throwsForbidden_whenAuthenticatedButNotFullyAuthenticated() {
        // e.g. an anonymous authentication token - authenticated() is false for these.
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        CurrentUserProvider provider = provider();

        assertThrows(ForbiddenException.class, provider::getCurrentAdmin);
    }

    @Test
    void getCurrentAdmin_throwsForbidden_whenTheAuthenticatedEmailNoLongerExists() {
        authenticateAs("ghost@smartcart.com");
        when(userRepository.findByEmail("ghost@smartcart.com")).thenReturn(Optional.empty());
        CurrentUserProvider provider = provider();

        assertThrows(ForbiddenException.class, provider::getCurrentAdmin);
    }

    // ── getCurrentMerchant / getCurrentCustomer ─────────────────────────
    // Same real-auth path as getCurrentAdmin now - see the class-level comment.

    @Test
    void getCurrentMerchant_returnsTheAuthenticatedUser_whenTheirRoleIsMerchant() {
        authenticateAs("merchant@smartcart.com");
        User merchant = user(1L, "merchant@smartcart.com", UserRole.MERCHANT);
        when(userRepository.findByEmail("merchant@smartcart.com")).thenReturn(Optional.of(merchant));

        assertSame(merchant, provider().getCurrentMerchant());
    }

    @Test
    void getCurrentMerchant_throwsForbidden_whenTheAuthenticatedUserIsNotAMerchant() {
        authenticateAs("customer@smartcart.com");
        User customer = user(2L, "customer@smartcart.com", UserRole.CUSTOMER);
        when(userRepository.findByEmail("customer@smartcart.com")).thenReturn(Optional.of(customer));
        CurrentUserProvider provider = provider();

        assertThrows(ForbiddenException.class, provider::getCurrentMerchant);
    }

    @Test
    void getCurrentMerchant_throwsForbidden_whenNoOneIsAuthenticated() {
        CurrentUserProvider provider = provider();

        assertThrows(ForbiddenException.class, provider::getCurrentMerchant);
    }

    @Test
    void getCurrentCustomer_returnsTheAuthenticatedUser_whenTheirRoleIsCustomer() {
        authenticateAs("grace@smartcart.com");
        User customer = user(2L, "grace@smartcart.com", UserRole.CUSTOMER);
        when(userRepository.findByEmail("grace@smartcart.com")).thenReturn(Optional.of(customer));

        assertSame(customer, provider().getCurrentCustomer());
    }

    @Test
    void getCurrentCustomer_throwsForbidden_whenNoOneIsAuthenticated() {
        CurrentUserProvider provider = provider();

        assertThrows(ForbiddenException.class, provider::getCurrentCustomer);
    }

    @Test
    void roleMismatch_includesTheExpectedRoleInTheMessage() {
        authenticateAs("customer@smartcart.com");
        User customer = user(2L, "customer@smartcart.com", UserRole.CUSTOMER);
        when(userRepository.findByEmail("customer@smartcart.com")).thenReturn(Optional.of(customer));
        CurrentUserProvider provider = provider();

        ForbiddenException exception = assertThrows(ForbiddenException.class, provider::getCurrentAdmin);
        assertEquals("This action requires a ADMIN account.", exception.getMessage());
    }
}
