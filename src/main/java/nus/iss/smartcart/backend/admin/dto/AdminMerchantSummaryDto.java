package nus.iss.smartcart.backend.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** AUTHOR: Htet Nandar(Grace)
 *
 * Admin-only merchant account row - one User (role = MERCHANT) plus how many product listings
 * it owns, so the "manage merchants" screen doesn't need a second round trip per row.
 * lastModifiedByAdminUsername/lastModifiedAt are null until the first admin-driven status
 * change (suspend/reinstate) - see User.lastModifiedByAdmin. */
@Getter
@Builder
public class AdminMerchantSummaryDto {
    private Long id;
    private String username;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private long listingCount;
    private String lastModifiedByAdminUsername;
    private LocalDateTime lastModifiedAt;
}
