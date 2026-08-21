package nus.iss.smartcart.backend.admin.service;

// AUTHOR: Htet Nandar(Grace)

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.admin.dto.AdminMerchantDetailDto;
import nus.iss.smartcart.backend.admin.dto.AdminMerchantSummaryDto;
import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.model.OrderItem;
import nus.iss.smartcart.backend.model.OrderStatus;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.OrderItemRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers the admin guard (getCurrentAdmin() called before either operation), the
 * newest-first ordering of the merchant list, the listing-count lookup per merchant, and -
 * most importantly - the allowed-transition table that stops an admin from putting a merchant
 * into a nonsensical status (e.g. ACTIVE straight to INACTIVE). There's no approval workflow
 * here - every merchant account is ACTIVE from creation - so the only transitions that matter
 * are ACTIVE <-> SUSPENDED, plus the legacy INACTIVE -> ACTIVE escape hatch.
 */
@ExtendWith(MockitoExtension.class)
class AdminMerchantServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private AdminMerchantService adminMerchantService;

    // lenient(): this helper is shared by tests that only ever get as far as reading
    // getRole()/getStatus() before the service throws (e.g. a disallowed transition never
    // reaches toSummaryDto(), which is what reads username/email/createdAt/id) - under
    // MockitoExtension's default strict stubbing, those tests would otherwise fail with
    // UnnecessaryStubbingException for the fields their code path never touches.
    private User merchant(long id, String username, String email, UserStatus status, LocalDateTime createdAt) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getUsername()).thenReturn(username);
        lenient().when(user.getEmail()).thenReturn(email);
        lenient().when(user.getRole()).thenReturn(UserRole.MERCHANT);
        lenient().when(user.getStatus()).thenReturn(status);
        lenient().when(user.getCreatedAt()).thenReturn(createdAt);
        return user;
    }

    // ── getAllMerchants ──────────────────────────────────────────────────

    @Test
    void getAllMerchants_returnsEveryMerchantRegardlessOfStatus_mappedToSummaryDto() {
        LocalDateTime now = LocalDateTime.now();
        User active = merchant(1L, "acme", "acme@smartcart.demo", UserStatus.ACTIVE, now.minusDays(2));

        when(userRepository.findByRole(UserRole.MERCHANT)).thenReturn(List.of(active));
        when(productRepository.findByMerchantId(1L)).thenReturn(List.of(mock(Product.class), mock(Product.class)));

        List<AdminMerchantSummaryDto> results = adminMerchantService.getAllMerchants();

        verify(currentUserProvider).getCurrentAdmin();
        assertEquals(1, results.size());
        assertEquals("acme", results.get(0).getUsername());
        assertEquals("acme@smartcart.demo", results.get(0).getEmail());
        assertEquals("ACTIVE", results.get(0).getStatus());
        assertEquals(2, results.get(0).getListingCount());
    }

    @Test
    void getAllMerchants_sortsNewestFirst() {
        LocalDateTime now = LocalDateTime.now();
        User oldActive = merchant(1L, "old-active", "old@smartcart.demo", UserStatus.ACTIVE, now.minusDays(10));
        User newActive = merchant(2L, "new-active", "new@smartcart.demo", UserStatus.ACTIVE, now.minusDays(1));
        User suspended = merchant(3L, "suspended", "suspended@smartcart.demo", UserStatus.SUSPENDED, now.minusDays(5));

        when(userRepository.findByRole(UserRole.MERCHANT)).thenReturn(List.of(oldActive, newActive, suspended));
        when(productRepository.findByMerchantId(anyLong())).thenReturn(List.of());

        List<AdminMerchantSummaryDto> results = adminMerchantService.getAllMerchants();

        assertEquals("new-active", results.get(0).getUsername());
        assertEquals("suspended", results.get(1).getUsername());
        assertEquals("old-active", results.get(2).getUsername());
    }

    @Test
    void getAllMerchants_nullCreatedAt_sortsLastWithoutThrowing() {
        User noCreatedAt = merchant(1L, "no-date", "no-date@smartcart.demo", UserStatus.ACTIVE, null);
        User withCreatedAt = merchant(2L, "has-date", "has-date@smartcart.demo", UserStatus.ACTIVE, LocalDateTime.now());

        when(userRepository.findByRole(UserRole.MERCHANT)).thenReturn(List.of(noCreatedAt, withCreatedAt));
        when(productRepository.findByMerchantId(anyLong())).thenReturn(List.of());

        List<AdminMerchantSummaryDto> results = adminMerchantService.getAllMerchants();

        assertEquals("has-date", results.get(0).getUsername());
        assertEquals("no-date", results.get(1).getUsername());
    }

    // ── updateMerchantStatus: not found ─────────────────────────────────

    @Test
    void updateMerchantStatus_merchantNotFound_throwsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> adminMerchantService.updateMerchantStatus(99L, UserStatus.ACTIVE));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMerchantStatus_userExistsButIsNotAMerchant_throwsEntityNotFoundException() {
        User customer = mock(User.class);
        when(customer.getRole()).thenReturn(UserRole.CUSTOMER);
        when(userRepository.findById(5L)).thenReturn(Optional.of(customer));

        assertThrows(EntityNotFoundException.class,
                () -> adminMerchantService.updateMerchantStatus(5L, UserStatus.ACTIVE));

        verify(userRepository, never()).save(any());
    }

    // ── updateMerchantStatus: transition guard ──────────────────────────

    @Test
    void updateMerchantStatus_sameStatus_throwsIllegalStateException() {
        User active = merchant(1L, "acme", "acme@smartcart.demo", UserStatus.ACTIVE, LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThrows(IllegalStateException.class,
                () -> adminMerchantService.updateMerchantStatus(1L, UserStatus.ACTIVE));

        verify(userRepository, never()).save(any());
    }

    private void assertDisallowedTransition(UserStatus from, UserStatus to) {
        User user = merchant(1L, "acme", "acme@smartcart.demo", from, LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> adminMerchantService.updateMerchantStatus(1L, to));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMerchantStatus_active_to_inactive_isDisallowed() {
        assertDisallowedTransition(UserStatus.ACTIVE, UserStatus.INACTIVE);
    }

    @Test
    void updateMerchantStatus_suspended_to_inactive_isDisallowed() {
        assertDisallowedTransition(UserStatus.SUSPENDED, UserStatus.INACTIVE);
    }

    @Test
    void updateMerchantStatus_inactive_to_suspended_isDisallowed() {
        assertDisallowedTransition(UserStatus.INACTIVE, UserStatus.SUSPENDED);
    }

    // ── updateMerchantStatus: allowed transitions ───────────────────────
    // A real User is used here, not a mock: the service reads getStatus() once to validate
    // the transition, calls the real setStatus(), then reads getStatus() again while building
    // the response DTO - a mock's getStatus() wouldn't reflect setStatus() being called unless
    // separately re-stubbed, which would just be asserting the stub, not the code.

    private AdminMerchantSummaryDto assertAllowedTransition(UserStatus from, UserStatus to) {
        User admin = mock(User.class);
        lenient().when(admin.getUsername()).thenReturn("grace_admin");
        when(currentUserProvider.getCurrentAdmin()).thenReturn(admin);

        User user = new User();
        user.setId(1L);
        user.setUsername("acme");
        user.setEmail("acme@smartcart.demo");
        user.setRole(UserRole.MERCHANT);
        user.setStatus(from);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(productRepository.findByMerchantId(1L)).thenReturn(List.of());

        AdminMerchantSummaryDto result = adminMerchantService.updateMerchantStatus(1L, to);

        verify(currentUserProvider).getCurrentAdmin();
        verify(userRepository).save(user);
        assertEquals(to, user.getStatus());
        assertEquals(to.name(), result.getStatus());

        // Who made this transition (and when) must be recorded on the real User, not just
        // that *an* admin was authenticated - that's the whole point of this field.
        assertEquals(admin, user.getLastModifiedByAdmin());
        assertNotNull(user.getLastModifiedAt());
        assertEquals("grace_admin", result.getLastModifiedByAdminUsername());
        return result;
    }

    @Test
    void updateMerchantStatus_active_to_suspended_suspendsTheAccount() {
        assertAllowedTransition(UserStatus.ACTIVE, UserStatus.SUSPENDED);
    }

    @Test
    void updateMerchantStatus_suspended_to_active_reinstatesTheAccount() {
        assertAllowedTransition(UserStatus.SUSPENDED, UserStatus.ACTIVE);
    }

    @Test
    void updateMerchantStatus_inactive_to_active_allowsLegacyReactivation() {
        assertAllowedTransition(UserStatus.INACTIVE, UserStatus.ACTIVE);
    }

    // ── getMerchantDetail ────────────────────────────────────────────────

    private OrderItem orderItem(Long orderId, OrderStatus status, String unitPrice, int quantity) {
        Order order = mock(Order.class);
        lenient().when(order.getId()).thenReturn(orderId);
        lenient().when(order.getStatus()).thenReturn(status);

        OrderItem item = mock(OrderItem.class);
        lenient().when(item.getOrder()).thenReturn(order);
        lenient().when(item.getUnitPrice()).thenReturn(new BigDecimal(unitPrice));
        lenient().when(item.getQuantity()).thenReturn(quantity);
        return item;
    }

    @Test
    void getMerchantDetail_merchantNotFound_throwsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> adminMerchantService.getMerchantDetail(99L));
    }

    @Test
    void getMerchantDetail_userExistsButIsNotAMerchant_throwsEntityNotFoundException() {
        User customer = mock(User.class);
        when(customer.getRole()).thenReturn(UserRole.CUSTOMER);
        when(userRepository.findById(5L)).thenReturn(Optional.of(customer));

        assertThrows(EntityNotFoundException.class, () -> adminMerchantService.getMerchantDetail(5L));
    }

    @Test
    void getMerchantDetail_noOrders_returnsZeroCountAndZeroRevenue() {
        User active = merchant(1L, "acme", "acme@smartcart.demo", UserStatus.ACTIVE, LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(active));
        when(productRepository.findByMerchantId(1L)).thenReturn(List.of());
        when(orderItemRepository.findByProductVariantProductMerchantId(1L)).thenReturn(List.of());

        AdminMerchantDetailDto result = adminMerchantService.getMerchantDetail(1L);

        verify(currentUserProvider).getCurrentAdmin();
        assertEquals(0, result.getOrderCount());
        assertEquals(BigDecimal.ZERO, result.getRevenue());
        assertEquals(0, result.getListingCount());
    }

    @Test
    void getMerchantDetail_countsOrdersAndRevenue_excludingCancelledAndPending() {
        User active = merchant(1L, "acme", "acme@smartcart.demo", UserStatus.ACTIVE, LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(active));
        when(productRepository.findByMerchantId(1L)).thenReturn(List.of(mock(Product.class)));

        List<OrderItem> items = List.of(
                orderItem(101L, OrderStatus.PAID, "10.00", 2),       // counted: order + revenue 20.00
                orderItem(102L, OrderStatus.DELIVERED, "5.00", 1),   // counted: order + revenue 5.00
                orderItem(103L, OrderStatus.CANCELLED, "50.00", 3),  // excluded entirely
                orderItem(104L, OrderStatus.PENDING, "100.00", 1)    // counted in orderCount only, not revenue
        );
        when(orderItemRepository.findByProductVariantProductMerchantId(1L)).thenReturn(items);

        AdminMerchantDetailDto result = adminMerchantService.getMerchantDetail(1L);

        assertEquals(3, result.getOrderCount()); // 101, 102, 104 - not the cancelled 103
        assertEquals(0, new BigDecimal("25.00").compareTo(result.getRevenue()));
        assertEquals(1, result.getListingCount());
    }

    @Test
    void getMerchantDetail_reflectsWhoLastChangedTheMerchantsStatus() {
        User admin = mock(User.class);
        lenient().when(admin.getUsername()).thenReturn("grace_admin");
        User active = merchant(1L, "acme", "acme@smartcart.demo", UserStatus.SUSPENDED, LocalDateTime.now());
        LocalDateTime changedAt = LocalDateTime.now().minusHours(2);
        when(active.getLastModifiedByAdmin()).thenReturn(admin);
        when(active.getLastModifiedAt()).thenReturn(changedAt);

        when(userRepository.findById(1L)).thenReturn(Optional.of(active));
        when(productRepository.findByMerchantId(1L)).thenReturn(List.of());
        when(orderItemRepository.findByProductVariantProductMerchantId(1L)).thenReturn(List.of());

        AdminMerchantDetailDto result = adminMerchantService.getMerchantDetail(1L);

        assertEquals("grace_admin", result.getLastModifiedByAdminUsername());
        assertEquals(changedAt, result.getLastModifiedAt());
    }

    @Test
    void getMerchantDetail_multipleItemsSameOrder_countOrderOnceButSumRevenueForEach() {
        User active = merchant(1L, "acme", "acme@smartcart.demo", UserStatus.ACTIVE, LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(active));
        when(productRepository.findByMerchantId(1L)).thenReturn(List.of());

        List<OrderItem> items = List.of(
                orderItem(200L, OrderStatus.PAID, "10.00", 1),
                orderItem(200L, OrderStatus.PAID, "3.00", 2)
        );
        when(orderItemRepository.findByProductVariantProductMerchantId(1L)).thenReturn(items);

        AdminMerchantDetailDto result = adminMerchantService.getMerchantDetail(1L);

        assertEquals(1, result.getOrderCount());
        assertEquals(0, new BigDecimal("16.00").compareTo(result.getRevenue()));
    }
}
