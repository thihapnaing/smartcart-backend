package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.*;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import nus.iss.smartcart.backend.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

//Author: Junior

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserProvider currentUserProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CurrentUserProvider currentUserProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserProvider = currentUserProvider;
    }

    public LoginResponse register(RegisterRequest request) {

        if (request.getUsername() == null ||
                request.getUsername().isBlank()) {

            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        if (request.getUsername().contains(" ")) {

            throw new IllegalArgumentException(
                    "Username shouldn't have space"
            );
        }

        if (request.getEmail() == null ||
                request.getEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().length() < 6) {

            throw new IllegalArgumentException(
                    "Password must be at least 6 characters"
            );
        }

        if (userRepository.existsByEmail(
                request.getEmail())) {

            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        if (userRepository.existsByUsername(
                request.getUsername())) {

            throw new IllegalArgumentException(
                    "Username is already registered"
            );
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // Customer signup can only create CUSTOMER accounts.
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return new LoginResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                Boolean.TRUE.equals(savedUser.getMustChangePassword())
        );
    }

    public LoginResponse registerMerchant(RegisterRequest request) {

        if (request.getUsername() == null ||
                request.getUsername().isBlank()) {

            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        if (request.getUsername().contains(" ")) {

            throw new IllegalArgumentException(
                    "Username shouldn't have space"
            );
        }

        if (request.getEmail() == null ||
                request.getEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Password is required"
            );
        }


        if (request.getPassword().length() < 6) {

            throw new IllegalArgumentException(
                    "Password must be at least 6 characters"
            );
        }


        // AUTHOR: Htet Nandar(Grace)
        // Plain character scans instead of ".*[A-Z].*"-style regexes - those are super-linear
        // (backtracking risk grows with input length) for what's really just a "does this string
        // contain a character in this class" check, which chars().anyMatch(...) does in one
        // linear pass with no regex engine involved at all.
        if (request.getPassword().chars().noneMatch(Character::isUpperCase)) {

            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter"
            );
        }


        if (request.getPassword().chars().noneMatch(Character::isLowerCase)) {

            throw new IllegalArgumentException(
                    "Password must contain at least one lowercase letter"
            );
        }


        if (request.getPassword().chars().noneMatch(Character::isDigit)) {

            throw new IllegalArgumentException(
                    "Password must contain at least one number"
            );
        }

        if (userRepository.existsByEmail(
                request.getEmail())) {

            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        if (userRepository.existsByUsername(
                request.getUsername())) {

            throw new IllegalArgumentException(
                    "Username is already registered"
            );
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // Merchant signup creates MERCHANT accounts.
        user.setRole(UserRole.MERCHANT);

        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return new LoginResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                Boolean.TRUE.equals(savedUser.getMustChangePassword())
        );
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password"
                        )
                );

        if (user.getStatus() != UserStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "User account is inactive"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                Boolean.TRUE.equals(user.getMustChangePassword())
        );
    }

    // AUTHOR: Htet Nandar(Grace)
    /** only an admin-invited admin ever has mustChangePassword set) replace their password.
     * Always clears mustChangePassword */
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserProvider.getCurrentUser();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    //Author: Junior
    public void resetPassword(ResetPasswordRequest request) {

        if (request.getEmail() == null ||
                request.getEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        if (request.getNewPassword() == null ||
                request.getNewPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "New password is required"
            );
        }

        if (request.getConfirmPassword() == null ||
                request.getConfirmPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Please confirm your password"
            );
        }

        // Check email in users table
        User user = userRepository
                .findByEmail(request.getEmail().trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Email address not found"
                        )
                );

        // Validate password length
        if (request.getNewPassword().length() < 6) {

            throw new IllegalArgumentException(
                    "Password must be at least 6 characters"
            );
        }

        // Validate uppercase
        if (!request.getNewPassword().matches(".*[A-Z].*")) {

            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter"
            );
        }

        // Validate lowercase
        if (!request.getNewPassword().matches(".*[a-z].*")) {

            throw new IllegalArgumentException(
                    "Password must contain at least one lowercase letter"
            );
        }

        // Validate number
        if (!request.getNewPassword().matches(".*[0-9].*")) {

            throw new IllegalArgumentException(
                    "Password must contain at least one number"
            );
        }

        // Check password confirmation
        if (!request.getNewPassword().equals(
                request.getConfirmPassword())) {

            throw new IllegalArgumentException(
                    "Passwords do not match"
            );
        }

        // Encode password before saving
        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);
    }

    //Junior
    public boolean checkEmail(String email) {

        return userRepository.findByEmail(email).isPresent();
    }
}