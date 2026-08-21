package nus.iss.smartcart.backend.controller;

import jakarta.validation.Valid;
import nus.iss.smartcart.backend.dto.*;
import nus.iss.smartcart.backend.service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

//Author: Junior

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private static final String MESSAGE = "message";

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(
            @RequestBody RegisterRequest request
    ) {

        try {

            LoginResponse response =
                    authService.register(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping("/merchant/register")
    public ResponseEntity<Object> registerMerchant(
            @RequestBody RegisterRequest request
    ) {

        try {

            LoginResponse response =
                    authService.registerMerchant(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(
            @RequestBody LoginRequest request
    ) {

        try {

            LoginResponse response =
                    authService.login(request);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        }
    }

    // AUTHOR: Htet Nandar(Grace)
    // Authenticated (see SecurityConfig - this path is carved out of /api/auth/**'s permitAll
    // before it, unlike register/login/logout above). Used both for the forced first-login flow
    // (an admin-invited account's mustChangePassword) and as a normal "change my password" action.
    @PostMapping("/change-password")
    public ResponseEntity<Object> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        try {

            authService.changePassword(request);

            return ResponseEntity.ok(
                    Map.of(MESSAGE, "Password changed successfully")
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    //Junior
    @PostMapping("/reset-password")
    public ResponseEntity<Object> resetPassword(
            @RequestBody ResetPasswordRequest request
    ) {

        try {

            authService.resetPassword(request);

            return ResponseEntity.ok(
                    java.util.Map.of(
                            MESSAGE,
                            "Password updated successfully"
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping("/check-email")
    public ResponseEntity<Map<String, String>> checkEmail(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            MESSAGE, "Email is required"
                    ));
        }

        boolean exists = authService.checkEmail(email.trim());

        if (!exists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            MESSAGE, "Email address not found"
                    ));
        }

        return ResponseEntity.ok(
                Map.of(
                        MESSAGE, "Email address found"
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout() {

        return ResponseEntity.ok(
                java.util.Map.of(
                        MESSAGE, "Logout successful"
                )
        );
    }
}