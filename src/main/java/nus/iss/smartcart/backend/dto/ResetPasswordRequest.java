package nus.iss.smartcart.backend.dto;

// Author: Junior

import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    private String email;
    private String newPassword;
    private String confirmPassword;

    public ResetPasswordRequest() {
    }
}