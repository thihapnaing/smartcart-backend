package nus.iss.smartcart.backend.tools.service;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.dto.CartItemDetail;
import nus.iss.smartcart.backend.dto.CartItemsResponse;
import nus.iss.smartcart.backend.dto.ProductSearchResult;
import nus.iss.smartcart.backend.model.Category;
import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.model.OrderItem;
import nus.iss.smartcart.backend.model.OrderStatus;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductVariant;
import nus.iss.smartcart.backend.repository.OrderRepository;
import nus.iss.smartcart.backend.service.CartService;
import nus.iss.smartcart.backend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
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

    @Mock
    private CartService cartService;

    private ToolDataService service() {
        return new ToolDataService(orderRepository, productService, cartService);
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

    private ProductVariant variantOf(long variantId, String productName, String imageUrl) {
        Product product = new Product();
        product.setName(productName);
        product.setImageUrl(imageUrl);
        // category is NOT NULL on Product in production; getOrderHistory() dereferences it
        // unconditionally, so the fixture needs one too even though this test doesn't assert on it.
        product.setCategory(category("Tops"));
        ProductVariant variant = new ProductVariant();
        ReflectionTestUtils.setField(variant, "id", variantId);
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
            LocalDateTime.of(2026, Month.AUGUST, 1, 10, 0), variantOf(category("Shoes")));
        Order olderOrder = order(1L, new BigDecimal("30.00"), OrderStatus.DELIVERED,
            LocalDateTime.of(2026, Month.JULY, 1, 10, 0), variantOf(category("Tops")));
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
            sixOrders.add(order(i, new BigDecimal("10.00"), OrderStatus.PAID, LocalDateTime.of(2026, Month.JANUARY, i, 0, 0)));
        }
        when(orderRepository.findByUserIdOrderByOrderDateDesc(5L)).thenReturn(sixOrders);

        Map<String, Object> result = service().getOrderHistory(5L);

        List<Map<String, Object>> recentOrders = (List<Map<String, Object>>) result.get("recentOrders");
        assertEquals(5, recentOrders.size());
        assertEquals(1L, recentOrders.get(0).get("orderId"));
        assertEquals("PAID", recentOrders.get(0).get("status"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrderHistory_usesRealTrackingNoAsOrderNumberWhenPresent() {
        Order order = order(1L, new BigDecimal("10.00"), OrderStatus.DELIVERED, LocalDateTime.now());
        order.setTrackingNo("SC-TRK-000001");
        when(orderRepository.findByUserIdOrderByOrderDateDesc(1L)).thenReturn(List.of(order));

        Map<String, Object> result = service().getOrderHistory(1L);

        List<Map<String, Object>> recentOrders = (List<Map<String, Object>>) result.get("recentOrders");
        assertEquals("SC-TRK-000001", recentOrders.get(0).get("orderNumber"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrderHistory_fallsBackToZeroPaddedOrderNumber_whenNoTrackingNo() {
        Order order = order(7L, new BigDecimal("10.00"), OrderStatus.DELIVERED, LocalDateTime.now());
        when(orderRepository.findByUserIdOrderByOrderDateDesc(1L)).thenReturn(List.of(order));

        Map<String, Object> result = service().getOrderHistory(1L);

        List<Map<String, Object>> recentOrders = (List<Map<String, Object>>) result.get("recentOrders");
        assertEquals("SC-000007", recentOrders.get(0).get("orderNumber"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrderHistory_mapsItemsWithNameImageAndVariantId() {
        ProductVariant variant = variantOf(11L, "Classic Crew Tee", "/assets/products/tee-crew.jpg");
        Order order = order(1L, new BigDecimal("19.90"), OrderStatus.DELIVERED, LocalDateTime.now(), variant);
        order.getItems().get(0).setUnitPrice(new BigDecimal("19.90"));
        order.getItems().get(0).setQuantity(2);
        when(orderRepository.findByUserIdOrderByOrderDateDesc(1L)).thenReturn(List.of(order));

        Map<String, Object> result = service().getOrderHistory(1L);

        List<Map<String, Object>> recentOrders = (List<Map<String, Object>>) result.get("recentOrders");
        List<Map<String, Object>> items = (List<Map<String, Object>>) recentOrders.get(0).get("items");
        assertEquals(1, items.size());
        assertEquals("Classic Crew Tee", items.get(0).get("name"));
        assertEquals("/assets/products/tee-crew.jpg", items.get(0).get("imageUrl"));
        assertEquals(new BigDecimal("19.90"), items.get(0).get("price"));
        assertEquals(2, items.get(0).get("quantity"));
        assertEquals(11L, items.get(0).get("productVariantId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrderHistory_mapsItemsToEmptyListWhenOrderHasNoItems() {
        Order order = order(1L, new BigDecimal("10.00"), OrderStatus.DELIVERED, LocalDateTime.now());
        when(orderRepository.findByUserIdOrderByOrderDateDesc(1L)).thenReturn(List.of(order));

        Map<String, Object> result = service().getOrderHistory(1L);

        List<Map<String, Object>> recentOrders = (List<Map<String, Object>>) result.get("recentOrders");
        assertEquals(List.of(), recentOrders.get(0).get("items"));
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

    // ── Remaining partial-branch coverage (SonarCloud condition coverage) ──

    @Test
    void getOrderHistory_skipsDuplicateAndNullCategoryNames() {
        // Covers both the "categoryName != null" false branch and the
        // "!purchasedCategories.contains(categoryName)" false branch - prior fixtures only ever
        // hit the true/true path.
        Order order = order(1L, new BigDecimal("10.00"), OrderStatus.DELIVERED, LocalDateTime.now(),
            variantOf(category("Shoes")), variantOf(category("Shoes")), variantOf(category(null)));
        when(orderRepository.findByUserIdOrderByOrderDateDesc(7L)).thenReturn(List.of(order));

        Map<String, Object> result = service().getOrderHistory(7L);

        assertEquals(List.of("Shoes"), result.get("purchasedCategories"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrderHistory_mapsNullStatusAsNullInRecentOrders() {
        // Covers the false branch of o.getStatus() != null - prior fixtures always set a status.
        Order orderWithNullStatus = order(3L, new BigDecimal("5.00"), null, LocalDateTime.now());
        when(orderRepository.findByUserIdOrderByOrderDateDesc(8L)).thenReturn(List.of(orderWithNullStatus));

        Map<String, Object> result = service().getOrderHistory(8L);

        List<Map<String, Object>> recentOrders = (List<Map<String, Object>>) result.get("recentOrders");
        assertNull(recentOrders.get(0).get("status"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrderHistory_mapsNullOrderDateAsNullInRecentOrders() {
        // Covers the false branch of o.getOrderDate() != null - prior fixtures always set an
        // orderDate (needed for the recency sort), so that branch was never exercised.
        Order orderWithNullDate = order(4L, new BigDecimal("5.00"), OrderStatus.PENDING, null);
        when(orderRepository.findByUserIdOrderByOrderDateDesc(9L)).thenReturn(List.of(orderWithNullDate));

        Map<String, Object> result = service().getOrderHistory(9L);

        List<Map<String, Object>> recentOrders = (List<Map<String, Object>>) result.get("recentOrders");
        assertNull(recentOrders.get(0).get("orderDate"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchProducts_excludesProductsWithNullPriceWhenMaxPriceIsSet() {
        // Covers the false branch of p.getPrice() != null inside the maxPrice filter - prior
        // fixtures with a maxPrice always had a non-null price on every candidate.
        ProductSearchResult noPrice = ProductSearchResult.builder().id(1L).name("No Price").price(null).build();
        ProductSearchResult withPrice = ProductSearchResult.builder().id(2L).name("Has Price").price(new BigDecimal("10.00")).build();
        when(productService.search(any(), any(), any(), anyBoolean(), anyInt())).thenReturn(List.of(noPrice, withPrice));

        Map<String, Object> result = service().searchProducts(null, new BigDecimal("50.00"), null, 4, false);

        List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("products");
        assertEquals(1, products.size());
        assertEquals("Has Price", products.get(0).get("name"));
    }

    // ── getCart ──────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getCart_mapsItemsAndTotalsFromCartService() {
        CartItemDetail item = CartItemDetail.builder()
            .cartItemId(1L)
            .productName("Tee")
            .size("M")
            .quantity(2)
            .unitPrice(new BigDecimal("15.00"))
            .subtotal(new BigDecimal("30.00"))
            .build();
        CartItemsResponse cart = new CartItemsResponse(List.of(item), new BigDecimal("30.00"));
        when(cartService.getCart(42L)).thenReturn(cart);

        Map<String, Object> result = service().getCart(42L);

        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertEquals("Tee", items.get(0).get("productName"));
        assertEquals("M", items.get(0).get("size"));
        assertEquals(2, items.get(0).get("quantity"));
        assertEquals(new BigDecimal("15.00"), items.get(0).get("unitPrice"));
        assertEquals(new BigDecimal("30.00"), items.get(0).get("subtotal"));
        assertEquals(new BigDecimal("30.00"), result.get("cartTotal"));
        assertEquals(1, result.get("itemCount"));
    }

    @Test
    void getCart_returnsEmptyItemsWhenCartIsEmpty() {
        CartItemsResponse cart = new CartItemsResponse(List.of(), BigDecimal.ZERO);
        when(cartService.getCart(99L)).thenReturn(cart);

        Map<String, Object> result = service().getCart(99L);

        assertEquals(List.of(), result.get("items"));
        assertEquals(BigDecimal.ZERO, result.get("cartTotal"));
        assertEquals(0, result.get("itemCount"));
    }
}
