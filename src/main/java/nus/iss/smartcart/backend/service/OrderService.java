package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.*;
import nus.iss.smartcart.backend.model.*;
import nus.iss.smartcart.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentUserProvider currentUserProvider;

    public OrderService(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductVariantRepository productVariantRepository, UserRepository userRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository, PaymentRepository paymentRepository, CurrentUserProvider currentUserProvider) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public List<CheckoutResponse> checkout(Long userId, CheckoutRequest checkoutRequest) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found"));
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }
        validateStock(cartItems);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Map<User, List<CartItem>> itemsByMerchant = cartItems.stream()
                .collect(Collectors.groupingBy(ci -> ci.getProductVariant().getProduct().getMerchant()));

        List<CheckoutResponse> responses = new ArrayList<>();
        for (List<CartItem> merchantItems : itemsByMerchant.values()) {
            BigDecimal totalAmount = calculateTotal(merchantItems);
            Order order = createOrder(user, checkoutRequest, totalAmount);
            addOrderItems(merchantItems, order);
            createPayment(order, checkoutRequest.getPaymentMethod());
            responses.add(buildCheckOutResponse(order, checkoutRequest));
        }

        clearCart(cart);
        return responses;
    }

    @Transactional(readOnly = true)
    public CheckoutResponse getOrderDetail(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order is not found"));
        if(!order.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Order not found");
        }
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for this order"));
        DeliveryDetails deliveryDetails = DeliveryDetails.builder()
                .firstName(order.getFirstName())
                .lastName(order.getLastName())
                .shippingAddress(order.getShippingAddress())
                .phoneNumber(order.getPhoneNumber())
                .build();
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        List<CartItemDetail> cartItemDetailList = orderItems.stream()
                .map(this::toCartItemDetail)
                .toList();
        return CheckoutResponse.builder()
                .orderId(order.getId())
                .cartItemDetails(cartItemDetailList)
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getStatus())
                .deliveryDetails(deliveryDetails)
                .paymentMethod(payment.getPaymentMethod())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MerchantOrderItemResponse> getMerchantOrderItems() {
        User merchant = currentUserProvider.getCurrentMerchant();
        List<OrderItem> orderItemList = orderItemRepository.findByProductVariantProductMerchantId(merchant.getId());
        return orderItemList.stream()
                .map(this::toMerchantOrderItemResponse)
                .toList();
    }

    private CheckoutResponse buildCheckOutResponse(Order order, CheckoutRequest checkoutRequest) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        List<CartItemDetail> cartItemDetails = orderItems
                .stream()
                .map(this::toCartItemDetail)
                .toList();
        DeliveryDetails deliveryDetails =
                DeliveryDetails.builder()
                        .firstName(checkoutRequest.getFirstName())
                        .lastName(checkoutRequest.getLastName())
                        .shippingAddress(checkoutRequest.getShippingAddress())
                        .phoneNumber(checkoutRequest.getPhoneNumber())
                        .build();
        return CheckoutResponse.builder()
                .orderId(order.getId())
                .cartItemDetails(cartItemDetails)
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getStatus())
                .deliveryDetails(deliveryDetails)
                .paymentMethod(checkoutRequest.getPaymentMethod())
                .build();
    }

    private CartItemDetail toCartItemDetail(OrderItem orderItem) {
        return CartItemDetail.builder()
                .cartItemId(orderItem.getId())
                .productVariantId(orderItem.getProductVariant().getId()) // Author: Htet Nandar (Grace)
                .productName(orderItem.getProductVariant().getProduct().getName())
                .imageUrl(orderItem.getProductVariant().getProduct().getImageUrl())
                .size(orderItem.getProductVariant().getSize())
                .unitPrice(orderItem.getUnitPrice())
                .quantity(orderItem.getQuantity())
                .subtotal(orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                .gender(orderItem.getProductVariant().getProduct().getGender().name())
                .categoryName(orderItem.getProductVariant().getProduct().getCategory().getName())
                .shopName(orderItem.getProductVariant().getProduct().getShopName())
                .build();
    }

    private MerchantOrderItemResponse toMerchantOrderItemResponse(OrderItem orderItem) {
        return MerchantOrderItemResponse.builder()
                .orderId(orderItem.getOrder().getId())
                .productName(orderItem.getProductVariant().getProduct().getName())
                .size(orderItem.getProductVariant().getSize())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .subtotal(orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                .orderStatus(orderItem.getOrder().getStatus().name())
                .orderDate(orderItem.getOrder().getOrderDate())
                .deliveredAt(orderItem.getOrder().getDeliveredAt())
                .buyerFirstName(orderItem.getOrder().getFirstName())
                .buyerLastName(orderItem.getOrder().getLastName())
                .build();
    }

    private void createPayment(Order order, PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod);
        paymentRepository.save(payment);
    }

    private void clearCart(Cart cart) {
        cart.getItems().clear();
    }

    private void addOrderItems(List<CartItem> cartItems, Order order) {
        for(CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getProductVariant();
            Integer currentStock = variant.getStock();
            Integer purchasedQuantity = cartItem.getQuantity();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(purchasedQuantity);
            orderItem.setUnitPrice(variant.getProduct().getPrice());
            orderItemRepository.save(orderItem);

            orderItem.getProductVariant().setStock(currentStock - purchasedQuantity);
            productVariantRepository.save(variant);
        }
    }

    private BigDecimal calculateTotal(List<CartItem> cartItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for(CartItem cartItem : cartItems) {
            Integer quantity = cartItem.getQuantity();
            BigDecimal unitPrice = cartItem.getProductVariant().getProduct().getPrice();
            BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(subTotal);
        }
        return totalAmount;
    }

    private Order createOrder(User user, CheckoutRequest checkoutRequest, BigDecimal totalAmount) {
        Order order = new Order();
        order.setUser(user);
        order.setFirstName(checkoutRequest.getFirstName());
        order.setLastName(checkoutRequest.getLastName());
        order.setPhoneNumber(checkoutRequest.getPhoneNumber());
        order.setShippingAddress(checkoutRequest.getShippingAddress());
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PAID);

        return orderRepository.save(order);
    }

    private void validateStock(List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            Integer availableStock = cartItem.getProductVariant().getStock();
            Integer quantityRequested = cartItem.getQuantity();
            if (quantityRequested > availableStock) {
                String productName = cartItem.getProductVariant().getProduct().getName();
                String size = cartItem.getProductVariant().getSize();
                throw new IllegalStateException(
                        "Not enough stock for " + productName + " (size " + size + "). Available: " + availableStock);
            }
        }
    }
}
