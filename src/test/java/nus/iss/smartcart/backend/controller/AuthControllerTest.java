package nus.iss.smartcart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nus.iss.smartcart.backend.dto.ChangePasswordRequest;
import nus.iss.smartcart.backend.dto.LoginRequest;
import nus.iss.smartcart.backend.dto.LoginResponse;
import nus.iss.smartcart.backend.dto.RegisterRequest;
import nus.iss.smartcart.backend.exception.ForbiddenException;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CustomUserDetailsService;
import nus.iss.smartcart.backend.security.JwtService;
import nus.iss.smartcart.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void register_validRequest_returnsCreatedWithLoginResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jane");
        request.setEmail("jane@example.com");
        request.setPassword("password123");

        LoginResponse response =
                new LoginResponse("fake-jwt-token", 1L, "jane", "jane@example.com", "CUSTOMER", false);
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.username").value("jane"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void register_serviceThrowsIllegalArgument_returnsBadRequestWithMessage() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jane");
        request.setEmail("jane@example.com");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Email is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email is already registered"));
    }

    @Test
    void login_validCredentials_returnsOkWithLoginResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("password123");

        LoginResponse response =
                new LoginResponse("fake-jwt-token", 1L, "jane", "jane@example.com", "CUSTOMER", false);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"));
    }

    @Test
    void login_invalidCredentials_returnsUnauthorizedWithMessage() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("wrong-password");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }

    //VALIDATION ERROR
    @Test
    void register_validationError()
            throws Exception {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("");
        request.setEmail("john@example.com");
        request.setPassword("Password1");


        when(
                authService.register(
                        any(RegisterRequest.class)
                )
        ).thenThrow(
                new IllegalArgumentException(
                        "Username is required"
                )
        );


        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isBadRequest()
                )

                .andExpect(
                        content().string(
                                "Username is required"
                        )
                );


        verify(
                authService
        ).register(
                any(RegisterRequest.class)
        );
    }

    //DUPLICATE EMAIL
    @Test
    void register_duplicateEmail()
            throws Exception {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setPassword("Password1");


        when(
                authService.register(
                        any(RegisterRequest.class)
                )
        ).thenThrow(
                new IllegalArgumentException(
                        "Email is already registered"
                )
        );


        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isBadRequest()
                )

                .andExpect(
                        content().string(
                                "Email is already registered"
                        )
                );
    }

    //MERCHANT REGISTER SUCCESS
    @Test
    void registerMerchant_success()
            throws Exception {

        RegisterRequest request =
                new RegisterRequest();

        request.setUsername("merchant01");
        request.setEmail("merchant@example.com");
        request.setPassword("Password1");


        LoginResponse response =
                new LoginResponse(
                        "merchant-jwt-token",
                        2L,
                        "merchant01",
                        "merchant@example.com",
                        "MERCHANT",
                        false
                );


        when(
                authService.registerMerchant(
                        any(RegisterRequest.class)
                )
        ).thenReturn(response);


        mockMvc.perform(
                        post("/api/auth/merchant/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isCreated()
                )

                .andExpect(
                        jsonPath("$.token")
                                .value("merchant-jwt-token")
                )

                .andExpect(
                        jsonPath("$.userId")
                                .value(2)
                )

                .andExpect(
                        jsonPath("$.username")
                                .value("merchant01")
                )

                .andExpect(
                        jsonPath("$.email")
                                .value("merchant@example.com")
                )

                .andExpect(
                        jsonPath("$.role")
                                .value("MERCHANT")
                );


        verify(
                authService
        ).registerMerchant(
                any(RegisterRequest.class)
        );
    }

    //LOGIN MERCHANT SUCCESS
    @Test
    void login_merchant_success()
            throws Exception {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "merchant@smartcart.com.sg"
        );

        request.setPassword(
                "Password1"
        );


        LoginResponse response =
                new LoginResponse(
                        "merchant-jwt-token",
                        4L,
                        "merchant01",
                        "merchant@smartcart.com.sg",
                        "MERCHANT",
                        false
                );


        when(
                authService.login(
                        any(LoginRequest.class)
                )
        ).thenReturn(response);


        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.token")
                                .value("merchant-jwt-token")
                )

                // IMPORTANT: LoginResponse uses userId
                .andExpect(
                        jsonPath("$.userId")
                                .value(4)
                )

                .andExpect(
                        jsonPath("$.username")
                                .value("merchant01")
                )

                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "merchant@smartcart.com.sg"
                                )
                )

                .andExpect(
                        jsonPath("$.role")
                                .value("MERCHANT")
                );


        verify(
                authService
        ).login(
                any(LoginRequest.class)
        );
    }

    //LOGIN VALIDATION
    @Test
    void login_invalidCredentials()
            throws Exception {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "wrong@example.com"
        );

        request.setPassword(
                "wrongpassword"
        );


        when(
                authService.login(
                        any(LoginRequest.class)
                )
        ).thenThrow(
                new IllegalArgumentException(
                        "Invalid email or password"
                )
        );


        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isUnauthorized()
                )

                .andExpect(
                        content().string(
                                "Invalid email or password"
                        )
                );


        verify(
                authService
        ).login(
                any(LoginRequest.class)
        );
    }

    //INACTIVE ACCOUNT
    @Test
    void login_inactiveAccount()
            throws Exception {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(
                "inactive@example.com"
        );

        request.setPassword(
                "Password1"
        );


        when(
                authService.login(
                        any(LoginRequest.class)
                )
        ).thenThrow(
                new IllegalArgumentException(
                        "User account is inactive"
                )
        );


        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isUnauthorized()
                )

                .andExpect(
                        content().string(
                                "User account is inactive"
                        )
                );
    }

    //LOGOUT TEST
    @Test
    void logout_success()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/logout")
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Logout successful"
                                )
                );
    }

    // ── change-password ────────────────────────────────────────────────

    @Test
    void changePassword_validRequest_returnsOkAndCallsService() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("newpassword123");
        request.setConfirmPassword("newpassword123");

        doNothing().when(authService).changePassword(any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(authService).changePassword(any(ChangePasswordRequest.class));
    }

    @Test
    void changePassword_passwordsDoNotMatch_returnsBadRequestWithMessage() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("newpassword123");
        request.setConfirmPassword("somethingElse123");

        doThrow(new IllegalArgumentException("Passwords do not match"))
                .when(authService).changePassword(any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Passwords do not match"));
    }

    @Test
    void changePassword_newPasswordTooShort_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"abc\",\"confirmPassword\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_callerNotAuthenticated_returnsForbidden() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("newpassword123");
        request.setConfirmPassword("newpassword123");

        doThrow(new ForbiddenException("Not authenticated."))
                .when(authService).changePassword(any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
