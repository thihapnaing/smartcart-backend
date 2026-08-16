package nus.iss.smartcart.backend.dto;

import lombok.Getter;
import lombok.Setter;

//Author: Junior

@Getter
@Setter
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String email;
    private String role;

    private boolean mustChangePassword;

    public LoginResponse(
            String token,
            Long userId,
            String username,
            String email,
            String role,
            boolean mustChangePassword
    ) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }
}