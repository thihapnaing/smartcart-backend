package nus.iss.smartcart.backend.service;

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.dto.*;
import nus.iss.smartcart.backend.model.*;
import nus.iss.smartcart.backend.repository.CategoryRepository;
import nus.iss.smartcart.backend.repository.ProductRepository;
import nus.iss.smartcart.backend.repository.UserProfileRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserProfileRepository userProfileRepository;

    @InjectMocks private ProductService productService;

    @Test
    void searchByKeyword_returnMappedResults() {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1L);

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("White Tee");
        when(product.getDescription()).thenReturn("White and soft");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(50));
        when(product.getImageUrl()).thenReturn("/assets/products/product1");
        when(product.getShopName()).thenReturn("SmartCart Shop");
        when(product.getCategory()).thenReturn(category);
        when(product.getGender()).thenReturn(Gender.MEN);
        when(product.getVariants()).thenReturn(List.of(productVariant));

        when(productRepository.searchByKeyword("shirt")).thenReturn(List.of(product));
        List<ProductSearchResult> results = productService.searchByKeyword("shirt");

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals("White Tee", results.get(0).getName());
        assertEquals("White and soft", results.get(0).getDescription());
        assertEquals(BigDecimal.valueOf(50), results.get(0).getPrice());
        assertEquals("/assets/products/product1", results.get(0).getImageUrl());
        assertEquals("SmartCart Shop", results.get(0).getShopName());
        assertEquals("Tops", results.get(0).getCategoryName());
        assertEquals("MEN", results.get(0).getGender());
        assertEquals(1L, results.get(0).getDefaultVariantId());
    }

    @Test
    void searchByKeyword_noVariants_defaultVariantIdIsNull() {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("White Tee");
        when(product.getDescription()).thenReturn("White and soft");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(50));
        when(product.getImageUrl()).thenReturn("/assets/products/product1");
        when(product.getShopName()).thenReturn("SmartCart Shop");
        when(product.getCategory()).thenReturn(category);
        when(product.getGender()).thenReturn(Gender.MEN);
        when(product.getVariants()).thenReturn(List.of());

        when(productRepository.searchByKeyword("shirt")).thenReturn(List.of(product));
        List<ProductSearchResult> results = productService.searchByKeyword("shirt");

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals("White Tee", results.get(0).getName());
        assertEquals("White and soft", results.get(0).getDescription());
        assertEquals(BigDecimal.valueOf(50), results.get(0).getPrice());
        assertEquals("/assets/products/product1", results.get(0).getImageUrl());
        assertEquals("SmartCart Shop", results.get(0).getShopName());
        assertEquals("Tops", results.get(0).getCategoryName());
        assertEquals("MEN", results.get(0).getGender());
        assertNull(results.get(0).getDefaultVariantId());
    }

    @Test
    void search_zeroLimit_stillReturnsAtLeastOne() {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1L);

        Product product1 = mock(Product.class);
        when(product1.getId()).thenReturn(1L);
        when(product1.getName()).thenReturn("White Tee");
        when(product1.getDescription()).thenReturn("White and soft");
        when(product1.getPrice()).thenReturn(BigDecimal.valueOf(50));
        when(product1.getImageUrl()).thenReturn("/assets/products/product1");
        when(product1.getShopName()).thenReturn("SmartCart Shop");
        when(product1.getCategory()).thenReturn(category);
        when(product1.getGender()).thenReturn(Gender.MEN);
        when(product1.getVariants()).thenReturn(List.of(productVariant));

        // product2 is never stubbed — since limit(1) runs before map(), it should
        // never be touched by toSearchResult, proving the limit cuts the stream early
        Product product2 = mock(Product.class);

        when(productRepository.search(null, null, null, false, ProductStatus.ACTIVE))
                .thenReturn(List.of(product1, product2));

        List<ProductSearchResult> results = productService.search(null, null, null, false, 0);

        assertEquals(1, results.size());
        assertEquals("White Tee", results.get(0).getName());
    }

    @Test
    void getProductDetail_notFound_throwsEntityNotFoundException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.getProductDetail(1L));
    }

    @Test
    void getProductDetail_found_returnsPopulatedResponse() {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        ProductVariant productVariant = mock(ProductVariant.class);
        when(productVariant.getId()).thenReturn(1L);
        when(productVariant.getSize()).thenReturn("S");
        when(productVariant.getStock()).thenReturn(5);

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("White Tee");
        when(product.getDescription()).thenReturn("White and soft");
        when(product.getPrice()).thenReturn(BigDecimal.valueOf(50));
        when(product.getImageUrl()).thenReturn("/assets/products/product1");
        when(product.getGender()).thenReturn(Gender.MEN);
        when(product.getCategory()).thenReturn(category);
        when(product.getShopName()).thenReturn("SmartCart Shop");
        when(product.getVariants()).thenReturn(List.of(productVariant));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDetailResponse response = productService.getProductDetail(1L);

        assertEquals(1L, response.getProductId());
        assertEquals("White Tee", response.getName());
        assertEquals("White and soft", response.getDescription());
        assertEquals(BigDecimal.valueOf(50), response.getPrice());
        assertEquals("/assets/products/product1", response.getImageUrl());
        assertEquals("MEN", response.getGender());
        assertEquals("Tops", response.getCategoryName());
        assertEquals("SmartCart Shop", response.getShopName());

        assertEquals(1, response.getVariants().size());
        ProductVariantDetail variantDetail = response.getVariants().get(0);
        assertEquals(1L, variantDetail.getProductVariantId());
        assertEquals("S", variantDetail.getSize());
        assertEquals(5, variantDetail.getStock());
    }

    @Test
    void createProduct_validRequest_returnsCreatedProduct() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(1L);
        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);

        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        UserProfile profile = mock(UserProfile.class);
        when(profile.getShopName()).thenReturn("SmartCart Official");
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        VariantRequest variantRequest = new VariantRequest();
        variantRequest.setSize("M");
        variantRequest.setStock(10);

        ProductCreateRequest request =
                ProductCreateRequest.builder()
                                .name("Classic Tee")
                                .description("Soft cotton tee")
                                .price(BigDecimal.valueOf(19.90))
                                .gender(Gender.MEN)
                                .categoryId(1L)
                                .status(ProductStatus.ACTIVE)
                                .variants(List.of(variantRequest))
                                .build();

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 100L);
            return p;
        });

        ProductDetailResponse response = productService.createProduct(request);

        assertEquals(100L, response.getProductId());
        assertEquals("Classic Tee", response.getName());
        assertEquals("SmartCart Official", response.getShopName());
        assertEquals("Tops", response.getCategoryName());
        assertEquals(1, response.getVariants().size());
        assertEquals("M", response.getVariants().get(0).getSize());
        assertEquals(10, response.getVariants().get(0).getStock());
    }

    @Test
    void createProduct_categoryNotFound_throwsIllegalArgumentException() {
        User merchant = mock(User.class);
        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);

        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        ProductCreateRequest request = ProductCreateRequest.builder()
                        .categoryId(99L)
                        .build();

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(request));
    }

    @Test
    void createProduct_noProfile_fallsBackToUsername() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(1L);
        when(merchant.getUsername()).thenReturn("smartcart_official");
        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);

        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        VariantRequest variantRequest = new VariantRequest();
        variantRequest.setSize("M");
        variantRequest.setStock(10);

        ProductCreateRequest request =
                ProductCreateRequest.builder()
                        .name("Classic Tee")
                        .description("Soft cotton tee")
                        .price(BigDecimal.valueOf(19.90))
                        .gender(Gender.MEN)
                        .categoryId(1L)
                        .status(ProductStatus.ACTIVE)
                        .variants(List.of(variantRequest))
                        .build();

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 100L);
            return p;
        });

        ProductDetailResponse response = productService.createProduct(request);

        assertEquals("smartcart_official", response.getShopName());
    }




}
