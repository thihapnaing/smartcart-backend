package nus.iss.smartcart.backend.admin.service;

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.admin.dto.AdminMerchantDetailDto;
import nus.iss.smartcart.backend.admin.dto.AdminMerchantSummaryDto;
import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.model.OrderItem;
import nus.iss.smartcart.backend.model.OrderStatus;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.OrderItemRepository;
import nus.iss.smartcart.backend.repository.ProductRepository;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// AUTHOR: Htet Nandar(Grace)
/** Admin-facing merchant account management: list every merchant account, and move one
 * through its lifecycle (ACTIVE <-> SUSPENDED). A merchant account is ACTIVE from the moment
 * it's created - there's no approval queue - so the only decision an admin makes here is
 * whether an already-operating storefront should be suspended or reinstated. See UserStatus
 * for why this reuses the same enum every other User already has instead of a separate one. */
@Service
public class AdminMerchantService {

    // Which status changes an admin is allowed to make from the merchant list/detail screen.
    // SUSPENDED is reversible back to ACTIVE (a suspension can be lifted) rather than being a
    // dead end. INACTIVE predates this workflow (the generic status every role could already be
    // set to) - only allowing INACTIVE -> ACTIVE here, not the reverse, keeps this endpoint from
    // becoming a general-purpose status editor.
    private static final Map<UserStatus, Set<UserStatus>> ALLOWED_TRANSITIONS = Map.of(
            UserStatus.ACTIVE, Set.of(UserStatus.SUSPENDED),
            UserStatus.SUSPENDED, Set.of(UserStatus.ACTIVE),
            UserStatus.INACTIVE, Set.of(UserStatus.ACTIVE)
    );

    // Orders that never got paid for shouldn't count toward revenue - a CANCELLED order was
    // called off entirely, and a still-PENDING one hasn't been paid yet, so neither reflects
    // money the merchant actually made.
    private static final Set<OrderStatus> REVENUE_STATUSES = Set.of(
            OrderStatus.PAID, OrderStatus.PACKED, OrderStatus.PICKED_UP, OrderStatus.DELIVERED
    );

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final CurrentUserProvider currentUserProvider;

    public AdminMerchantService(UserRepository userRepository, ProductRepository productRepository,
                                 OrderItemRepository orderItemRepository,
                                 CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public List<AdminMerchantSummaryDto> getAllMerchants() {
        currentUserProvider.getCurrentAdmin();

        return userRepository.findByRole(UserRole.MERCHANT).stream()
                // Most-recently-joined first.
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional
    public AdminMerchantSummaryDto updateMerchantStatus(Long merchantId, UserStatus status) {
        User admin = currentUserProvider.getCurrentAdmin();

        User merchant = findMerchant(merchantId);
        UserStatus current = merchant.getStatus();

        if (current == status) {
            throw new IllegalStateException("Merchant " + merchantId + " is already " + status + ".");
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(status)) {
            throw new IllegalStateException(
                    "Cannot move a merchant from " + current + " to " + status + ".");
        }

        merchant.setStatus(status);
        // Who suspended/reinstated this merchant and when - see User.lastModifiedByAdmin's javadoc.
        merchant.setLastModifiedByAdmin(admin);
        merchant.setLastModifiedAt(LocalDateTime.now(ZoneId.of("Asia/Singapore")));
        User saved = userRepository.save(merchant);

        return toSummaryDto(saved);
    }

    // Everything the summary row has, plus order activity and revenue - the numbers an admin
    // actually needs to judge whether a merchant should be suspended, not just approved/rejected.
    @Transactional
    public AdminMerchantDetailDto getMerchantDetail(Long merchantId) {
        currentUserProvider.getCurrentAdmin();

        User merchant = findMerchant(merchantId);
        long listingCount = productRepository.findByMerchantId(merchant.getId()).size();
        List<OrderItem> orderItems = orderItemRepository.findByProductVariantProductMerchantId(merchant.getId());

        long orderCount = orderItems.stream()
                .map(OrderItem::getOrder)
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getId)
                .distinct()
                .count();

        BigDecimal revenue = orderItems.stream()
                .filter(item -> REVENUE_STATUSES.contains(item.getOrder().getStatus()))
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminMerchantDetailDto.builder()
                .id(merchant.getId())
                .username(merchant.getUsername())
                .email(merchant.getEmail())
                .status(merchant.getStatus() != null ? merchant.getStatus().name() : null)
                .createdAt(merchant.getCreatedAt())
                .listingCount(listingCount)
                .orderCount(orderCount)
                .revenue(revenue)
                .lastModifiedByAdminUsername(
                        merchant.getLastModifiedByAdmin() != null ? merchant.getLastModifiedByAdmin().getUsername() : null)
                .lastModifiedAt(merchant.getLastModifiedAt())
                .build();
    }

    private User findMerchant(Long merchantId) {
        User user = userRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));
        if (user.getRole() != UserRole.MERCHANT) {
            throw new EntityNotFoundException("Merchant not found: " + merchantId);
        }
        return user;
    }

    private AdminMerchantSummaryDto toSummaryDto(User merchant) {
        long listingCount = productRepository.findByMerchantId(merchant.getId()).size();

        return AdminMerchantSummaryDto.builder()
                .id(merchant.getId())
                .username(merchant.getUsername())
                .email(merchant.getEmail())
                .status(merchant.getStatus() != null ? merchant.getStatus().name() : null)
                .createdAt(merchant.getCreatedAt())
                .listingCount(listingCount)
                .lastModifiedByAdminUsername(
                        merchant.getLastModifiedByAdmin() != null ? merchant.getLastModifiedByAdmin().getUsername() : null)
                .lastModifiedAt(merchant.getLastModifiedAt())
                .build();
    }
}
