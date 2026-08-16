package nus.iss.smartcart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import nus.iss.smartcart.backend.dto.LoginRequest;
import nus.iss.smartcart.backend.dto.LoginResponse;
import nus.iss.smartcart.backend.dto.RegisterRequest;
import nus.iss.smartcart.backend.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// UPDATED BY JUNIOR

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(
                                authController
                        )
                        .build();

        objectMapper =
                new ObjectMapper();
    }

    @Test
    void register_validRequest_returnsCreatedWithLoginResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jane");
        request.setEmail("jane@example.com");
        request.setPassword("password123");

        LoginResponse response =
                new LoginResponse("fake-jwt-token", 1L, "jane", "jane@example.com", "CUSTOMER");
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
                new LoginResponse("fake-jwt-token", 1L, "jane", "jane@example.com", "CUSTOMER");
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
                        "MERCHANT"
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
                        "MERCHANT"
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
}
