package nus.iss.smartcart.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

// AUTHOR: Htet Nandar(Grace)
/** Small, non-sensitive subset of the admin dashboard numbers (active listing count, merchant
 * count, total revenue) shown on the public /admin/login screen before the user has signed in.
 * Deliberately its own DTO rather than reusing AdminDashboardStatsDto - keeps the public,
 * unauthenticated response limited to exactly these 3 fields even if the admin dashboard DTO
 * later grows more detailed/sensitive fields. */
@Getter
@Builder
public class PublicStatsDto {
    private long activeListings;
    private long activeMerchants;
    private BigDecimal totalRevenue;
}
