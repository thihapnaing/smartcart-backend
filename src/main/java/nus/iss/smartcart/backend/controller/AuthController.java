package nus.iss.smartcart.backend.controller;

import jakarta.validation.Valid;
import nus.iss.smartcart.backend.dto.ChangePasswordRequest;
import nus.iss.smartcart.backend.dto.LoginRequest;
import nus.iss.smartcart.backend.dto.LoginResponse;
import nus.iss.smartcart.backend.dto.RegisterRequest;
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
                    Map.of("message", "Password changed successfully")
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout() {

        return ResponseEntity.ok(
                java.util.Map.of(
                        "message", "Logout successful"
                )
        );
    }
}