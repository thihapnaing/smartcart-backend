package nus.iss.smartcart.backend.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** AUTHOR: Htet Nandar(Grace)
 *
 * Admin-only merchant detail view - everything on AdminMerchantSummaryDto, plus the order
 * activity and revenue an admin needs to judge whether a merchant should be suspended.
 * orderCount counts every non-cancelled order that included one of this merchant's products;
 * revenue only sums the ones that were actually paid for (PAID/PACKED/PICKED_UP/DELIVERED) - see
 * AdminMerchantService.getMerchantDetail() for why PENDING and CANCELLED are excluded. */
@Getter
@Builder
public class AdminMerchantDetailDto {
    private Long id;
    private String username;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private long listingCount;
    private long orderCount;
    private BigDecimal revenue;
    private String lastModifiedByAdminUsername;
    private LocalDateTime lastModifiedAt;
}
