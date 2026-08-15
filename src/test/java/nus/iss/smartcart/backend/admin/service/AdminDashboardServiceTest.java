package nus.iss.smartcart.backend.admin.service;

// AUTHOR: Htet Nandar(Grace)

import nus.iss.smartcart.backend.admin.dto.AdminDashboardStatsDto;
import nus.iss.smartcart.backend.admin.dto.CategoryCountDto;
import nus.iss.smartcart.backend.admin.dto.GenderCountDto;
import nus.iss.smartcart.backend.dto.PublicStatsDto;
import nus.iss.smartcart.backend.model.Category;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.model.OrderStatus;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.OrderRepository;
import nus.iss.smartcart.backend.repository.ProductRepository;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers the admin dashboard overview stats: the admin guard, revenue is platform-wide (not
 * per-merchant) and only counts paid/packed/delivered orders, listing/merchant counts, the
 * category and gender breakdowns, and that recent listings are capped at 6 and sorted
 * newest-first.
 */
@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private AdminDashboardService adminDashboardService;

    private Product mockProduct(long id, String name, String categoryName, Gender gender,
                                 ProductStatus status, LocalDateTime createdAt) {
        Category category = mock(Category.class);
        lenient().when(category.getName()).thenReturn(categoryName);

        Product product = mock(Product.class);
        lenient().when(product.getId()).thenReturn(id);
        lenient().when(product.getName()).thenReturn(name);
        lenient().when(product.getPrice()).thenReturn(BigDecimal.valueOf(19.9));
        lenient().when(product.getImageUrl()).thenReturn("/assets/products/" + name);
        lenient().when(product.getCategory()).thenReturn(category);
        lenient().when(product.getShopName()).thenReturn("SmartCart Official");
        lenient().when(product.getGender()).thenReturn(gender);
        lenient().when(product.getStatus()).thenReturn(status);
        lenient().when(product.getCreatedAt()).thenReturn(createdAt);
        return product;
    }

    private Order mockOrder(OrderStatus status, BigDecimal totalAmount) {
        Order order = mock(Order.class);
        lenient().when(order.getStatus()).thenReturn(status);
        lenient().when(order.getTotalAmount()).thenReturn(totalAmount);
        return order;
    }

    @Test
    void getStats_callsAdminGuard() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        adminDashboardService.getStats();

        verify(currentUserProvider).getCurrentAdmin();
    }

    @Test
    void getStats_countsActiveAndInactiveListingsSeparately() {
        LocalDateTime old = LocalDateTime.now().minusDays(30);
        List<Product> products = List.of(
                mockProduct(1, "Tee", "Tops", Gender.MEN, ProductStatus.ACTIVE, old),
                mockProduct(2, "Jeans", "Bottoms", Gender.MEN, ProductStatus.ACTIVE, old),
                mockProduct(3, "Sneakers", "Shoes", Gender.WOMEN, ProductStatus.INACTIVE, old)
        );
        when(productRepository.findAll()).thenReturn(products);
        when(orderRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        AdminDashboardStatsDto stats = adminDashboardService.getStats();

        assertEquals(2, stats.getActiveListings());
        assertEquals(1, stats.getInactiveListings());
    }

    @Test
    void getStats_newListingsThisWeek_onlyCountsProductsCreatedWithinLast7Days() {
        LocalDateTime today = LocalDateTime.now().minusHours(1);
        LocalDateTime old = LocalDateTime.now().minusDays(30);
        List<Product> products = List.of(
                mockProduct(1, "Tee", "Tops", Gender.MEN, ProductStatus.ACTIVE, today),
                mockProduct(2, "Jeans", "Bottoms", Gender.MEN, ProductStatus.ACTIVE, old)
        );
        when(productRepository.findAll()).thenReturn(products);
        when(orderRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        AdminDashboardStatsDto stats = adminDashboardService.getStats();

        assertEquals(1, stats.getNewListingsThisWeek());
    }

    @Test
    void getStats_totalRevenue_isPlatformWide_onlyCountsPaidPackedDelivered() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        // Built as a separate statement (not inline inside when().thenReturn()) - constructing
        // mocks inline there confuses Mockito's stubbing tracker and throws
        // UnfinishedStubbingException, since each mockOrder() call itself starts/finishes its
        // own when()/thenReturn() pair while the outer one is still "open".
        List<Order> orders = List.of(
                mockOrder(OrderStatus.PAID, BigDecimal.valueOf(100)),
                mockOrder(OrderStatus.PACKED, BigDecimal.valueOf(50)),
                mockOrder(OrderStatus.DELIVERED, BigDecimal.valueOf(25)),
                mockOrder(OrderStatus.PENDING, BigDecimal.valueOf(999)),
                mockOrder(OrderStatus.CANCELLED, BigDecimal.valueOf(999)),
                mockOrder(OrderStatus.PAID, null)
        );
        when(orderRepository.findAll()).thenReturn(orders);

        AdminDashboardStatsDto stats = adminDashboardService.getStats();

        assertEquals(0, BigDecimal.valueOf(175).compareTo(stats.getTotalRevenue()));
    }

    @Test
    void getStats_activeMerchants_countsOnlyActiveMerchantRoleUsers() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of());

        User merchant1 = mock(User.class);
        when(merchant1.getRole()).thenReturn(UserRole.MERCHANT);
        when(merchant1.getStatus()).thenReturn(UserStatus.ACTIVE);
        User merchant2 = mock(User.class);
        when(merchant2.getRole()).thenReturn(UserRole.MERCHANT);
        when(merchant2.getStatus()).thenReturn(UserStatus.ACTIVE);
        // A suspended account still counts as MERCHANT-role but must NOT count as active -
        // see AdminMerchantService for the ACTIVE <-> SUSPENDED lifecycle.
        User suspendedMerchant = mock(User.class);
        when(suspendedMerchant.getRole()).thenReturn(UserRole.MERCHANT);
        when(suspendedMerchant.getStatus()).thenReturn(UserStatus.SUSPENDED);
        User customer = mock(User.class);
        when(customer.getRole()).thenReturn(UserRole.CUSTOMER);
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);

        when(userRepository.findAll())
                .thenReturn(List.of(merchant1, merchant2, suspendedMerchant, customer, admin));

        AdminDashboardStatsDto stats = adminDashboardService.getStats();

        assertEquals(2, stats.getActiveMerchants());
    }

    @Test
    void getStats_categoryBreakdown_groupsByNameAndSortsDescendingByCount() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> products = List.of(
                mockProduct(1, "Tee", "Tops", Gender.MEN, ProductStatus.ACTIVE, now),
                mockProduct(2, "Hoodie", "Tops", Gender.MEN, ProductStatus.ACTIVE, now),
                mockProduct(3, "Sneakers", "Shoes", Gender.WOMEN, ProductStatus.ACTIVE, now)
        );
        when(productRepository.findAll()).thenReturn(products);
        when(orderRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        AdminDashboardStatsDto stats = adminDashboardService.getStats();

        List<CategoryCountDto> breakdown = stats.getCategoryBreakdown();
        assertEquals(2, breakdown.size());
        assertEquals("Tops", breakdown.get(0).getCategoryName());
        assertEquals(2, breakdown.get(0).getCount());
        assertEquals("Shoes", breakdown.get(1).getCategoryName());
        assertEquals(1, breakdown.get(1).getCount());
    }

    @Test
    void getStats_genderSplit_computesPercentagesAndIncludesGendersWithZeroProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> products = List.of(
                mockProduct(1, "Tee", "Tops", Gender.MEN, ProductStatus.ACTIVE, now),
                mockProduct(2, "Hoodie", "Tops", Gender.MEN, ProductStatus.ACTIVE, now),
                mockProduct(3, "Sneakers", "Shoes", Gender.MEN, ProductStatus.ACTIVE, now)
        );
        when(productRepository.findAll()).thenReturn(products);
        when(orderRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        AdminDashboardStatsDto stats = adminDashboardService.getStats();

        List<GenderCountDto> genderSplit = stats.getGenderSplit();
        assertEquals(2, genderSplit.size());

        GenderCountDto men = genderSplit.stream().filter(g -> g.getGender().equals("MEN")).findFirst().orElseThrow();
        GenderCountDto women = genderSplit.stream().filter(g -> g.getGender().equals("WOMEN")).findFirst().orElseThrow();

        assertEquals(3, men.getCount());
        assertEquals(100.0, men.getPercentage());
        assertEquals(0, women.getCount());
        assertEquals(0.0, women.getPercentage());
    }

    @Test
    void getStats_recentListings_cappedAtSix_sortedNewestFirst_skipsNullCreatedAt() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> products = List.of(
                mockProduct(1, "Oldest", "Tops", Gender.MEN, ProductStatus.ACTIVE, now.minusDays(7)),
                mockProduct(2, "Newest", "Tops", Gender.MEN, ProductStatus.ACTIVE, now),
                mockProduct(3, "Middle", "Tops", Gender.MEN, ProductStatus.ACTIVE, now.minusDays(3)),
                mockProduct(4, "NoDate", "Tops", Gender.MEN, ProductStatus.ACTIVE, null),
                mockProduct(5, "P5", "Tops", Gender.MEN, ProductStatus.ACTIVE, now.minusDays(1)),
                mockProduct(6, "P6", "Tops", Gender.MEN, ProductStatus.ACTIVE, now.minusDays(2)),
                mockProduct(7, "P7", "Tops", Gender.MEN, ProductStatus.ACTIVE, now.minusDays(4)),
                mockProduct(8, "P8", "Tops", Gender.MEN, ProductStatus.ACTIVE, now.minusDays(5))
        );
        when(productRepository.findAll()).thenReturn(products);
        when(orderRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        AdminDashboardStatsDto stats = adminDashboardService.getStats();

        assertEquals(6, stats.getRecentListings().size());
        assertEquals("Newest", stats.getRecentListings().get(0).getName());
        assertTrue(stats.getRecentListings().stream().noneMatch(p -> p.getName().equals("NoDate")));
        assertTrue(stats.getRecentListings().stream().noneMatch(p -> p.getName().equals("Oldest")));
    }

    // ── getPublicStats ───────────────────────────────────────────────────
    // Backs the unauthenticated /admin/login screen (PublicStatsController) - unlike getStats(),
    // must NOT require an already-logged-in admin.

    @Test
    void getPublicStats_neverCallsTheAdminGuard() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        adminDashboardService.getPublicStats();

        verifyNoInteractions(currentUserProvider);
    }

    @Test
    void getPublicStats_countsActiveListingsOnly() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> products = List.of(
                mockProduct(1, "Tee", "Tops", Gender.MEN, ProductStatus.ACTIVE, now),
                mockProduct(2, "Jeans", "Bottoms", Gender.MEN, ProductStatus.ACTIVE, now),
                mockProduct(3, "Sneakers", "Shoes", Gender.WOMEN, ProductStatus.INACTIVE, now)
        );
        when(productRepository.findAll()).thenReturn(products);
        when(orderRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        PublicStatsDto stats = adminDashboardService.getPublicStats();

        assertEquals(2, stats.getActiveListings());
    }

    @Test
    void getPublicStats_countsActiveMerchantRoleUsersOnly() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of());

        User merchant = mock(User.class);
        when(merchant.getRole()).thenReturn(UserRole.MERCHANT);
        when(merchant.getStatus()).thenReturn(UserStatus.ACTIVE);
        User suspendedMerchant = mock(User.class);
        when(suspendedMerchant.getRole()).thenReturn(UserRole.MERCHANT);
        when(suspendedMerchant.getStatus()).thenReturn(UserStatus.SUSPENDED);
        User customer = mock(User.class);
        when(customer.getRole()).thenReturn(UserRole.CUSTOMER);

        when(userRepository.findAll()).thenReturn(List.of(merchant, suspendedMerchant, customer));

        PublicStatsDto stats = adminDashboardService.getPublicStats();

        assertEquals(1, stats.getActiveMerchants());
    }

    @Test
    void getPublicStats_totalRevenue_onlyCountsPaidPackedDelivered() {
        when(productRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());

        List<Order> orders = List.of(
                mockOrder(OrderStatus.PAID, BigDecimal.valueOf(100)),
                mockOrder(OrderStatus.PACKED, BigDecimal.valueOf(50)),
                mockOrder(OrderStatus.DELIVERED, BigDecimal.valueOf(25)),
                mockOrder(OrderStatus.PENDING, BigDecimal.valueOf(999)),
                mockOrder(OrderStatus.CANCELLED, BigDecimal.valueOf(999)),
                mockOrder(OrderStatus.PAID, null)
        );
        when(orderRepository.findAll()).thenReturn(orders);

        PublicStatsDto stats = adminDashboardService.getPublicStats();

        assertEquals(0, BigDecimal.valueOf(175).compareTo(stats.getTotalRevenue()));
    }
}
