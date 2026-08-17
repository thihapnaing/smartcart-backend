package nus.iss.smartcart.backend.dto;

import lombok.Getter;
import lombok.Setter;
import nus.iss.smartcart.backend.model.UserRole;

//Author: Junior

@Getter
@Setter
public class RegisterRequest {

    private String username;
    private String email;
    private String password;
    private UserRole role; //if not provided , register as customer role

    public RegisterRequest() {
        // Intentionally left empty - required by Jackson for JSON deserialization
    }
}