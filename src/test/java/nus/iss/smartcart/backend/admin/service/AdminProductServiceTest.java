package nus.iss.smartcart.backend.admin.service;

// AUTHOR: Htet Nandar(Grace)

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.admin.dto.AdminProductSummaryDto;
import nus.iss.smartcart.backend.model.Category;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.repository.CartItemRepository;
import nus.iss.smartcart.backend.repository.ProductRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers the admin guard (getCurrentAdmin() called before either operation) and, most
 * importantly, the cart-clearing side effect: deactivating a product must strip it out of
 * every cart it's currently sitting in, while reactivating must leave carts untouched.
 */
@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private CartItemRepository cartItemRepository;

    @InjectMocks private AdminProductService adminProductService;

    @Test
    void getAllProducts_returnsEveryListingRegardlessOfStatus_mappedToSummaryDto() {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        Product active = mock(Product.class);
        when(active.getId()).thenReturn(1L);
        when(active.getName()).thenReturn("Classic Crew Tee");
        when(active.getPrice()).thenReturn(BigDecimal.valueOf(19.9));
        when(active.getImageUrl()).thenReturn("/assets/products/tee");
        when(active.getCategory()).thenReturn(category);
        when(active.getShopName()).thenReturn("SmartCart Official");
        when(active.getStatus()).thenReturn(ProductStatus.ACTIVE);

        Product inactive = mock(Product.class);
        when(inactive.getId()).thenReturn(2L);
        when(inactive.getName()).thenReturn("Canvas Sneakers");
        when(inactive.getPrice()).thenReturn(BigDecimal.valueOf(59.9));
        when(inactive.getImageUrl()).thenReturn("/assets/products/sneakers");
        when(inactive.getCategory()).thenReturn(category);
        when(inactive.getShopName()).thenReturn("SmartCart Official");
        when(inactive.getStatus()).thenReturn(ProductStatus.INACTIVE);

        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(active, inactive));

        List<AdminProductSummaryDto> results = adminProductService.getAllProducts();

        verify(currentUserProvider).getCurrentAdmin();
        assertEquals(2, results.size());
        assertEquals("ACTIVE", results.get(0).getStatus());
        assertEquals("INACTIVE", results.get(1).getStatus());
        assertEquals("Classic Crew Tee", results.get(0).getName());
        assertEquals("Tops", results.get(0).getCategoryName());
    }

    @Test
    void getAllProducts_nullStatus_mapsToNullNotException() {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("Classic Crew Tee");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(19.9));
        when(product.getImageUrl()).thenReturn("/assets/products/tee");
        when(product.getCategory()).thenReturn(category);
        when(product.getShopName()).thenReturn("SmartCart Official");
        when(product.getStatus()).thenReturn(null);

        when(productRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(product));

        List<AdminProductSummaryDto> results = adminProductService.getAllProducts();

        assertNull(results.get(0).getStatus());
    }

    @Test
    void updateProductStatus_productNotFound_throwsEntityNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> adminProductService.updateProductStatus(99L, ProductStatus.INACTIVE));

        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void updateProductStatus_toInactive_deactivatesAndClearsCartItems() {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        User admin = mock(User.class);
        when(admin.getUsername()).thenReturn("grace_admin");
        when(currentUserProvider.getCurrentAdmin()).thenReturn(admin);

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("Classic Crew Tee");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(19.9));
        when(product.getImageUrl()).thenReturn("/assets/products/tee");
        when(product.getCategory()).thenReturn(category);
        when(product.getShopName()).thenReturn("SmartCart Official");
        when(product.getStatus()).thenReturn(ProductStatus.INACTIVE);
        when(product.getLastModifiedByAdmin()).thenReturn(admin);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        AdminProductSummaryDto result = adminProductService.updateProductStatus(1L, ProductStatus.INACTIVE);

        verify(currentUserProvider).getCurrentAdmin();
        verify(product).setStatus(ProductStatus.INACTIVE);
        verify(product).setAdminLocked(true);
        verify(cartItemRepository).deleteByProductVariant_Product_Id(1L);
        assertEquals("INACTIVE", result.getStatus());

        // Who deactivated this listing (and when) must be recorded, not just that *an* admin
        // was authenticated - that's the whole point of this field.
        verify(product).setLastModifiedByAdmin(admin);
        ArgumentCaptor<LocalDateTime> timestampCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(product).setLastModifiedAt(timestampCaptor.capture());
        assertNotNull(timestampCaptor.getValue());
        assertEquals("grace_admin", result.getLastModifiedByAdminUsername());
    }

    @Test
    void updateProductStatus_toActive_doesNotTouchCartItems() {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        User admin = mock(User.class);
        when(currentUserProvider.getCurrentAdmin()).thenReturn(admin);

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("Classic Crew Tee");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(19.9));
        when(product.getImageUrl()).thenReturn("/assets/products/tee");
        when(product.getCategory()).thenReturn(category);
        when(product.getShopName()).thenReturn("SmartCart Official");
        when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        AdminProductSummaryDto result = adminProductService.updateProductStatus(1L, ProductStatus.ACTIVE);

        verify(product).setStatus(ProductStatus.ACTIVE);
        verify(product).setAdminLocked(false);
        verify(cartItemRepository, never()).deleteByProductVariant_Product_Id(anyLong());
        assertEquals("ACTIVE", result.getStatus());

        // Reactivating is a status change too - still recorded, same as deactivating.
        verify(product).setLastModifiedByAdmin(admin);
        verify(product).setLastModifiedAt(any(LocalDateTime.class));
    }
}
