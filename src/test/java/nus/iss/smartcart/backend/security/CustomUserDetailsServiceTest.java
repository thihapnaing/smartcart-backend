package nus.iss.smartcart.backend.security;

import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    private CustomUserDetailsService service() {
        return new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_activeUser_returnsEnabledUserDetailsWithRoleAuthority() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("hashed-password");
        user.setRole(UserRole.MERCHANT);
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service().loadUserByUsername("jane@example.com");

        assertEquals("jane@example.com", details.getUsername());
        assertEquals("hashed-password", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MERCHANT")));
    }

    @Test
    void loadUserByUsername_inactiveUser_returnsDisabledUserDetails() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("hashed-password");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service().loadUserByUsername("jane@example.com");

        assertFalse(details.isEnabled());
    }

    @Test
    void loadUserByUsername_noMatchingUser_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        CustomUserDetailsService service = service();

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost@example.com"));
    }
}
