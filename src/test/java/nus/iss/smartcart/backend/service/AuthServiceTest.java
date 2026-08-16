package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.LoginRequest;
import nus.iss.smartcart.backend.dto.LoginResponse;
import nus.iss.smartcart.backend.dto.RegisterRequest;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {

        testUser = new User();

        testUser.setId(1L);
        testUser.setUsername("junior");
        testUser.setEmail("junior@smartcart.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setStatus(UserStatus.ACTIVE);
    }

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

    //MERCHANT
    @Test
    void registerMerchant_success() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("merchant01");
        request.setEmail("merchant@smartcart.com");
        request.setPassword("Merchant1");


        User merchantUser =
                new User();

        merchantUser.setId(2L);
        merchantUser.setUsername("merchant01");
        merchantUser.setEmail("merchant@smartcart.com");
        merchantUser.setPassword("encodedMerchantPassword");
        merchantUser.setRole(UserRole.MERCHANT);
        merchantUser.setStatus(UserStatus.ACTIVE);


        when(userRepository.existsByEmail(
                "merchant@smartcart.com"
        )).thenReturn(false);


        when(userRepository.existsByUsername(
                "merchant01"
        )).thenReturn(false);


        when(passwordEncoder.encode(
                "Merchant1"
        )).thenReturn("encodedMerchantPassword");


        when(userRepository.save(
                any(User.class)
        )).thenReturn(merchantUser);


        when(jwtService.generateToken(
                merchantUser
        )).thenReturn("merchant-jwt-token");


        LoginResponse response =
                authService.registerMerchant(request);


        assertNotNull(response);


        assertEquals(
                "merchant-jwt-token",
                response.getToken()
        );


        assertEquals(
                2L,
                response.getUserId()
        );


        assertEquals(
                "merchant01",
                response.getUsername()
        );


        assertEquals(
                "merchant@smartcart.com",
                response.getEmail()
        );


        assertEquals(
                "MERCHANT",
                response.getRole()
        );


        verify(userRepository)
                .existsByEmail(
                        "merchant@smartcart.com"
                );


        verify(userRepository)
                .existsByUsername("merchant01");


        verify(passwordEncoder)
                .encode("Merchant1");


        verify(userRepository)
                .save(any(User.class));


        verify(jwtService)
                .generateToken(merchantUser);
    }

    @Test
    void registerMerchant_passwordRequired() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("merchant01");
        request.setEmail("merchant@smartcart.com");
        request.setPassword(null);


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                authService.registerMerchant(request)
                );


        assertEquals(
                "Password is required",
                exception.getMessage()
        );
    }

    @Test
    void registerMerchant_passwordTooShort() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("merchant01");
        request.setEmail("merchant@smartcart.com");
        request.setPassword("Mer1");


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                authService.registerMerchant(request)
                );


        assertEquals(
                "Password must be at least 6 characters",
                exception.getMessage()
        );
    }

    @Test
    void registerMerchant_passwordWithoutUppercase() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("merchant01");
        request.setEmail("merchant@smartcart.com");
        request.setPassword("merchant1");


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                authService.registerMerchant(request)
                );


        assertEquals(
                "Password must contain at least one uppercase letter",
                exception.getMessage()
        );
    }

    @Test
    void registerMerchant_passwordWithoutLowercase() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("merchant01");
        request.setEmail("merchant@smartcart.com");
        request.setPassword("MERCHANT1");


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                authService.registerMerchant(request)
                );


        assertEquals(
                "Password must contain at least one lowercase letter",
                exception.getMessage()
        );
    }

    @Test
    void registerMerchant_passwordWithoutNumber() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("merchant01");
        request.setEmail("merchant@smartcart.com");
        request.setPassword("Merchant");


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                authService.registerMerchant(request)
                );


        assertEquals(
                "Password must contain at least one number",
                exception.getMessage()
        );
    }

    @Test
    void registerMerchant_emailAlreadyExists() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("merchant01");
        request.setEmail("merchant@smartcart.com");
        request.setPassword("Merchant1");


        when(userRepository.existsByEmail(
                "merchant@smartcart.com"
        )).thenReturn(true);


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                authService.registerMerchant(request)
                );


        assertEquals(
                "Email is already registered",
                exception.getMessage()
        );


        verify(userRepository)
                .existsByEmail(
                        "merchant@smartcart.com"
                );
    }

    @Test
    void registerMerchant_usernameAlreadyExists() {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("merchant01");
        request.setEmail("merchant@smartcart.com");
        request.setPassword("Merchant1");


        when(userRepository.existsByEmail(
                "merchant@smartcart.com"
        )).thenReturn(false);


        when(userRepository.existsByUsername(
                "merchant01"
        )).thenReturn(true);


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                authService.registerMerchant(request)
                );


        assertEquals(
                "Username is already registered",
                exception.getMessage()
        );


        verify(userRepository)
                .existsByEmail(
                        "merchant@smartcart.com"
                );


        verify(userRepository)
                .existsByUsername("merchant01");
    }

    //CUSTOMER LOGIN SUCCESS
    @Test
    void login_customerSuccess() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "junior@smartcart.com"
        );

        request.setPassword(
                "Password1"
        );


        when(userRepository.findByEmail(
                "junior@smartcart.com"
        )).thenReturn(
                Optional.of(testUser)
        );


        when(passwordEncoder.matches(
                "Password1",
                "encodedPassword"
        )).thenReturn(true);


        when(jwtService.generateToken(
                testUser
        )).thenReturn("customer-jwt-token");


        LoginResponse response =
                authService.login(request);


        assertNotNull(response);


        assertEquals(
                "customer-jwt-token",
                response.getToken()
        );


        assertEquals(
                1L,
                response.getUserId()
        );


        assertEquals(
                "junior",
                response.getUsername()
        );


        assertEquals(
                "junior@smartcart.com",
                response.getEmail()
        );


        assertEquals(
                "CUSTOMER",
                response.getRole()
        );


        verify(userRepository)
                .findByEmail(
                        "junior@smartcart.com"
                );


        verify(passwordEncoder)
                .matches(
                        "Password1",
                        "encodedPassword"
                );


        verify(jwtService)
                .generateToken(testUser);
    }

    //MERCHANT LOGIN SUCCESS
    @Test
    void login_merchantSuccess() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "merchant@smartcart.com"
        );

        request.setPassword(
                "Merchant1"
        );


        User merchantUser =
                new User();

        merchantUser.setId(2L);
        merchantUser.setUsername("merchant01");
        merchantUser.setEmail("merchant@smartcart.com");
        merchantUser.setPassword("encodedMerchantPassword");
        merchantUser.setRole(UserRole.MERCHANT);
        merchantUser.setStatus(UserStatus.ACTIVE);


        when(userRepository.findByEmail(
                "merchant@smartcart.com"
        )).thenReturn(
                Optional.of(merchantUser)
        );


        when(passwordEncoder.matches(
                "Merchant1",
                "encodedMerchantPassword"
        )).thenReturn(true);


        when(jwtService.generateToken(
                merchantUser
        )).thenReturn("merchant-jwt-token");


        LoginResponse response =
                authService.login(request);


        assertNotNull(response);


        assertEquals(
                "merchant-jwt-token",
                response.getToken()
        );


        assertEquals(
                2L,
                response.getUserId()
        );


        assertEquals(
                "merchant01",
                response.getUsername()
        );


        assertEquals(
                "merchant@smartcart.com",
                response.getEmail()
        );


        assertEquals(
                "MERCHANT",
                response.getRole()
        );
    }

    //EMAIL NOT EXIST
    @Test
    void login_emailNotFound() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "unknown@smartcart.com"
        );

        request.setPassword(
                "Password1"
        );


        when(userRepository.findByEmail(
                "unknown@smartcart.com"
        )).thenReturn(
                Optional.empty()
        );


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                authService.login(request)
                );


        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );


        verify(userRepository)
                .findByEmail(
                        "unknown@smartcart.com"
                );
    }

    //USER ACCOUNT NOT ACTIVE
    @Test
    void login_inactiveAccount() {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "junior@smartcart.com"
        );

        request.setPassword(
                "Password1"
        );


        testUser.setStatus(
                UserStatus.INACTIVE
        );


        when(userRepository.findByEmail(
                "junior@smartcart.com"
        )).thenReturn(
                Optional.of(testUser)
        );


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                authService.login(request)
                );


        assertEquals(
                "User account is inactive",
                exception.getMessage()
        );


        verify(userRepository)
                .findByEmail(
                        "junior@smartcart.com"
                );
    }


}
