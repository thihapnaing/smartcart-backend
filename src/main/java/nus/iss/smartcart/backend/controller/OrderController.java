package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.*;
import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import nus.iss.smartcart.backend.service.OrderService;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest request) {
        Long userId = currentUserProvider.getCurrentCustomer().getId();
        CheckoutResponse response  = orderService.checkout(userId, request);
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
    @GetMapping("/assigned/{deliveryPersonId}")
    public ResponseEntity<List<Order>> getAssignedOrders(
            @PathVariable Long deliveryPersonId
    ) {
        List<Order> orders =
                orderService.getAssignedOrders(
                        deliveryPersonId
                );

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/in-progress/{deliveryPersonId}")
    public ResponseEntity<List<Order>> getInProgressOrders(
            @PathVariable Long deliveryPersonId
    ) {

        return ResponseEntity.ok(
                orderService.getInProgressOrders(
                        deliveryPersonId
                )
        );
    }

    @GetMapping("/completed/{deliveryPersonId}")
    public ResponseEntity<List<Order>> getCompletedOrders(
            @PathVariable Long deliveryPersonId
    ) {

        return ResponseEntity.ok(
                orderService.getCompletedOrders(
                        deliveryPersonId
                )
        );
    }

    @PatchMapping("/pickup")
    public ResponseEntity<Order> confirmPickup(
            @RequestBody OrderRequest request
    ) {
        Order updatedOrder = orderService.pickupParcel(
                request.getTrackingNo(),
                request.getDeliveryPersonId()
        );

        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/delivered")
    public ResponseEntity<Order> confirmDelivered(
            @RequestBody OrderRequest request
    ) {
        Order updatedOrder = orderService.deliveredParcel(
                request.getTrackingNo(),
                request.getDeliveryPersonId()
        );

        return ResponseEntity.ok(updatedOrder);
    }

    //    GET http://localhost:8080/api/orders/search/TRK-2026-0001/1
    @GetMapping(
            "/search/{trackingNo}/{deliveryPersonId}"
    )
    public ResponseEntity<Order> searchOrder(
            @PathVariable String trackingNo,
            @PathVariable Long deliveryPersonId
    ) {
        Order order =
                orderService.searchAssignedOrderByTrackingNo(
                        trackingNo,
                        deliveryPersonId
                );

        return ResponseEntity.ok(order);
    }

    @PostMapping("/{trackingNo}/proof/confirm")
    public ResponseEntity<Order> confirmDeliveryProof(
            @PathVariable("trackingNo") String trackingNo,
            @RequestBody ConfirmDeliveryRequest request
    ) {
        if (request.getFileKey() == null ||
                request.getFileKey().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Order updatedOrder =
                orderService.confirmDeliveryProof(
                        trackingNo,
                        request.getFileKey()
                );

        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping(
            "/assign/{trackingNo}/{deliveryPersonId}"
    )
    public ResponseEntity<Order> assignOrder(
            @PathVariable String trackingNo,
            @PathVariable Long deliveryPersonId
    ) {
        return ResponseEntity.ok(
                orderService.assignOrder(
                        trackingNo,
                        deliveryPersonId
                )
        );
    }

    @PatchMapping("/{orderId}/delivery-details")
    public ResponseEntity<Order> updateDeliveryDetails(
            @PathVariable Long orderId,
            @RequestBody UpdateDeliveryRequest request
    ) {
        Order updatedOrder =
                orderService.updateDeliveryDetails(
                        orderId,
                        request
                );

        return ResponseEntity.ok(updatedOrder);
    }

}
