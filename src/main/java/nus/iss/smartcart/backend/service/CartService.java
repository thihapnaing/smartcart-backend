package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.CartItemDetail;
import nus.iss.smartcart.backend.dto.CartItemsResponse;
import nus.iss.smartcart.backend.model.Cart;
import nus.iss.smartcart.backend.model.CartItem;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.model.ProductVariant;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.repository.CartItemRepository;
import nus.iss.smartcart.backend.repository.CartRepository;
import nus.iss.smartcart.backend.repository.ProductVariantRepository;
import nus.iss.smartcart.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, ProductVariantRepository productVariantRepository, CartItemRepository cartItemRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartItemsResponse getCart(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if(cartOpt.isEmpty()) {
            return new CartItemsResponse(List.of(), BigDecimal.ZERO);
        }
        return buildCartResponse(cartOpt.get().getId());
    }

    @Transactional
    public CartItemsResponse addToCart(Long userId, Long productVariantId, Integer quantity) {
        int qty = (quantity != null) ? quantity : 1;

        Cart cart = getOrCreateCart(userId);
        ProductVariant productVariant = getProductVariant(productVariantId);
        validateActive(productVariant);

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), productVariantId);
        validateStock(existingItem, productVariant, qty);
        saveCartItem(cart, productVariant, existingItem, qty);

        return buildCartResponse(cart.getId());
    }

    // Author: Htet Nandar (Grace)
    /**
     * Sets a cart_item's quantity to an exact value - backs the stepper's +/- buttons.
     * A quantity of 0 or less removes the row instead of leaving a zero-quantity item.
     */
    @Transactional
    public CartItemsResponse updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found"));
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found"));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new EntityNotFoundException("Cart item not found");
        }

        if (quantity == null || quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            if (quantity > item.getProductVariant().getStock()) {
                throw new IllegalArgumentException("Requested quantity exceeds available stock");
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return buildCartResponse(cart.getId());
    }

    private CartItemsResponse buildCartResponse(Long cartId) {
        List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);
        List<CartItemDetail> cartItemDetails = cartItems.stream()
                .map(this::toCartItemDetail)
                .toList();
        BigDecimal cartTotal = cartItemDetails.stream()
                .map(CartItemDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartItemsResponse(cartItemDetails, cartTotal);
    }

    private void saveCartItem(Cart cart, ProductVariant productVariant, Optional<CartItem> existingItem, int qty) {
        if (existingItem.isEmpty()) {
            cartItemRepository.save(new CartItem(cart, productVariant, qty));
        } else {
            CartItem existing = existingItem.get();
            existing.setQuantity(existing.getQuantity() + qty);
            cartItemRepository.save(existing);
        }
    }

    // Author: Htet Nandar (Grace)
    /**
     * Blocks adding (or re-adding, e.g. via chat's "Buy again") a product an admin has since
     * deactivated. Without this, a stale order/cart reference could still be checked out even
     * though the merchant/admin pulled the listing.
     */
    private void validateActive(ProductVariant productVariant) {
        if (productVariant.getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalArgumentException("This product is no longer available");
        }
    }

    private void validateStock(Optional<CartItem> existingItem, ProductVariant productVariant, int qty) {
        int requestedTotalQty = existingItem.map(item -> item.getQuantity() + qty).orElse(qty);
        if(requestedTotalQty > productVariant.getStock()) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock");
        }
    }

    private ProductVariant getProductVariant(Long productVariantId) {
        return productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new EntityNotFoundException("Product variant not found"));
    }

    private Cart getOrCreateCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User Not Found"));
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    private CartItemDetail toCartItemDetail(CartItem item) {
        BigDecimal unitPrice = item.getProductVariant().getProduct().getPrice();
        BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        return CartItemDetail.builder()
                .cartItemId(item.getId())
                .productVariantId(item.getProductVariant().getId())
                .productName(item.getProductVariant().getProduct().getName())
                .imageUrl(item.getProductVariant().getProduct().getImageUrl())
                .size(item.getProductVariant().getSize())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .subtotal(subTotal)
                .gender(item.getProductVariant().getProduct().getGender().name())
                .categoryName(item.getProductVariant().getProduct().getCategory().getName())
                .shopName(item.getProductVariant().getProduct().getShopName())
                .build();
    }

}
