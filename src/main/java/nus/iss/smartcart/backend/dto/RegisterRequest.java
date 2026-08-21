package nus.iss.smartcart.backend.dto;

import lombok.Getter;
import lombok.Setter;

//Author: Junior

@Getter
@Setter
public class RegisterRequest {

    private String username;
    private String email;
    private String password;

    public RegisterRequest() {
        // Intentionally left empty - required by Jackson for JSON deserialization
    }
}