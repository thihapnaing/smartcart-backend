package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.AddToCartRequest;
import nus.iss.smartcart.backend.dto.CartItemsResponse;
import nus.iss.smartcart.backend.dto.UpdateCartItemRequest;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import nus.iss.smartcart.backend.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:4200")
public class CartController {

    private final CartService cartService;
    private final CurrentUserProvider currentUserProvider;

    public CartController(CartService cartService, CurrentUserProvider currentUserProvider) {
        this.cartService = cartService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemsResponse> addToCart(@RequestBody AddToCartRequest request) {
        Long userId = currentUserProvider.getCurrentCustomer().getId();
        CartItemsResponse response = cartService.addToCart(userId, request.getProductVariantId(), request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
    @GetMapping
    public ResponseEntity<CartItemsResponse> getCart() {
        Long userId = currentUserProvider.getCurrentCustomer().getId();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    // Author: Htet Nandar (Grace)
    /** Backs the quantity stepper's +/- buttons. Quantity <= 0 removes the row. */
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CartItemsResponse> updateQuantity(@PathVariable Long cartItemId, @RequestBody UpdateCartItemRequest request) {
        Long userId = currentUserProvider.getCurrentCustomer().getId();
        return ResponseEntity.ok(cartService.updateQuantity(userId, cartItemId, request.getQuantity()));
    }
}
