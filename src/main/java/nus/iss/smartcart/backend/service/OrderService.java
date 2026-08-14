package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.*;
import nus.iss.smartcart.backend.model.*;
import nus.iss.smartcart.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private final PushNotificationService pushNotificationService;

    public OrderService(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductVariantRepository productVariantRepository, UserRepository userRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository, PaymentRepository paymentRepository, CurrentUserProvider currentUserProvider,  PushNotificationService pushNotificationService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.currentUserProvider = currentUserProvider;
        this.pushNotificationService = pushNotificationService;
    }

    @Transactional
    public CheckoutResponse checkout(Long userId, CheckoutRequest checkoutRequest) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found"));
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }
        validateStock(cartItems);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        BigDecimal totalAmount = calculateTotal(cartItems);
        Order order  = createOrder(user, checkoutRequest, totalAmount);
        addOrderItems(cartItems, order);
        clearCart(cart);
        createPayment(order, checkoutRequest.getPaymentMethod());
        return buildCheckOutResponse(order, checkoutRequest);
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

    // for delivery app
    public List<Order> getAssignedOrders(Long deliveryPersonId) {
        return orderRepository.findByDeliveryPersonId(deliveryPersonId);
    }

    public List<Order> getInProgressOrders(
            Long deliveryPersonId
    ) {
        return orderRepository
                .findByDeliveryPersonIdAndStatusInOrderByIdDesc(
                        deliveryPersonId,
                        List.of(
                                OrderStatus.PACKED,
                                OrderStatus.PICKED_UP
                        )
                );
    }

    public List<Order> getCompletedOrders(
            Long deliveryPersonId
    ) {
        return orderRepository
                .findByDeliveryPersonIdAndStatusOrderByDeliveredAtDesc(
                        deliveryPersonId,
                        OrderStatus.DELIVERED
                );
    }

    @Transactional
    public Order pickupParcel(
            String trackingNo,
            Long deliveryPersonId
    ) {
        Order order = orderRepository
                .findByTrackingNoAndDeliveryPersonId(
                        trackingNo,
                        deliveryPersonId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order not found or not assigned " +
                                        "to this delivery person"
                        )
                );

        if (order.getStatus() != OrderStatus.PACKED) {
            throw new IllegalStateException(
                    "Only PACKED orders can be picked up"
            );
        }

        order.setStatus(OrderStatus.PICKED_UP);

        return orderRepository.save(order);
    }

    @Transactional
    public Order deliveredParcel(
            String trackingNo,
            Long deliveryPersonId
    ) {
        Order order = orderRepository
                .findByTrackingNoAndDeliveryPersonId(
                        trackingNo,
                        deliveryPersonId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Order not found or not assigned " +
                                        "to this delivery person"
                        )
                );

        if (order.getStatus() != OrderStatus.PICKED_UP) {
            throw new IllegalStateException(
                    "Only PACKED orders can be picked up"
            );
        }

        order.setStatus(OrderStatus.DELIVERED);

        return orderRepository.save(order);
    }

    public Order searchAssignedOrderByTrackingNo(
            String trackingNo,
            Long deliveryPersonId
    ) {
        return orderRepository
                .findByTrackingNoAndDeliveryPersonId(
                        trackingNo,
                        deliveryPersonId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found or not assigned?: " + trackingNo
                        )
                );
    }

    @Transactional
    public Order confirmDeliveryProof(
            String trackingNo,
            String fileKey
    ) {
        Order order = orderRepository
                .findByTrackingNo(trackingNo)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        )
                );

        order.setDeliveryProofKey(fileKey);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Transactional
    public Order assignOrder(
            String trackingNo,
            Long deliveryPersonId
    ) {
        Order order = orderRepository
                .findByTrackingNo(trackingNo)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        )
                );

        order.setDeliveryPersonId(deliveryPersonId);

        Order savedOrder =
                orderRepository.save(order);

//        pushNotificationService.notifyJobAssigned(
//                savedOrder
//        );

        return savedOrder;
    }

    @Transactional
    public Order updateDeliveryDetails(
            Long orderId,
            UpdateDeliveryRequest request
    ) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        )
                );

        Long previousDeliveryPersonId =
                order.getDeliveryPersonId();

        if (request.getTrackingNo() != null &&
                !request.getTrackingNo().isBlank()) {

            String trackingNo =
                    request.getTrackingNo().trim();

            orderRepository
                    .findByTrackingNo(trackingNo)
                    .filter(existingOrder ->
                            !existingOrder.getId()
                                    .equals(orderId)
                    )
                    .ifPresent(existingOrder -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Tracking number already exists"
                        );
                    });

            order.setTrackingNo(trackingNo);
        }

        if (request.getDeliveryPersonId() != null) {
            if (order.getTrackingNo() == null ||
                    order.getTrackingNo().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tracking number is required before assigning a driver"
                );
            }

            if (order.getStatus() != OrderStatus.PACKED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only PACKED orders can be assigned to a driver"
                );
            }
            order.setDeliveryPersonId(
                    request.getDeliveryPersonId()
            );
        }

        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }

        Order savedOrder =
                orderRepository.save(order);

        if (request.getDeliveryPersonId() != null) {
            System.out.println(
                    "Sending notification to driver " +
                            savedOrder.getDeliveryPersonId()
            );

            pushNotificationService.notifyJobAssigned(
                    savedOrder
            );
        }
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrderDto> getDeliveryOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::toDeliveryOrderDto)
                .toList();
    }

    private DeliveryOrderDto toDeliveryOrderDto(Order order) {
        return new DeliveryOrderDto(
                order.getId(),
                order.getFirstName(),
                order.getLastName(),
                order.getStatus() != null
                        ? order.getStatus().name()
                        : null,
                order.getTrackingNo(),
                order.getDeliveryPersonId(),

                // Order currently stores only the delivery-person ID
                null,

                order.getDeliveredAt()
        );
    }
}
