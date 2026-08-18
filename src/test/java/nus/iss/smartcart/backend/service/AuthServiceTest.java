package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.*;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import nus.iss.smartcart.backend.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock CurrentUserProvider currentUserProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                currentUserProvider
        );
    }

    // =========================================================
    // REGISTER
    // =========================================================

    @Test
    void register_shouldCreateCustomerSuccessfully() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("junior123");
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getPassword()).thenReturn("Password123");

        when(userRepository.existsByEmail("junior@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("junior123")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");

        User user = new User();
        user.setId(1L);
        user.setUsername("junior123");
        user.setEmail("junior@example.com");
        user.setPassword("encoded");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("token");

        LoginResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("junior123", response.getUsername());
        assertEquals("junior@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
    }

    @Test
    void register_shouldRejectShortPassword() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("junior123");
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getPassword()).thenReturn("12345");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Password must be at least 6 characters", e.getMessage());
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("junior123");
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getPassword()).thenReturn("Password123");
        when(userRepository.existsByEmail("junior@example.com")).thenReturn(true);

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Email is already registered", e.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldRejectDuplicateUsername() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("junior123");
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getPassword()).thenReturn("Password123");
        when(userRepository.existsByEmail("junior@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("junior123")).thenReturn(true);

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Username is already registered", e.getMessage());
    }

    // =========================================================
    // MERCHANT REGISTER
    // =========================================================

    @Test
    void registerMerchant_shouldCreateMerchantSuccessfully() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("merchant123");
        when(request.getEmail()).thenReturn("merchant@example.com");
        when(request.getPassword()).thenReturn("Password123");

        when(userRepository.existsByEmail("merchant@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("merchant123")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");

        User user = new User();
        user.setId(2L);
        user.setUsername("merchant123");
        user.setEmail("merchant@example.com");
        user.setPassword("encoded");
        user.setRole(UserRole.MERCHANT);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("merchant-token");

        LoginResponse response = authService.registerMerchant(request);

        assertNotNull(response);
        assertEquals("merchant-token", response.getToken());
        assertEquals("MERCHANT", response.getRole());
    }

    @Test
    void registerMerchant_shouldRejectMissingPassword() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("merchant123");
        when(request.getEmail()).thenReturn("merchant@example.com");
        when(request.getPassword()).thenReturn(" ");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerMerchant(request)
        );

        assertEquals("Password is required", e.getMessage());
    }

    @Test
    void registerMerchant_shouldRejectShortPassword() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("merchant123");
        when(request.getEmail()).thenReturn("merchant@example.com");
        when(request.getPassword()).thenReturn("abc12");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerMerchant(request)
        );

        assertEquals("Password must be at least 6 characters", e.getMessage());
    }

    @Test
    void registerMerchant_shouldRejectPasswordWithoutUppercase() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("merchant123");
        when(request.getEmail()).thenReturn("merchant@example.com");
        when(request.getPassword()).thenReturn("password123");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerMerchant(request)
        );

        assertEquals(
                "Password must contain at least one uppercase letter",
                e.getMessage()
        );
    }

    @Test
    void registerMerchant_shouldRejectPasswordWithoutLowercase() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("merchant123");
        when(request.getEmail()).thenReturn("merchant@example.com");
        when(request.getPassword()).thenReturn("PASSWORD123");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerMerchant(request)
        );

        assertEquals(
                "Password must contain at least one lowercase letter",
                e.getMessage()
        );
    }

    @Test
    void registerMerchant_shouldRejectPasswordWithoutNumber() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("merchant123");
        when(request.getEmail()).thenReturn("merchant@example.com");
        when(request.getPassword()).thenReturn("Password");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerMerchant(request)
        );

        assertEquals(
                "Password must contain at least one number",
                e.getMessage()
        );
    }

    @Test
    void registerMerchant_shouldRejectDuplicateEmail() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("merchant123");
        when(request.getEmail()).thenReturn("merchant@example.com");
        when(request.getPassword()).thenReturn("Password123");
        when(userRepository.existsByEmail("merchant@example.com")).thenReturn(true);

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerMerchant(request)
        );

        assertEquals("Email is already registered", e.getMessage());
    }

    @Test
    void registerMerchant_shouldRejectDuplicateUsername() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getUsername()).thenReturn("merchant123");
        when(request.getEmail()).thenReturn("merchant@example.com");
        when(request.getPassword()).thenReturn("Password123");
        when(userRepository.existsByEmail("merchant@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("merchant123")).thenReturn(true);

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerMerchant(request)
        );

        assertEquals("Username is already registered", e.getMessage());
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @Test
    void login_shouldReturnResponse_whenValid() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getPassword()).thenReturn("Password123");

        User user = new User();
        user.setId(1L);
        user.setUsername("junior123");
        user.setEmail("junior@example.com");
        user.setPassword("encoded");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail("junior@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "encoded"))
                .thenReturn(true);
        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("CUSTOMER", response.getRole());
    }

    @Test
    void login_shouldRejectWrongPassword() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getPassword()).thenReturn("WrongPassword");

        User user = new User();
        user.setEmail("junior@example.com");
        user.setPassword("encoded");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail("junior@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "encoded"))
                .thenReturn(false);

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid email or password", e.getMessage());
    }

    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @Test
    void changePassword_shouldUpdatePassword() {
        ChangePasswordRequest request = mock(ChangePasswordRequest.class);
        when(request.getNewPassword()).thenReturn("NewPassword123");
        when(request.getConfirmPassword()).thenReturn("NewPassword123");

        User user = new User();
        user.setMustChangePassword(true);

        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.encode("NewPassword123"))
                .thenReturn("encodedNew");

        authService.changePassword(request);

        assertEquals("encodedNew", user.getPassword());
        assertFalse(Boolean.TRUE.equals(user.getMustChangePassword()));

        verify(userRepository).save(user);
    }

    @Test
    void changePassword_shouldRejectMismatchedPasswords() {
        ChangePasswordRequest request = mock(ChangePasswordRequest.class);
        when(request.getNewPassword()).thenReturn("NewPassword123");
        when(request.getConfirmPassword()).thenReturn("Different123");

        when(currentUserProvider.getCurrentUser()).thenReturn(new User());

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword(request)
        );

        assertEquals("Passwords do not match", e.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @Test
    void resetPassword_shouldUpdatePassword() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getNewPassword()).thenReturn("NewPassword123");
        when(request.getConfirmPassword()).thenReturn("NewPassword123");

        User user = new User();
        when(userRepository.findByEmail("junior@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword123"))
                .thenReturn("encodedNew");

        authService.resetPassword(request);

        assertEquals("encodedNew", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_shouldRejectMissingEmail() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.getEmail()).thenReturn(" ");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals("Email is required", e.getMessage());
    }

    @Test
    void resetPassword_shouldRejectMissingNewPassword() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getNewPassword()).thenReturn(" ");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals("New password is required", e.getMessage());
    }

    @Test
    void resetPassword_shouldRejectMissingConfirmPassword() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getNewPassword()).thenReturn("NewPassword123");
        when(request.getConfirmPassword()).thenReturn(" ");

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals("Please confirm your password", e.getMessage());
    }

    @Test
    void resetPassword_shouldRejectUnknownEmail() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.getEmail()).thenReturn("unknown@example.com");
        when(request.getNewPassword()).thenReturn("NewPassword123");
        when(request.getConfirmPassword()).thenReturn("NewPassword123");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals("Email address not found", e.getMessage());
    }

    @Test
    void resetPassword_shouldRejectShortPassword() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getNewPassword()).thenReturn("Ab123");
        when(request.getConfirmPassword()).thenReturn("Ab123");

        when(userRepository.findByEmail("junior@example.com"))
                .thenReturn(Optional.of(new User()));

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals(
                "Password must be at least 6 characters",
                e.getMessage()
        );
    }

    @Test
    void resetPassword_shouldRejectPasswordWithoutUppercase() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getNewPassword()).thenReturn("password123");
        when(request.getConfirmPassword()).thenReturn("password123");

        when(userRepository.findByEmail("junior@example.com"))
                .thenReturn(Optional.of(new User()));

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals(
                "Password must contain at least one uppercase letter",
                e.getMessage()
        );
    }

    @Test
    void resetPassword_shouldRejectPasswordWithoutLowercase() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getNewPassword()).thenReturn("PASSWORD123");
        when(request.getConfirmPassword()).thenReturn("PASSWORD123");

        when(userRepository.findByEmail("junior@example.com"))
                .thenReturn(Optional.of(new User()));

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals(
                "Password must contain at least one lowercase letter",
                e.getMessage()
        );
    }

    @Test
    void resetPassword_shouldRejectPasswordWithoutNumber() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.getEmail()).thenReturn("Password@example.com");
        when(request.getNewPassword()).thenReturn("Password");
        when(request.getConfirmPassword()).thenReturn("Password");

        when(userRepository.findByEmail("Password@example.com"))
                .thenReturn(Optional.of(new User()));

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals(
                "Password must contain at least one number",
                e.getMessage()
        );
    }

    @Test
    void resetPassword_shouldRejectMismatchedPasswords() {
        ResetPasswordRequest request = mock(ResetPasswordRequest.class);
        when(request.getEmail()).thenReturn("junior@example.com");
        when(request.getNewPassword()).thenReturn("NewPassword123");
        when(request.getConfirmPassword()).thenReturn("Different123");

        when(userRepository.findByEmail("junior@example.com"))
                .thenReturn(Optional.of(new User()));

        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> authService.resetPassword(request)
        );

        assertEquals("Passwords do not match", e.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
    }

    // =========================================================
    // CHECK EMAIL
    // =========================================================

    @Test
    void checkEmail_shouldReturnTrue_whenEmailExists() {
        when(userRepository.findByEmail("junior@example.com"))
                .thenReturn(Optional.of(new User()));

        assertTrue(authService.checkEmail("junior@example.com"));
    }

    @Test
    void checkEmail_shouldReturnFalse_whenEmailDoesNotExist() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertFalse(authService.checkEmail("unknown@example.com"));
    }
}
