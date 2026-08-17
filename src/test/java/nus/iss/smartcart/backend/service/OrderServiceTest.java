package nus.iss.smartcart.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import nus.iss.smartcart.backend.dto.CartItemDetail;
import nus.iss.smartcart.backend.dto.CheckoutRequest;
import nus.iss.smartcart.backend.dto.CheckoutResponse;
import nus.iss.smartcart.backend.dto.MerchantOrderItemResponse;
import nus.iss.smartcart.backend.model.*;
import nus.iss.smartcart.backend.repository.*;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private Cart cart;

    @InjectMocks private OrderService orderService;

    @BeforeEach
    void setUp() {
        lenient().when(cart.getId()).thenReturn(1L);
    }
    @Test
    void checkout_cartNotFound_throwsEntityNotFoundException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        CheckoutRequest request = new CheckoutRequest();
        assertThrows(EntityNotFoundException.class, () -> orderService.checkout(1L, request));
        verifyNoInteractions(cartItemRepository, userRepository, orderRepository, paymentRepository, orderItemRepository);
    }

    @Test
    void checkout_emptyCart_throwsIllegalStateException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of());
        CheckoutRequest request = new CheckoutRequest();
        assertThrows(IllegalStateException.class, () -> orderService.checkout(1L, request));
        verifyNoInteractions(userRepository, orderRepository, paymentRepository, orderItemRepository);
    }

    @Test
    void checkout_insufficientStock_throwsIllegalStateException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        CartItem cartItem = mock(CartItem.class);
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
        ProductVariant productVariant = mock(ProductVariant.class);
        when(cartItem.getProductVariant()).thenReturn(productVariant);
        when(productVariant.getStock()).thenReturn(2);
        when(cartItem.getQuantity()).thenReturn(3);
        Product product = mock(Product.class);
        when(productVariant.getProduct()).thenReturn(product);
        when(product.getName()).thenReturn("White Tee");
        when(productVariant.getSize()).thenReturn("S");
        CheckoutRequest request = new CheckoutRequest();
        assertThrows(IllegalStateException.class, () -> orderService.checkout(1L, request));
        verifyNoInteractions(userRepository);
    }

    @Test
    void checkout_userNotFound_throwsEntityNotFoundException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        CartItem cartItem = mock(CartItem.class);
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
        ProductVariant productVariant = mock(ProductVariant.class);
        when(cartItem.getProductVariant()).thenReturn(productVariant);
        when(productVariant.getStock()).thenReturn(3);
        when(cartItem.getQuantity()).thenReturn(1);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        CheckoutRequest request = new CheckoutRequest();
        assertThrows(EntityNotFoundException.class, () -> orderService.checkout(1L, request));
    }

    @Test
    void checkout_successfulCheckout_returnsPopulatedResponse() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        CartItem cartItem = mock(CartItem.class);
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItem));
        ProductVariant productVariant = mock(ProductVariant.class);
        when(cartItem.getProductVariant()).thenReturn(productVariant);
        when(productVariant.getStock()).thenReturn(3);
        when(cartItem.getQuantity()).thenReturn(1);
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Product product = mock(Product.class);
        when(productVariant.getProduct()).thenReturn(product);
        when(productVariant.getId()).thenReturn(1L);
        when(productVariant.getSize()).thenReturn("S");
        when(product.getName()).thenReturn("White Tee");
        when(product.getImageUrl()).thenReturn("/assets/products/photo1");
        when(product.getGender()).thenReturn(Gender.MEN);
        User merchant = mock(User.class);
        when(product.getMerchant()).thenReturn(merchant);
        Category category = mock(Category.class);
        when(product.getCategory()).thenReturn(category);
        when(category.getName()).thenReturn("Tops");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(1));
        Order order = mock(Order.class);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        CheckoutRequest checkoutRequest = mock(CheckoutRequest.class);
        when(checkoutRequest.getFirstName()).thenReturn("John");
        when(checkoutRequest.getLastName()).thenReturn("Smith");
        when(checkoutRequest.getPhoneNumber()).thenReturn("91234567");
        when(checkoutRequest.getShippingAddress()).thenReturn("123 Rainbow Street");
        OrderItem orderItem = mock(OrderItem.class);
        when(orderItem.getProductVariant()).thenReturn(productVariant);
        when(cart.getItems()).thenReturn(new ArrayList<>());
        when(orderItem.getId()).thenReturn(1L);
        when(orderItem.getUnitPrice()).thenReturn(BigDecimal.valueOf(1));
        when(orderItem.getQuantity()).thenReturn(1);
        when(order.getId()).thenReturn(1L);
        when(orderItemRepository.findByOrderId(order.getId())).thenReturn(List.of(orderItem));
        when(order.getTotalAmount()).thenReturn(BigDecimal.valueOf(1));
        when(order.getStatus()).thenReturn(OrderStatus.PAID);
        when(checkoutRequest.getPaymentMethod()).thenReturn(PaymentMethod.CREDIT_CARD);

        List<CheckoutResponse> responses = orderService.checkout(1L, checkoutRequest);

        assertEquals(1, responses.size());
        CheckoutResponse response = responses.get(0);

        assertEquals(1L, response.getOrderId());
        assertEquals(BigDecimal.valueOf(1), response.getTotalAmount());
        assertEquals(OrderStatus.PAID, response.getOrderStatus());
        assertEquals(PaymentMethod.CREDIT_CARD, response.getPaymentMethod());

        assertEquals("John", response.getDeliveryDetails().getFirstName());
        assertEquals("Smith", response.getDeliveryDetails().getLastName());
        assertEquals("123 Rainbow Street", response.getDeliveryDetails().getShippingAddress());
        assertEquals("91234567", response.getDeliveryDetails().getPhoneNumber());

        assertEquals(1, response.getCartItemDetails().size());
        CartItemDetail detail = response.getCartItemDetails().get(0);
        assertEquals(1L, detail.getCartItemId());
        assertEquals(1L, detail.getProductVariantId());
        assertEquals("White Tee", detail.getProductName());
        assertEquals("/assets/products/photo1", detail.getImageUrl());
        assertEquals("S", detail.getSize());
        assertEquals(BigDecimal.valueOf(1), detail.getUnitPrice());
        assertEquals(BigDecimal.valueOf(1), detail.getSubtotal());
        assertEquals("MEN", detail.getGender());
        assertEquals("Tops", detail.getCategoryName());

        verify(orderItemRepository).save(any(OrderItem.class));
        verify(productVariantRepository).save(productVariant);
        verify(paymentRepository).save(any(Payment.class));

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void checkout_multipleMerchants_createsSeparateOrderPerMerchant() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        // Merchant A's item
        CartItem cartItemA = mock(CartItem.class);
        ProductVariant variantA = mock(ProductVariant.class);
        Product productA = mock(Product.class);
        User merchantA = mock(User.class);

        when(cartItemA.getProductVariant()).thenReturn(variantA);
        when(cartItemA.getQuantity()).thenReturn(1);
        when(variantA.getStock()).thenReturn(5);
        when(variantA.getProduct()).thenReturn(productA);
        when(productA.getMerchant()).thenReturn(merchantA);
        when(productA.getPrice()).thenReturn(BigDecimal.valueOf(10));

        // Merchant B's item
        CartItem cartItemB = mock(CartItem.class);
        ProductVariant variantB = mock(ProductVariant.class);
        Product productB = mock(Product.class);
        User merchantB = mock(User.class);

        when(cartItemB.getProductVariant()).thenReturn(variantB);
        when(cartItemB.getQuantity()).thenReturn(1);
        when(variantB.getStock()).thenReturn(5);
        when(variantB.getProduct()).thenReturn(productB);
        when(productB.getMerchant()).thenReturn(merchantB);
        when(productB.getPrice()).thenReturn(BigDecimal.valueOf(20));

        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(cartItemA, cartItemB));

        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CheckoutRequest checkoutRequest = mock(CheckoutRequest.class);
        when(checkoutRequest.getPaymentMethod()).thenReturn(PaymentMethod.CREDIT_CARD);

        when(orderRepository.save(any(Order.class))).thenAnswer(new Answer<Order>() {
            private long nextId = 1L;
            @Override
            public Order answer(InvocationOnMock invocation) {
                Order order = mock(Order.class);
                when(order.getId()).thenReturn(nextId++);
                when(order.getStatus()).thenReturn(OrderStatus.PAID);
                when(order.getTotalAmount()).thenReturn(BigDecimal.valueOf(10));
                return order;
            }
        });

        when(cart.getItems()).thenReturn(new ArrayList<>());
        when(orderItemRepository.findByOrderId(anyLong())).thenReturn(List.of());

        List<CheckoutResponse> responses = orderService.checkout(1L, checkoutRequest);

        assertEquals(2, responses.size());
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void getOrderDetail_orderNotFound_throwsEntityNotFoundException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.getOrderDetail(1L, 1L));
    }

    @Test
    void getOrderDetail_orderBelongsToDifferentUser_throwsEntityNotFoundException() {
        Order order = mock(Order.class);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        User user = mock(User.class);
        when(order.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(2L);
        assertThrows(EntityNotFoundException.class, () -> orderService.getOrderDetail(1L, 1L));
    }

    @Test
    void getOrderDetail_paymentNotFound_throwsEntityNotFoundException() {
        Order order = mock(Order.class);
        User user = mock(User.class);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(order.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.getOrderDetail(1L, 1L));
    }

    @Test
    void getOrderDetail_success_returnsPopulatedResponse() {
        Order order = mock(Order.class);
        User user = mock(User.class);
        when(order.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);

        when(order.getId()).thenReturn(1L);
        when(order.getFirstName()).thenReturn("John");
        when(order.getLastName()).thenReturn("Smith");
        when(order.getShippingAddress()).thenReturn("123 Rainbow Street");
        when(order.getPhoneNumber()).thenReturn("91234567");
        when(order.getTotalAmount()).thenReturn(BigDecimal.valueOf(50));
        when(order.getStatus()).thenReturn(OrderStatus.PAID);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Payment payment = mock(Payment.class);
        when(payment.getPaymentMethod()).thenReturn(PaymentMethod.CREDIT_CARD);
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        Product product = mock(Product.class);
        when(product.getName()).thenReturn("White Tee");
        when(product.getImageUrl()).thenReturn("/assets/products/photo1");
        when(product.getGender()).thenReturn(Gender.MEN);
        when(product.getCategory()).thenReturn(category);

        ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1L);
        when(productVariant.getSize()).thenReturn("S");
        when(productVariant.getProduct()).thenReturn(product);

        OrderItem orderItem = mock(OrderItem.class);
        when(orderItem.getId()).thenReturn(1L);
        when(orderItem.getProductVariant()).thenReturn(productVariant);
        when(orderItem.getUnitPrice()).thenReturn(BigDecimal.valueOf(50));
        when(orderItem.getQuantity()).thenReturn(1);

        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(orderItem));

        CheckoutResponse response = orderService.getOrderDetail(1L, 1L);
        assertEquals(1L, response.getOrderId());
        assertEquals(BigDecimal.valueOf(50), response.getTotalAmount());
        assertEquals(OrderStatus.PAID, response.getOrderStatus());
        assertEquals(PaymentMethod.CREDIT_CARD, response.getPaymentMethod());

        assertEquals("John", response.getDeliveryDetails().getFirstName());
        assertEquals("Smith", response.getDeliveryDetails().getLastName());
        assertEquals("123 Rainbow Street", response.getDeliveryDetails().getShippingAddress());
        assertEquals("91234567", response.getDeliveryDetails().getPhoneNumber());

        assertEquals(1, response.getCartItemDetails().size());
        CartItemDetail detail = response.getCartItemDetails().get(0);
        assertEquals(1L, detail.getCartItemId());
        assertEquals(1L, detail.getProductVariantId());
        assertEquals("White Tee", detail.getProductName());
        assertEquals("/assets/products/photo1", detail.getImageUrl());
        assertEquals("S", detail.getSize());
        assertEquals(BigDecimal.valueOf(50), detail.getUnitPrice());
        assertEquals(BigDecimal.valueOf(50), detail.getSubtotal());
        assertEquals("MEN", detail.getGender());
        assertEquals("Tops", detail.getCategoryName());
    }

    @Test
    void getMerchantOrderItems_returnsPopulatedResponse() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(1L);
        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);

        Order order = mock(Order.class);
        when(order.getId()).thenReturn(1L);
        when(order.getStatus()).thenReturn(OrderStatus.PAID);
        when(order.getFirstName()).thenReturn("John");
        when(order.getLastName()).thenReturn("Tan");

        Product product = mock(Product.class);
        when(product.getName()).thenReturn("White Tee");

        ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getProduct()).thenReturn(product);
        when(productVariant.getSize()).thenReturn("S");

        OrderItem orderItem = mock(OrderItem.class);
        when(orderItem.getOrder()).thenReturn(order);
        when(orderItem.getProductVariant()).thenReturn(productVariant);
        when(orderItem.getQuantity()).thenReturn(10);
        when(orderItem.getUnitPrice()).thenReturn(BigDecimal.valueOf(1));

        when(orderItemRepository.findByProductVariantProductMerchantId(1L)).thenReturn(List.of(orderItem));

        List<MerchantOrderItemResponse> responseList = orderService.getMerchantOrderItems();
        assertEquals(1, responseList.size());
        MerchantOrderItemResponse response = responseList.get(0);
        assertEquals(1L, response.getOrderId());
        assertEquals("John", response.getBuyerFirstName());
    }
}
