package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.LoginRequest;
import nus.iss.smartcart.backend.dto.LoginResponse;
import nus.iss.smartcart.backend.dto.RegisterRequest;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jane");
        request.setEmail("jane@example.com");
        request.setPassword("password123");
        return request;
    }

    // ── register ────────────────────────────────────────────────────────

    @Test
    void register_missingUsername_throwsIllegalArgumentException() {
        RegisterRequest request = validRegisterRequest();
        request.setUsername("   ");

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    void register_missingEmail_throwsIllegalArgumentException() {
        RegisterRequest request = validRegisterRequest();
        request.setEmail(null);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_passwordTooShort_throwsIllegalArgumentException() {
        RegisterRequest request = validRegisterRequest();
        request.setPassword("abc");

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_emailAlreadyRegistered_throwsIllegalArgumentException() {
        RegisterRequest request = validRegisterRequest();
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_usernameAlreadyRegistered_throwsIllegalArgumentException() {
        RegisterRequest request = validRegisterRequest();
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("jane")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_validRequest_savesCustomerAndReturnsTokenWithUserDetails() {
        RegisterRequest request = validRegisterRequest();
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("jane")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("jane");
        savedUser.setEmail("jane@example.com");
        savedUser.setRole(UserRole.CUSTOMER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("fake-jwt-token");

        LoginResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();
        assertEquals("hashed-password", captured.getPassword());
        assertEquals(UserRole.CUSTOMER, captured.getRole());
        assertEquals(UserStatus.ACTIVE, captured.getStatus());

        assertEquals("fake-jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("jane", response.getUsername());
        assertEquals("CUSTOMER", response.getRole());
    }

    // ── login ───────────────────────────────────────────────────────────

    @Test
    void login_userNotFound_throwsIllegalArgumentException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("password123");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_inactiveAccount_throwsIllegalArgumentException() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setStatus(UserStatus.INACTIVE);
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("password123");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_wrongPassword_throwsIllegalArgumentException() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("hashed-password");
        user.setStatus(UserStatus.ACTIVE);
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("wrong-password");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void login_validCredentials_returnsTokenWithUserDetails() {
        User user = new User();
        user.setId(2L);
        user.setUsername("jane");
        user.setEmail("jane@example.com");
        user.setPassword("hashed-password");
        user.setRole(UserRole.MERCHANT);
        user.setStatus(UserStatus.ACTIVE);
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("password123");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("fake-jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals("fake-jwt-token", response.getToken());
        assertEquals(2L, response.getUserId());
        assertEquals("MERCHANT", response.getRole());
    }
}
