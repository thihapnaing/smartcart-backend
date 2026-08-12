package nus.iss.smartcart.backend.service;

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.dto.ProductDetailResponse;
import nus.iss.smartcart.backend.dto.ProductRequest;
import nus.iss.smartcart.backend.dto.ProductSearchResult;
import nus.iss.smartcart.backend.dto.VariantRequest;
import nus.iss.smartcart.backend.exception.ForbiddenException;
import nus.iss.smartcart.backend.model.Category;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.model.ProductVariant;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserProfile;
import nus.iss.smartcart.backend.repository.CategoryRepository;
import nus.iss.smartcart.backend.repository.ProductRepository;
import nus.iss.smartcart.backend.repository.UserProfileRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                currentUserProvider,
                categoryRepository,
                userProfileRepository
        );
    }

    // ------------------------------------------------------------
    // searchByKeyword
    // ------------------------------------------------------------

    @Test
    void searchByKeyword_returnsMappedResults() {
        Product product = productWithoutVariants(1L, "White Tee", Gender.MEN);

        when(productRepository.searchByKeyword("shirt"))
                .thenReturn(List.of(product));

        List<ProductSearchResult> results =
                productService.searchByKeyword("shirt");

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

        verify(productRepository).searchByKeyword("shirt");
    }

    @Test
    void searchByKeyword_noResults_returnsEmptyList() {
        when(productRepository.searchByKeyword("unknown"))
                .thenReturn(List.of());

        List<ProductSearchResult> results =
                productService.searchByKeyword("unknown");

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(productRepository).searchByKeyword("unknown");
    }

    @Test
    void searchByKeyword_withVariant_mapsVariantData() {
        Product product = productWithoutVariants(2L, "Blue Shirt", Gender.WOMEN);

        ProductVariant variant = mock(ProductVariant.class);
        when(variant.getId()).thenReturn(101L);
        when(variant.getSize()).thenReturn("M");
        when(variant.getStock()).thenReturn(10);

        product.setVariants(new ArrayList<>(List.of(variant)));

        when(productRepository.searchByKeyword("shirt"))
                .thenReturn(List.of(product));

        ProductSearchResult result =
                productService.searchByKeyword("shirt").get(0);

        assertEquals(101L, result.getDefaultVariantId());
        assertEquals(1, result.getVariants().size());
        assertEquals(101L, result.getVariants().get(0).getId());
        assertEquals("M", result.getVariants().get(0).getSize());
        assertEquals(10, result.getVariants().get(0).getStock());
    }

    // ------------------------------------------------------------
    // search
    // ------------------------------------------------------------

    @Test
    void search_appliesLimit() {
        Product p1 = productWithoutVariants(1L, "Shirt 1", Gender.MEN);
        Product p2 = productWithoutVariants(2L, "Shirt 2", Gender.MEN);
        Product p3 = productWithoutVariants(3L, "Shirt 3", Gender.MEN);

        when(productRepository.search(
                "shirt",
                "Tops",
                Gender.MEN,
                false,
                ProductStatus.ACTIVE
        )).thenReturn(List.of(p1, p2, p3));

        List<ProductSearchResult> results =
                productService.search("shirt", "Tops", Gender.MEN, false, 2);

        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals(2L, results.get(1).getId());

        verify(productRepository).search(
                "shirt",
                "Tops",
                Gender.MEN,
                false,
                ProductStatus.ACTIVE
        );
    }

    @Test
    void search_zeroLimit_returnsAtLeastOneResult() {
        Product product = productWithoutVariants(1L, "White Tee", Gender.MEN);

        when(productRepository.search(
                "shirt",
                null,
                null,
                false,
                ProductStatus.ACTIVE
        )).thenReturn(List.of(product));

        List<ProductSearchResult> results =
                productService.search("shirt", null, null, false, 0);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
    }

    @Test
    void search_negativeLimit_returnsAtLeastOneResult() {
        Product product = productWithoutVariants(1L, "White Tee", Gender.MEN);

        when(productRepository.search(
                null,
                null,
                null,
                true,
                ProductStatus.ACTIVE
        )).thenReturn(List.of(product));

        List<ProductSearchResult> results =
                productService.search(null, null, null, true, -5);

        assertEquals(1, results.size());
    }

    @Test
    void search_newestFirstPassesRepositoryParameters() {
        when(productRepository.search(
                "shirt",
                "Tops",
                Gender.WOMEN,
                true,
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        List<ProductSearchResult> results =
                productService.search("shirt", "Tops", Gender.WOMEN, true, 10);

        assertTrue(results.isEmpty());

        verify(productRepository).search(
                "shirt",
                "Tops",
                Gender.WOMEN,
                true,
                ProductStatus.ACTIVE
        );
    }

    // ------------------------------------------------------------
    // getProductDetail
    // ------------------------------------------------------------

    @Test
    void getProductDetail_returnsMappedProduct() {
        Product product = productWithoutVariants(10L, "Red Shirt", Gender.MEN);

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        ProductDetailResponse response =
                productService.getProductDetail(10L);

        assertEquals(10L, response.getProductId());
        assertEquals("Red Shirt", response.getName());
        assertEquals("White and soft", response.getDescription());
        assertEquals(BigDecimal.valueOf(50), response.getPrice());
        assertEquals("/assets/products/product1", response.getImageUrl());
        assertEquals("MEN", response.getGender());
        assertEquals("Tops", response.getCategoryName());
        assertEquals("SmartCart Shop", response.getShopName());
        assertEquals("ACTIVE", response.getStatus());
        assertTrue(response.getVariants().isEmpty());
    }

    @Test
    void getProductDetail_notFound_throwsException() {
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> productService.getProductDetail(999L)
        );

        assertEquals("Product is not found", exception.getMessage());
    }

    // ------------------------------------------------------------
    // createProduct
    // ------------------------------------------------------------

    @Test
    void createProduct_createsProductAndReturnsDetail() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(20L);
        when(merchant.getUsername()).thenReturn("merchant1");

        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        ProductRequest request = mock(ProductRequest.class);
        when(request.getName()).thenReturn("Black Shirt");
        when(request.getDescription()).thenReturn("Black shirt");
        when(request.getPrice()).thenReturn(BigDecimal.valueOf(80));
        when(request.getGender()).thenReturn(Gender.MEN);
        when(request.getCategoryId()).thenReturn(3L);
        when(request.getImageUrl()).thenReturn("/images/black.jpg");
        when(request.getStatus()).thenReturn(ProductStatus.ACTIVE);

        when(request.getVariants()).thenReturn(List.of());

        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(userProfileRepository.findByUserId(20L)).thenReturn(Optional.empty());

        Product saved = productWithoutVariants(
                50L,
                "Black Shirt",
                Gender.MEN
        );
        saved.setDescription("Black shirt");
        saved.setPrice(BigDecimal.valueOf(80));
        saved.setImageUrl("/images/black.jpg");
        saved.setShopName("merchant1");
        saved.setStatus(ProductStatus.ACTIVE);
        saved.setCategory(category);
        saved.setMerchant(merchant);

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductDetailResponse response =
                productService.createProduct(request);

        assertEquals(50L, response.getProductId());
        assertEquals("Black Shirt", response.getName());
        assertEquals("MEN", response.getGender());
        assertEquals("Tops", response.getCategoryName());
        assertEquals("merchant1", response.getShopName());

        ArgumentCaptor<Product> captor =
                ArgumentCaptor.forClass(Product.class);

        verify(productRepository).save(captor.capture());

        Product created = captor.getValue();
        assertEquals("Black Shirt", created.getName());
        assertEquals("Black shirt", created.getDescription());
        assertEquals(BigDecimal.valueOf(80), created.getPrice());
        assertEquals(Gender.MEN, created.getGender());
        assertEquals(category, created.getCategory());
        assertEquals(merchant, created.getMerchant());
        assertEquals("merchant1", created.getShopName());
        assertEquals("/images/black.jpg", created.getImageUrl());
        assertEquals(ProductStatus.ACTIVE, created.getStatus());
        assertTrue(created.getVariants().isEmpty());
    }

    @Test
    void createProduct_usesShopNameFromProfile() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(20L);
        when(merchant.getUsername()).thenReturn("merchant1");

        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        UserProfile profile = mock(UserProfile.class);
        when(profile.getShopName()).thenReturn("My Fashion Shop");

        ProductRequest request = basicProductRequest(3L);

        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(userProfileRepository.findByUserId(20L))
                .thenReturn(Optional.of(profile));

        Product saved = productWithoutVariants(51L, "Blue Shirt", Gender.MEN);
        saved.setCategory(category);
        saved.setShopName("My Fashion Shop");

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductDetailResponse response =
                productService.createProduct(request);

        assertEquals("My Fashion Shop", response.getShopName());
    }

    @Test
    void createProduct_categoryNotFound_throwsException() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(20L);

        ProductRequest request = basicProductRequest(999L);

        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(request)
        );

        assertEquals("Category not found: 999", exception.getMessage());

        verify(productRepository, never()).save(any(Product.class));
    }

    // ------------------------------------------------------------
    // updateProduct
    // ------------------------------------------------------------

    @Test
    void updateProduct_updatesOwnedProduct() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(20L);

        User productMerchant = mock(User.class);
        when(productMerchant.getId()).thenReturn(20L);

        Category oldCategory = mock(Category.class);
        when(oldCategory.getName()).thenReturn("Old");

        Category newCategory = mock(Category.class);
        when(newCategory.getName()).thenReturn("New");

        Product product = productWithoutVariants(10L, "Old Shirt", Gender.MEN);
        product.setMerchant(productMerchant);
        product.setCategory(oldCategory);

        ProductRequest request = basicProductRequest(4L);
        when(request.getName()).thenReturn("New Shirt");
        when(request.getDescription()).thenReturn("New description");
        when(request.getPrice()).thenReturn(BigDecimal.valueOf(90));
        when(request.getGender()).thenReturn(Gender.WOMEN);
        when(request.getImageUrl()).thenReturn("/images/new.jpg");
        when(request.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(request.getVariants()).thenReturn(List.of());

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(newCategory));
        when(productRepository.save(product)).thenReturn(product);

        ProductDetailResponse response =
                productService.updateProduct(10L, request);

        assertEquals("New Shirt", product.getName());
        assertEquals("New description", product.getDescription());
        assertEquals(BigDecimal.valueOf(90), product.getPrice());
        assertEquals(Gender.WOMEN, product.getGender());
        assertEquals(newCategory, product.getCategory());
        assertEquals("/images/new.jpg", product.getImageUrl());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
        assertEquals("New Shirt", response.getName());
        assertEquals("WOMEN", response.getGender());
    }

    @Test
    void updateProduct_notFound_throwsException() {
        ProductRequest request = basicProductRequest(4L);

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> productService.updateProduct(999L, request)
        );

        assertEquals("Product not found: 999", exception.getMessage());
    }

    @Test
    void updateProduct_differentMerchant_throwsForbiddenException() {
        User currentMerchant = mock(User.class);
        when(currentMerchant.getId()).thenReturn(20L);

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);

        Product product = productWithoutVariants(10L, "Shirt", Gender.MEN);
        product.setMerchant(owner);

        ProductRequest request = basicProductRequest(4L);

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));
        when(currentUserProvider.getCurrentMerchant())
                .thenReturn(currentMerchant);

        assertThrows(
                ForbiddenException.class,
                () -> productService.updateProduct(10L, request)
        );

        verify(categoryRepository, never()).findById(anyLong());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_removesOldVariantsAndAddsNewVariants() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(20L);

        Product product = productWithoutVariants(10L, "Shirt", Gender.MEN);

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(20L);
        product.setMerchant(owner);

        ProductVariant small = new ProductVariant();
        small.setId(1L);
        small.setSize("S");
        small.setStock(5);
        small.setProduct(product);

        ProductVariant medium = new ProductVariant();
        medium.setId(2L);
        medium.setSize("M");
        medium.setStock(3);
        medium.setProduct(product);

        product.setVariants(new ArrayList<>(List.of(small, medium)));

        VariantRequest updatedSmall = mock(VariantRequest.class);
        when(updatedSmall.getSize()).thenReturn("S");
        when(updatedSmall.getStock()).thenReturn(10);

        VariantRequest newLarge = mock(VariantRequest.class);
        when(newLarge.getSize()).thenReturn("L");
        when(newLarge.getStock()).thenReturn(7);

        ProductRequest request = basicProductRequest(4L);
        when(request.getVariants())
                .thenReturn(List.of(updatedSmall, newLarge));

        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(category));
        when(productRepository.save(product)).thenReturn(product);

        productService.updateProduct(10L, request);

        assertEquals(2, product.getVariants().size());

        assertTrue(product.getVariants().stream()
                .anyMatch(v -> "S".equals(v.getSize()) && v.getStock() == 10));

        assertTrue(product.getVariants().stream()
                .anyMatch(v -> "L".equals(v.getSize()) && v.getStock() == 7));

        assertTrue(product.getVariants().stream()
                .noneMatch(v -> "M".equals(v.getSize())));
    }

    // ------------------------------------------------------------
    // deactivateProduct
    // ------------------------------------------------------------

    @Test
    void deactivateProduct_setsStatusInactive() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(20L);

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(20L);

        Product product = productWithoutVariants(10L, "Shirt", Gender.MEN);
        product.setMerchant(owner);

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));
        when(currentUserProvider.getCurrentMerchant())
                .thenReturn(merchant);
        when(productRepository.save(product))
                .thenReturn(product);

        ProductDetailResponse response =
                productService.deactivateProduct(10L);

        assertEquals(ProductStatus.INACTIVE, product.getStatus());
        assertEquals("INACTIVE", response.getStatus());

        verify(productRepository).save(product);
    }

    @Test
    void deactivateProduct_differentMerchant_throwsForbiddenException() {
        User currentMerchant = mock(User.class);
        when(currentMerchant.getId()).thenReturn(20L);

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);

        Product product = productWithoutVariants(10L, "Shirt", Gender.MEN);
        product.setMerchant(owner);

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));
        when(currentUserProvider.getCurrentMerchant())
                .thenReturn(currentMerchant);

        assertThrows(
                ForbiddenException.class,
                () -> productService.deactivateProduct(10L)
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    // ------------------------------------------------------------
    // getMerchantProducts
    // ------------------------------------------------------------

    @Test
    void getMerchantProducts_returnsMerchantProducts() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(20L);

        Product product = productWithoutVariants(1L, "Merchant Shirt", Gender.MEN);

        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);
        when(productRepository.findByMerchantId(20L))
                .thenReturn(List.of(product));

        List<ProductSearchResult> results =
                productService.getMerchantProducts();

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals("Merchant Shirt", results.get(0).getName());
        assertEquals("MEN", results.get(0).getGender());

        verify(productRepository).findByMerchantId(20L);
    }

    @Test
    void getMerchantProducts_noProducts_returnsEmptyList() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(20L);

        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);
        when(productRepository.findByMerchantId(20L))
                .thenReturn(List.of());

        List<ProductSearchResult> results =
                productService.getMerchantProducts();

        assertTrue(results.isEmpty());
    }

    // ------------------------------------------------------------
    // toSearchResult
    // ------------------------------------------------------------

    @Test
    void toSearchResult_nullCategoryAndGender_returnsNullFields() {
        Product product = mock(Product.class);

        when(product.getId()).thenReturn(1L);
        when(product.getName()).thenReturn("Shirt");
        when(product.getDescription()).thenReturn("Description");
        when(product.getPrice()).thenReturn(BigDecimal.TEN);
        when(product.getImageUrl()).thenReturn("/image.jpg");
        when(product.getShopName()).thenReturn("Shop");
        when(product.getCategory()).thenReturn(null);
        when(product.getGender()).thenReturn(null);
        when(product.getColor()).thenReturn("RED");
        when(product.getVariants()).thenReturn(List.of());

        ProductSearchResult result =
                productService.toSearchResult(product);

        assertEquals(1L, result.getId());
        assertEquals("Shirt", result.getName());
        assertNull(result.getCategoryName());
        assertNull(result.getGender());
        assertEquals("RED", result.getColor());
        assertNull(result.getDefaultVariantId());
        assertTrue(result.getVariants().isEmpty());
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private Product productWithoutVariants(
            Long id,
            String name,
            Gender gender
    ) {
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription("White and soft");
        product.setPrice(BigDecimal.valueOf(50));
        product.setImageUrl("/assets/products/product1");
        product.setShopName("SmartCart Shop");
        product.setCategory(category);
        product.setGender(gender);
        product.setStatus(ProductStatus.ACTIVE);
        product.setVariants(new ArrayList<>());

        return product;
    }

    private ProductRequest basicProductRequest(Long categoryId) {
        ProductRequest request = mock(ProductRequest.class);

        when(request.getName()).thenReturn("Blue Shirt");
        when(request.getDescription()).thenReturn("Blue shirt");
        when(request.getPrice()).thenReturn(BigDecimal.valueOf(70));
        when(request.getGender()).thenReturn(Gender.MEN);
        when(request.getCategoryId()).thenReturn(categoryId);
        when(request.getImageUrl()).thenReturn("/images/blue.jpg");
        when(request.getStatus()).thenReturn(ProductStatus.ACTIVE);
        when(request.getVariants()).thenReturn(List.of());

        return request;
    }

    @Test
    void activateProduct_productNotFound_throwsEntityNotFoundException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> productService.activateProduct(1L));
    }

    @Test
    void activateProduct_wrongMerchant_throwsForbiddenException() {
        Product product = mock(Product.class);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        User merchant2 = mock(User.class);
        when(product.getMerchant()).thenReturn(merchant2);
        when(merchant2.getId()).thenReturn(2L);

        User merchant = mock(User.class);
        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);
        when(merchant.getId()).thenReturn(1L);

        assertThrows(ForbiddenException.class, () -> productService.activateProduct(1L));
    }

    @Test
    void activateProduct_returnsOkWithProductData() {
        User merchant = mock(User.class);
        when(merchant.getId()).thenReturn(1L);

        Category category = mock(Category.class);
        when(category.getName()).thenReturn("Tops");

        Product product = new Product();
        product.setMerchant(merchant);
        product.setName("White Tee");
        product.setDescription("soft and made of cotton");
        product.setPrice(BigDecimal.valueOf(1));
        product.setCategory(category);
        product.setGender(Gender.MEN);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(currentUserProvider.getCurrentMerchant()).thenReturn(merchant);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductDetailResponse response = productService.activateProduct(1L);

        assertEquals(ProductStatus.ACTIVE.name(), response.getStatus());
    }
}
