package nus.iss.smartcart.backend.service;

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.dto.CartItemDetail;
import nus.iss.smartcart.backend.dto.CartItemsResponse;
import nus.iss.smartcart.backend.model.*;
import nus.iss.smartcart.backend.repository.CartItemRepository;
import nus.iss.smartcart.backend.repository.CartRepository;
import nus.iss.smartcart.backend.repository.ProductVariantRepository;
import nus.iss.smartcart.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CartService cartService;

    @Mock Cart cart;

    @BeforeEach
    void setUp() {
        lenient().when(cart.getId()).thenReturn(1L);
    }

    @Test
    void getCart_noCartExists_returnsEmptyResponse() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        CartItemsResponse response = cartService.getCart(1L);
        assertTrue(response.getCartItemDetails().isEmpty());
        assertEquals(BigDecimal.ZERO, response.getCartTotal());
    }

    @Test
    void getCart_cartExistsButEmpty_returnsZeroTotal() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        CartItemsResponse response = cartService.getCart(1L);
        assertEquals(BigDecimal.ZERO, response.getCartTotal());
        assertEquals(List.of(), response.getCartItemDetails());
    }

    @Test
    void getCart_cartExistsWithItems_returnsPopulatedResponse() {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        Product product = mock(Product.class);
        when (product.getPrice()).thenReturn(BigDecimal.valueOf(50));
        when (product.getGender()).thenReturn(Gender.MEN);
        when(product.getName()).thenReturn("Classic Crew Tee");
        when(product.getImageUrl()).thenReturn("/assets/products/tee-crew.jpg");
        when(product.getCategory()).thenReturn(category);

        ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1L);
        when(productVariant.getProduct()).thenReturn(product);
        when(productVariant.getSize()).thenReturn("S");

        CartItem cartItem = mock(CartItem.class);
        when(cartItem.getId()).thenReturn(1L);
        when(cartItem.getProductVariant()).thenReturn(productVariant);
        when(cartItem.getQuantity()).thenReturn(2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));

        CartItemsResponse response = cartService.getCart(1L);
        CartItemDetail cartItemDetail = response.getCartItemDetails().get(0);
        assertEquals(1, response.getCartItemDetails().size());
        assertEquals(1L, cartItemDetail.getCartItemId());
        assertEquals(1L, cartItemDetail.getProductVariantId());
        assertEquals("Classic Crew Tee", cartItemDetail.getProductName());
        assertEquals("/assets/products/tee-crew.jpg", cartItemDetail.getImageUrl());
        assertEquals("S", cartItemDetail.getSize());
        assertEquals(BigDecimal.valueOf(50), cartItemDetail.getUnitPrice());
        assertEquals(BigDecimal.valueOf(100), cartItemDetail.getSubtotal());
        assertEquals("MEN", cartItemDetail.getGender());
        assertEquals(BigDecimal.valueOf(100), response.getCartTotal());
    }

    @Test
    void addToCart_userNotFound_throwsEntityNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> cartService.addToCart(1L, 100L, 100));
        verifyNoInteractions(productVariantRepository, cartItemRepository);
    }

    @Test
    void addToCart_productVariantNotFound_throwsEntityNotFoundException() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productVariantRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> cartService.addToCart(1L,100L,100));
        verifyNoInteractions(cartItemRepository);
    }

    // Author: Htet Nandar (Grace)
    private Product activeProduct() {
        Product product = mock(Product.class);
        lenient().when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
        return product;
    }

    @Test
    void addToCart_deactivatedProduct_throwsIllegalArgumentException() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        Product product = mock(Product.class);
        when(product.getStatus()).thenReturn(ProductStatus.INACTIVE);

        ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getProduct()).thenReturn(product);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(productVariant));

        assertThrows(IllegalArgumentException.class, () -> cartService.addToCart(1L, 1L, 1));
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void addToCart_newItem_addsSuccessfully() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        ProductVariant productVariant = mock(ProductVariant.class);
        // activeProduct() is fetched into a local first, not inlined as thenReturn()'s argument
        // - it runs its own when()/thenReturn() cycle internally, and interleaving that inside
        // this when(...).thenReturn(...) call leaves Mockito's stubbing state mid-flight
        // (UnfinishedStubbing on the outer call).
        Product product = activeProduct();
        when(productVariant.getProduct()).thenReturn(product);
        when(productVariant.getStock()).thenReturn(100);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(productVariant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), 1L))
                .thenReturn(Optional.empty());
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        cartService.addToCart(1L,1L, 3);

        verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 3));
    }

    @Test
    void addToCart_existingItem_incrementsQuantity() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        ProductVariant productVariant = mock(ProductVariant.class);
        Product product = activeProduct();
        when(productVariant.getProduct()).thenReturn(product);
        when(productVariant.getStock()).thenReturn(100);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(productVariant));

        CartItem existing = mock(CartItem.class);
        when(existing.getQuantity()).thenReturn(3);

        when(cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), 1L))
                .thenReturn(Optional.of(existing));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of());

        cartService.addToCart(1L, 1L, 2);
        verify(existing).setQuantity(5);
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addToCart_existingItem_combinedQuantityExceedsStock_throwsException() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        ProductVariant productVariant = mock(ProductVariant.class);
        Product product = activeProduct();
        when(productVariant.getProduct()).thenReturn(product);
        when(productVariant.getStock()).thenReturn(2);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(productVariant));

        CartItem existing = mock(CartItem.class);
        when(existing.getQuantity()).thenReturn(1);
        when(cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), 1L))
                .thenReturn(Optional.of(existing));
        assertThrows(IllegalArgumentException.class, () -> cartService.addToCart(1L,1L,2));
        verify(existing, never()).setQuantity(anyInt());
    }

    @Test
    void addToCart_noExistingCart_createsNewCart() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        ProductVariant productVariant = mock(ProductVariant.class);
        Product product = activeProduct();
        when(productVariant.getProduct()).thenReturn(product);
        when(productVariant.getStock()).thenReturn(10);
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(productVariant));

        when(cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of());

        cartService.addToCart(1L, 1L, 1);

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addToCart_newItem_exceedsStock_throwsIllegalArgumentException() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        ProductVariant productVariant = mock(ProductVariant.class);
        Product product = activeProduct();
        when(productVariant.getProduct()).thenReturn(product);
        when(productVariant.getStock()).thenReturn(2);

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(productVariant));
        when(cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), 1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> cartService.addToCart(1L, 1L, 100));
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateQuantity_cartNotFound_throwsEntityNotFoundException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> cartService.updateQuantity(1L,1L,2));
    }

    @Test
    void updateQuantity_cartItemNotFound_throwsEntityNotFoundException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> cartService.updateQuantity(1L, 1L, 2));
    }

    @Test
    void updateQuantity_itemBelongsToDifferentCart_throwsEntityNotFoundException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        Cart otherCart = mock(Cart.class);
        when(otherCart.getId()).thenReturn(2L);

        CartItem item = mock(CartItem.class);
        when(item.getCart()).thenReturn(otherCart);

        when(cartItemRepository.findById(2L)).thenReturn(Optional.of(item));
        assertThrows(EntityNotFoundException.class, () -> cartService.updateQuantity(1L, 2L, 2));
    }

    @Test
    void updateQuantity_nullQuantity_deletesItem() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartItem item = mock(CartItem.class);
        when(item.getCart()).thenReturn(cart);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        cartService.updateQuantity(1L, 1L, null);

        verify(cartItemRepository).delete(item);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateQuantity_zeroOrNegativeQuantity_deletesItem() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        CartItem item = mock(CartItem.class);
        when(item.getCart()).thenReturn(cart);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        cartService.updateQuantity(1L, 1L, 0);

        verify(cartItemRepository).delete(item);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateQuantity_exceedsStock_throwsIllegalArgumentException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getStock()).thenReturn(2);

        CartItem item = mock(CartItem.class);
        when(item.getCart()).thenReturn(cart);
        when(item.getProductVariant()).thenReturn(productVariant);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> cartService.updateQuantity(1L, 1L, 3));
        verify(cartItemRepository, never()).save(item);
        verify(cartItemRepository, never()).delete(item);
    }

    @Test
    void updateQuantity_validQuantity_updatesAndSaves() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getStock()).thenReturn(2);

        CartItem item = mock(CartItem.class);
        when(item.getCart()).thenReturn(cart);
        when(item.getProductVariant()).thenReturn(productVariant);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        cartService.updateQuantity(1L, 1L, 2);

        verify(item).setQuantity(2);
        verify(cartItemRepository).save(item);
    }
}
