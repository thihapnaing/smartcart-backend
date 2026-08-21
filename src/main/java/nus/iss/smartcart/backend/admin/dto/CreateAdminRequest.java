package nus.iss.smartcart.backend.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// AUTHOR: Htet Nandar(Grace)

/** Body for POST /api/admin/admins - deliberately its own type rather than reusing the
 * public-facing RegisterRequest, since this one is only ever read by AdminAccountController
 * (an ADMIN-only endpoint) and validates eagerly instead of the manual if-checks
 * AuthService.register() does.
 *
 * No password field - AdminAccountService always starts a new admin on a freshly generated,
 * single-use random temporary password (see generateTemporaryPassword()) and forces a change
 * on first login, rather than trusting the inviting admin to type/share a good one. */
@Getter
@Setter
public class CreateAdminRequest {

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;
}
