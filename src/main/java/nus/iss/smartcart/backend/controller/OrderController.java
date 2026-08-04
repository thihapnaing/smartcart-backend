package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.CheckoutRequest;
import nus.iss.smartcart.backend.dto.CheckoutResponse;
import nus.iss.smartcart.backend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    //change to authenticated user ID, once JWT auth is implemented
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest request) {
        Long userId = 2L;
        CheckoutResponse response  = orderService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CheckoutResponse> getOrderDetail(@PathVariable Long id) {
        Long userId = 2L; //replace with authenticated user once auth is implemented
        return ResponseEntity.ok(orderService.getOrderDetail(id, userId));
    }
}
