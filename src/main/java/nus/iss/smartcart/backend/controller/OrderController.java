package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.*;
import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import nus.iss.smartcart.backend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    public OrderController(OrderService orderService, CurrentUserProvider currentUserProvider) {
        this.orderService = orderService;
        this.currentUserProvider = currentUserProvider;
    }

    //change to authenticated user ID, once JWT auth is implemented
    @PostMapping("/checkout")
    public ResponseEntity<List<CheckoutResponse>> checkout(@RequestBody CheckoutRequest request) {
        Long userId = currentUserProvider.getCurrentCustomer().getId();
        List<CheckoutResponse> response = orderService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CheckoutResponse> getOrderDetail(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentCustomer().getId();
        return ResponseEntity.ok(orderService.getOrderDetail(id, userId));
    }

    @GetMapping("/merchant")
    public ResponseEntity<List<MerchantOrderItemResponse>> getMerchantOrderItems() {
        List<MerchantOrderItemResponse> response = orderService.getMerchantOrderItems();
        return ResponseEntity.ok(response);
    }

    // for delivery app
// ------------------------------------------------
    // Android delivery application
    // ------------------------------------------------
    // ------------------------------------------------
// Merchant delivery management
// ------------------------------------------------

    @GetMapping(
            value = "/orders",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<DeliveryOrderDto>>
    getDeliveryOrders() {

        List<DeliveryOrderDto> orders =
                orderService.getDeliveryOrders();

        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/delivery-details")
    public ResponseEntity<DeliveryOrderDto>
    updateDeliveryDetails(
            @PathVariable Long orderId,
            @RequestBody UpdateDeliveryRequest request
    ) {
        DeliveryOrderDto updatedOrder =
                orderService.updateDeliveryDetails(
                        orderId,
                        request
                );

        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping("/assigned/{deliveryPersonId}")
    public ResponseEntity<List<OrderResponse>>
    getAssignedOrders(
            @PathVariable Long deliveryPersonId
    ) {
        List<OrderResponse> orders =
                orderService.getAssignedOrders(
                        deliveryPersonId
                );

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/in-progress/{deliveryPersonId}")
    public ResponseEntity<List<OrderResponse>>
    getInProgressOrders(
            @PathVariable Long deliveryPersonId
    ) {
        List<OrderResponse> orders =
                orderService.getInProgressOrders(
                        deliveryPersonId
                );

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/completed/{deliveryPersonId}")
    public ResponseEntity<List<OrderResponse>>
    getCompletedOrders(
            @PathVariable Long deliveryPersonId
    ) {
        List<OrderResponse> orders =
                orderService.getCompletedOrders(
                        deliveryPersonId
                );

        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/pickup")
    public ResponseEntity<OrderResponse> pickupOrder(
            @RequestBody OrderRequest request
    ) {
        OrderResponse updatedOrder =
                orderService.pickupParcel(
                        request.getTrackingNo(),
                        request.getDeliveryPersonId()
                );

        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/delivered")
    public ResponseEntity<OrderResponse> confirmDelivered(
            @RequestBody OrderRequest request
    ) {
        OrderResponse updatedOrder =
                orderService.deliveredParcel(
                        request.getTrackingNo(),
                        request.getDeliveryPersonId()
                );

        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping(
            "/search/{trackingNo}/{deliveryPersonId}"
    )
    public ResponseEntity<OrderResponse> searchOrder(
            @PathVariable String trackingNo,
            @PathVariable Long deliveryPersonId
    ) {
        OrderResponse order =
                orderService
                        .searchAssignedOrderByTrackingNo(
                                trackingNo,
                                deliveryPersonId
                        );

        return ResponseEntity.ok(order);
    }

    // Save the S3 file key and mark the order delivered.
    // Android expects Response<Unit>, so return HTTP 204.
    @PostMapping("/{trackingNo}/proof/confirm")
    public ResponseEntity<Void> confirmDeliveryProof(
            @PathVariable String trackingNo,
            @RequestBody ConfirmDeliveryRequest request
    ) {
        orderService.confirmDeliveryProof(
                trackingNo,
                request.getFileKey()
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @PutMapping(
            "/assign/{trackingNo}/{deliveryPersonId}"
    )
    public ResponseEntity<OrderResponse> assignOrder(
            @PathVariable String trackingNo,
            @PathVariable Long deliveryPersonId
    ) {
        OrderResponse updatedOrder =
                orderService.assignOrder(
                        trackingNo,
                        deliveryPersonId
                );

        return ResponseEntity.ok(updatedOrder);
    }

}
