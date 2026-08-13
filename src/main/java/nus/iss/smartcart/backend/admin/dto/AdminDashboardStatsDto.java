package nus.iss.smartcart.backend.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

// AUTHOR: Htet Nandar(Grace)
/** Everything the admin dashboard overview page needs in one call. SmartCart has no
 * pending-approval workflow (products are just ACTIVE/INACTIVE, no PENDING state), so this
 * reports inactiveListings instead of a "pending review" count. newListingsThisWeek is a real
 * qualifier (createdAt within the last 7 days) rather than a fabricated trend percentage. */
@Getter
@Builder
public class AdminDashboardStatsDto {
    private BigDecimal totalRevenue;
    private long activeListings;
    private long inactiveListings;
    private long newListingsThisWeek;
    private long activeMerchants;
    private List<CategoryCountDto> categoryBreakdown;
    private List<GenderCountDto> genderSplit;
    private List<AdminProductSummaryDto> recentListings;
}
