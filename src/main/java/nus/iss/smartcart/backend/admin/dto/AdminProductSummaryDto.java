package nus.iss.smartcart.backend.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** AUTHOR: Htet Nandar(Grace)
 *
 * Admin-only product listing row - includes status (unlike ProductSearchResult, which is
 * customer-facing and only ever shows ACTIVE products) so the moderation screen can show
 * every listing regardless of state. lastModifiedByAdminUsername/lastModifiedAt are null until
 * the first admin-driven status change - see Product.lastModifiedByAdmin. */
@Getter
@Builder
public class AdminProductSummaryDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String categoryName;
    private String shopName;
    private String gender;
    private String status;
    private LocalDateTime createdAt;
    private Long merchantId;
    private String lastModifiedByAdminUsername;
    private LocalDateTime lastModifiedAt;
}
