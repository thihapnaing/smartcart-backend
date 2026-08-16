package nus.iss.smartcart.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AUTHOR: Htet Nandar(Grace)

/** Body for POST /api/auth/change-password. confirmPassword can't be checked with a bean
 * validation annotation alone (it's a cross-field rule), so AuthService.changePassword()
 * checks newPassword.equals(confirmPassword) itself. */
@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
