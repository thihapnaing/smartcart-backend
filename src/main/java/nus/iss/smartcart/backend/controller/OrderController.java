package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.CheckoutRequest;
import nus.iss.smartcart.backend.dto.CheckoutResponse;
import nus.iss.smartcart.backend.dto.MerchantOrderItemResponse;
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
}
