package nus.iss.smartcart.backend.admin.service;

import nus.iss.smartcart.backend.admin.dto.AdminDashboardStatsDto;
import nus.iss.smartcart.backend.admin.dto.AdminProductSummaryDto;
import nus.iss.smartcart.backend.admin.dto.CategoryCountDto;
import nus.iss.smartcart.backend.admin.dto.GenderCountDto;
import nus.iss.smartcart.backend.dto.PublicStatsDto;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.model.OrderStatus;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.OrderRepository;
import nus.iss.smartcart.backend.repository.ProductRepository;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// AUTHOR: Htet Nandar(Grace)
/** Overview stats for the admin dashboard landing page. Computes everything in-memory off
 * findAll() rather than adding a pile of one-off COUNT/SUM repository queries - the product,
 * order, and user tables are small enough (tens to low hundreds of rows) that this is simpler
 * to read and just as fast in practice. */
@Service
public class AdminDashboardService {

    private static final Set<OrderStatus> REVENUE_COUNTING_STATUSES =
            Set.of(OrderStatus.PAID, OrderStatus.PACKED, OrderStatus.DELIVERED);

    private static final int RECENT_LISTINGS_LIMIT = 6;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public AdminDashboardService(ProductRepository productRepository, OrderRepository orderRepository,
                                  UserRepository userRepository, CurrentUserProvider currentUserProvider) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public AdminDashboardStatsDto getStats() {
        currentUserProvider.getCurrentAdmin();

        List<Product> products = productRepository.findAll();
        LocalDateTime weekAgo = LocalDateTime.now(ZoneId.of("Asia/Singapore")).minusDays(7);

        long activeListings = products.stream().filter(p -> p.getStatus() == ProductStatus.ACTIVE).count();
        long inactiveListings = products.stream().filter(p -> p.getStatus() == ProductStatus.INACTIVE).count();
        long newThisWeek = products.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(weekAgo))
                .count();

        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> REVENUE_COUNTING_STATUSES.contains(o.getStatus()))
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Only ACTIVE ones - a merchant account can also be SUSPENDED (see AdminMerchantService),
        // and counting every role=MERCHANT row here would overstate this tile with accounts
        // that can't actually operate a storefront right now.
        long activeMerchants = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.MERCHANT && u.getStatus() == UserStatus.ACTIVE)
                .count();

        List<CategoryCountDto> categoryBreakdown = products.stream()
                .filter(p -> p.getCategory() != null)
                .collect(java.util.stream.Collectors.groupingBy(p -> p.getCategory().getName(), java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(e -> new CategoryCountDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(CategoryCountDto::getCount).reversed())
                .toList();

        List<GenderCountDto> genderSplit = buildGenderSplit(products);

        List<AdminProductSummaryDto> recentListings = products.stream()
                .filter(p -> p.getCreatedAt() != null)
                .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
                .limit(RECENT_LISTINGS_LIMIT)
                .map(this::toSummaryDto)
                .toList();

        return AdminDashboardStatsDto.builder()
                .totalRevenue(totalRevenue)
                .activeListings(activeListings)
                .inactiveListings(inactiveListings)
                .newListingsThisWeek(newThisWeek)
                .activeMerchants(activeMerchants)
                .categoryBreakdown(categoryBreakdown)
                .genderSplit(genderSplit)
                .recentListings(recentListings)
                .build();
    }
    
    @Transactional
    public PublicStatsDto getPublicStats() {
        long activeListings = productRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .count();

        // Only ACTIVE ones - a merchant account can also be SUSPENDED (see AdminMerchantService),
        // and counting every role=MERCHANT row here would overstate this tile with accounts
        // that can't actually operate a storefront right now.
        long activeMerchants = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.MERCHANT && u.getStatus() == UserStatus.ACTIVE)
                .count();

        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> REVENUE_COUNTING_STATUSES.contains(o.getStatus()))
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PublicStatsDto.builder()
                .activeListings(activeListings)
                .activeMerchants(activeMerchants)
                .totalRevenue(totalRevenue)
                .build();
    }

    private List<GenderCountDto> buildGenderSplit(List<Product> products) {
        Map<Gender, Long> counts = new EnumMap<>(Gender.class);
        for (Gender g : Gender.values()) {
            counts.put(g, 0L);
        }
        for (Product p : products) {
            if (p.getGender() != null) {
                counts.merge(p.getGender(), 1L, Long::sum);
            }
        }
        long total = products.size();
        return counts.entrySet().stream()
                .map(e -> new GenderCountDto(
                        e.getKey().name(),
                        e.getValue(),
                        total == 0 ? 0.0 : Math.round((e.getValue() * 1000.0) / total) / 10.0))
                .toList();
    }

    private AdminProductSummaryDto toSummaryDto(Product product) {
        return AdminProductSummaryDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory().getName())
                .shopName(product.getShopName())
                .gender(product.getGender() != null ? product.getGender().name() : null)
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .createdAt(product.getCreatedAt())
                .merchantId(product.getMerchant() != null ? product.getMerchant().getId() : null)
                .build();
    }
}
