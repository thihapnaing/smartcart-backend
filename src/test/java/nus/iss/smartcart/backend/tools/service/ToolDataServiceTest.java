package nus.iss.smartcart.backend.tools.service;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.dto.ProductSearchResult;
import nus.iss.smartcart.backend.model.Category;
import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.model.OrderItem;
import nus.iss.smartcart.backend.model.OrderStatus;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductVariant;
import nus.iss.smartcart.backend.repository.OrderRepository;
import nus.iss.smartcart.backend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * ProductVariant.product and Order.id have no public setters (JPA entities only expose them via
 * Hibernate reflection/@GeneratedValue), so fixtures here use ReflectionTestUtils to set them -
 * the standard Spring workaround for this exact situation.
 */
@ExtendWith(MockitoExtension.class)
class ToolDataServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    private ToolDataService service() {
        return new ToolDataService(orderRepository, productService);
    }

    // ── Fixture builders ──────────────────────────────────────────────────

    private Category category(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    private ProductVariant variantOf(Category category) {
        Product product = new Product();
        product.setCategory(category);
        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "product", product);
        return variant;
    }

    private Order order(long id, BigDecimal totalAmount, OrderStatus status, LocalDateTime orderDate, ProductVariant... variants) {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", id);
        order.setTotalAmount(totalAmount);
        order.setStatus(status);
        order.setOrderDate(orderDate);
        List<OrderItem> items = new ArrayList<>();
        for (ProductVariant variant : variants) {
            OrderItem item = new OrderItem();
            item.setProductVariant(variant);
            items.add(item);
        }
        order.setItems(items);
        return order;
    }

    // ── getOrderHistory ──────────────────────────────────────────────────

    @Test
    void getOrderHistory_topCategoryComesFromTheMostRecentOrderFirst() {
        Order recentOrder = order(2L, new BigDecimal("50.00"), OrderStatus.DELIVERED,
            LocalDateTime.of(2026, 8, 1, 10, 0), variantOf(category("Shoes")));
        Order olderOrder = order(1L, new BigDecimal("30.00"), OrderStatus.DELIVERED,
            LocalDateTime.of(2026, 7, 1, 10, 0), variantOf(category("Tops")));
        // Repository already returns most-recent-first per its derived query name.
        when(orderRepository.findByUserIdOrderByOrderDateDesc(42L)).thenReturn(List.of(recentOrder, olderOrder));

        Map<String, Object> result = service().getOrderHistory(42L);

        assertEquals("Shoes", result.get("topCategory"));
        assertEquals(List.of("Shoes", "Tops"), result.get("purchasedCategories"));
        assertEquals(new BigDecimal("80.00"), result.get("totalSpent"));
        assertEquals(2, result.get("orderCount"));
    }

    @Test
    void getOrderHistory_returnsEmptyDefaultsWhenUserHasNoOrders() {
        when(orderRepository.findByUserIdOrderByOrderDateDesc(99L)).thenReturn(List.of());

        Map<String, Object> result = service().getOrderHistory(99L);

        assertNull(result.get("topCategory"));
        assertEquals(List.of(), result.get("purchasedCategories"));
        assertEquals(BigDecimal.ZERO, result.get("totalSpent"));
        assertEquals(0, result.get("orderCount"));
        assertEquals(List.of(), result.get("recentOrders"));
    }

    @Test
    void getOrderHistory_ignoresNullTotalAmountsWhenSumming() {
        Order orderWithNullTotal = order(1L, null, OrderStatus.PENDING, LocalDateTime.now());
        Order orderWithTotal = order(2L, new BigDecimal("20.00"), OrderStatus.DELIVERED, LocalDateTime.now());
        when(orderRepository.findByUserIdOrderByOrderDateDesc(1L))
            .thenReturn(List.of(orderWithNullTotal, orderWithTotal));

        Map<String, Object> result = service().getOrderHistory(1L);

        assertEquals(new BigDecimal("20.00"), result.get("totalSpent"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrderHistory_mapsRecentOrdersAndCapsAtFive() {
        List<Order> sixOrders = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            sixOrders.add(order(i, new BigDecimal("10.00"), OrderStatus.PAID, LocalDateTime.of(2026, 1, i, 0, 0)));
        }
        when(orderRepository.findByUserIdOrderByOrderDateDesc(5L)).thenReturn(sixOrders);

        Map<String, Object> result = service().getOrderHistory(5L);

        List<Map<String, Object>> recentOrders = (List<Map<String, Object>>) result.get("recentOrders");
        assertEquals(5, recentOrders.size());
        assertEquals(1L, recentOrders.get(0).get("orderId"));
        assertEquals("PAID", recentOrders.get(0).get("status"));
    }

    // ── searchProducts ───────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void searchProducts_mapsFieldsAndFetchesThreeTimesTheLimitFromProductService() {
        ProductSearchResult tee = ProductSearchResult.builder()
            .id(1L).name("Tee").price(new BigDecimal("15.00")).imageUrl("tee.jpg")
            .categoryName("Tops").defaultVariantId(11L).build();
        when(productService.search(isNull(), eq("Tops"), isNull(), eq(false), eq(12)))
            .thenReturn(List.of(tee));

        Map<String, Object> result = service().searchProducts("Tops", null, null, 4, false);

        List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("products");
        assertEquals(1, products.size());
        assertEquals(1L, products.get(0).get("productId"));
        assertEquals("Tee", products.get(0).get("name"));
        assertEquals("Tops", products.get(0).get("category"));
        assertEquals(11L, products.get(0).get("defaultVariantId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchProducts_filtersOutResultsAboveMaxPrice() {
        ProductSearchResult cheap = ProductSearchResult.builder().id(1L).name("Cheap Tee").price(new BigDecimal("15.00")).build();
        ProductSearchResult pricey = ProductSearchResult.builder().id(2L).name("Pricey Tee").price(new BigDecimal("99.00")).build();
        when(productService.search(any(), any(), any(), anyBoolean(), anyInt())).thenReturn(List.of(cheap, pricey));

        Map<String, Object> result = service().searchProducts(null, new BigDecimal("50.00"), null, 4, false);

        List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("products");
        assertEquals(1, products.size());
        assertEquals("Cheap Tee", products.get(0).get("name"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchProducts_capsResultsAtRequestedLimit() {
        List<ProductSearchResult> five = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            five.add(ProductSearchResult.builder().id((long) i).name("Item " + i).price(BigDecimal.TEN).build());
        }
        when(productService.search(any(), any(), any(), anyBoolean(), anyInt())).thenReturn(five);

        Map<String, Object> result = service().searchProducts(null, null, null, 2, false);

        List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("products");
        assertEquals(2, products.size());
    }
}
