package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.ChangePasswordRequest;
import nus.iss.smartcart.backend.dto.LoginRequest;
import nus.iss.smartcart.backend.dto.LoginResponse;
import nus.iss.smartcart.backend.dto.RegisterRequest;
import nus.iss.smartcart.backend.dto.ResetPasswordRequest;
import nus.iss.smartcart.backend.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    private AuthController authController;


    // =========================================================
    // SETUP
    // =========================================================

    @BeforeEach
    void setUp() {

        authController =
                new AuthController(authService);

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(authController)
                        .build();
    }


    // =========================================================
    // REGISTER
    // =========================================================

    @Test
    void register_shouldReturn201_whenRegistrationSuccessful()
            throws Exception {

        LoginResponse response =
                mock(LoginResponse.class);

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "username": "junior123",
                                            "email": "junior@example.com",
                                            "password": "Password123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated());

        verify(authService)
                .register(any(RegisterRequest.class));
    }


    // =========================================================
    // REGISTER - ERROR
    // =========================================================

    @Test
    void register_shouldReturn400_whenRegistrationFails()
            throws Exception {

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(
                        new IllegalArgumentException(
                                "Username already exists"
                        )
                );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "username": "junior123",
                                            "email": "junior@example.com",
                                            "password": "Password123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().string("Username already exists")
                );

        verify(authService)
                .register(any(RegisterRequest.class));
    }


    // =========================================================
    // MERCHANT REGISTER
    // =========================================================

    @Test
    void registerMerchant_shouldReturn201_whenSuccessful()
            throws Exception {

        LoginResponse response =
                mock(LoginResponse.class);

        when(
                authService.registerMerchant(
                        any(RegisterRequest.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post("/api/auth/merchant/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "username": "merchant123",
                                            "email": "merchant@example.com",
                                            "password": "Password123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated());

        verify(authService)
                .registerMerchant(
                        any(RegisterRequest.class)
                );
    }


    // =========================================================
    // MERCHANT REGISTER - ERROR
    // =========================================================

    @Test
    void registerMerchant_shouldReturn400_whenRegistrationFails()
            throws Exception {

        when(
                authService.registerMerchant(
                        any(RegisterRequest.class)
                )
        ).thenThrow(
                new IllegalArgumentException(
                        "Merchant already exists"
                )
        );

        mockMvc.perform(
                        post("/api/auth/merchant/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "username": "merchant123",
                                            "email": "merchant@example.com",
                                            "password": "Password123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().string("Merchant already exists")
                );

        verify(authService)
                .registerMerchant(
                        any(RegisterRequest.class)
                );
    }


    // =========================================================
    // LOGIN
    // =========================================================

    @Test
    void login_shouldReturn200_whenLoginSuccessful()
            throws Exception {

        LoginResponse response =
                mock(LoginResponse.class);

        when(
                authService.login(
                        any(LoginRequest.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "junior@example.com",
                                            "password": "Password123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk());

        verify(authService)
                .login(any(LoginRequest.class));
    }


    // =========================================================
    // LOGIN - INVALID CREDENTIALS
    // =========================================================

    @Test
    void login_shouldReturn401_whenCredentialsAreInvalid()
            throws Exception {

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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "junior@example.com",
                                            "password": "wrongpassword"
                                        }
                                        """
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        content().string(
                                "Invalid email or password"
                        )
                );

        verify(authService)
                .login(any(LoginRequest.class));
    }


    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    @Test
    void changePassword_shouldReturn200_whenSuccessful()
            throws Exception {

        doNothing()
                .when(authService)
                .changePassword(
                        any(ChangePasswordRequest.class)
                );

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "currentPassword": "OldPassword123",
                                            "newPassword": "NewPassword123",
                                            "confirmPassword": "NewPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value("Password changed successfully")
                );

        verify(authService)
                .changePassword(
                        any(ChangePasswordRequest.class)
                );
    }


    // =========================================================
    // CHANGE PASSWORD - ERROR
    // =========================================================

    @Test
    void changePassword_shouldReturn400_whenServiceFails()
            throws Exception {

        doThrow(
                new IllegalArgumentException(
                        "Current password is incorrect"
                )
        )
                .when(authService)
                .changePassword(
                        any(ChangePasswordRequest.class)
                );

        mockMvc.perform(
                        post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "currentPassword": "WrongPassword123",
                                            "newPassword": "NewPassword123",
                                            "confirmPassword": "NewPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().string(
                                "Current password is incorrect"
                        )
                );

        verify(authService)
                .changePassword(
                        any(ChangePasswordRequest.class)
                );
    }


    // =========================================================
    // RESET PASSWORD
    // =========================================================

    @Test
    void resetPassword_shouldReturn200_whenSuccessful()
            throws Exception {

        doNothing()
                .when(authService)
                .resetPassword(
                        any(ResetPasswordRequest.class)
                );

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "junior@example.com",
                                            "newPassword": "NewPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value("Password updated successfully")
                );

        verify(authService)
                .resetPassword(
                        any(ResetPasswordRequest.class)
                );
    }


    // =========================================================
    // RESET PASSWORD - ERROR
    // =========================================================

    @Test
    void resetPassword_shouldReturn400_whenServiceFails()
            throws Exception {

        doThrow(
                new IllegalArgumentException(
                        "Email address not found"
                )
        )
                .when(authService)
                .resetPassword(
                        any(ResetPasswordRequest.class)
                );

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "unknown@example.com",
                                            "newPassword": "NewPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().string(
                                "Email address not found"
                        )
                );

        verify(authService)
                .resetPassword(
                        any(ResetPasswordRequest.class)
                );
    }


    // =========================================================
    // CHECK EMAIL - EMPTY
    // =========================================================

    @Test
    void checkEmail_shouldReturn400_whenEmailIsMissing()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/check-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {}
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Email is required")
                );

        verify(
                authService,
                never()
        ).checkEmail(anyString());
    }


    // =========================================================
    // CHECK EMAIL - SPACES
    // =========================================================

    @Test
    void checkEmail_shouldReturn400_whenEmailIsOnlySpaces()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/check-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "   "
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Email is required")
                );

        verify(
                authService,
                never()
        ).checkEmail(anyString());
    }


    // =========================================================
    // CHECK EMAIL - FOUND
    // =========================================================

    @Test
    void checkEmail_shouldReturn200_whenEmailExists()
            throws Exception {

        when(
                authService.checkEmail(
                        "junior@example.com"
                )
        ).thenReturn(true);

        mockMvc.perform(
                        post("/api/auth/check-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "junior@example.com"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value("Email address found")
                );

        verify(authService)
                .checkEmail("junior@example.com");
    }


    // =========================================================
    // CHECK EMAIL - NOT FOUND
    // =========================================================

    @Test
    void checkEmail_shouldReturn404_whenEmailDoesNotExist()
            throws Exception {

        when(
                authService.checkEmail(
                        "unknown@example.com"
                )
        ).thenReturn(false);

        mockMvc.perform(
                        post("/api/auth/check-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "unknown@example.com"
                                        }
                                        """
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.message")
                                .value("Email address not found")
                );

        verify(authService)
                .checkEmail("unknown@example.com");
    }


    // =========================================================
    // CHECK EMAIL - TRIM
    // =========================================================

    @Test
    void checkEmail_shouldTrimEmailBeforeChecking()
            throws Exception {

        when(
                authService.checkEmail(
                        "junior@example.com"
                )
        ).thenReturn(true);

        mockMvc.perform(
                        post("/api/auth/check-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "email": "  junior@example.com  "
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value("Email address found")
                );

        verify(authService)
                .checkEmail(
                        "junior@example.com"
                );
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @Test
    void logout_shouldReturn200_whenSuccessful()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/logout")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value("Logout successful")
                );
    }
}