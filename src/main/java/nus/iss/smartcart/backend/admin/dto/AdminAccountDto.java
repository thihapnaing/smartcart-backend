package nus.iss.smartcart.backend.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** AUTHOR: Htet Nandar(Grace)
 *
 * One admin (role = ADMIN) account row - shared shape for both GET /api/admin/admins (the
 * list) and the POST /api/admin/admins response. Deliberately carries no token: unlike
 * AuthController's self-service register endpoints, the caller here is an already-authenticated
 * admin, so there's no session to hand back.
 *
 */
@Getter
@Builder(toBuilder = true)
public class AdminAccountDto {
    private Long id;
    private String username;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private boolean mustChangePassword;
    private String temporaryPassword;
}
