package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.*;
import nus.iss.smartcart.backend.exception.ForbiddenException;
import nus.iss.smartcart.backend.model.*;
import nus.iss.smartcart.backend.repository.CategoryRepository;
import nus.iss.smartcart.backend.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.repository.UserProfileRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CurrentUserProvider currentUserProvider;
    private final CategoryRepository categoryRepository;
    private final UserProfileRepository userProfileRepository;

    public ProductService(ProductRepository productRepository, CurrentUserProvider currentUserProvider, CategoryRepository categoryRepository, UserProfileRepository userProfileRepository) {
        this.productRepository = productRepository;
        this.currentUserProvider = currentUserProvider;
        this.categoryRepository = categoryRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductSearchResult> searchByKeyword(String keyword) {
        List<Product> products = productRepository.searchByKeyword(keyword);

        return products.stream()
                .map(this::toSearchResult)
                .toList();
    }

    // Author: Htet Nandar (Grace)
    /**
     * General-purpose search backing both the "/search" REST endpoint (keyword only, for backward
     * compatibility) and the AI chat's tool calls, which can also filter by category/gender and sort
     * by recency for "new arrivals" style questions. Any parameter left null is not filtered on.
     */
    @Transactional(readOnly = true)
    public List<ProductSearchResult> search(String keyword, String categoryName, Gender gender, boolean newestFirst, int limit) {
        List<Product> products = productRepository.search(keyword, categoryName, gender, newestFirst, ProductStatus.ACTIVE);
        return products.stream()
                .limit(Math.max(1, limit))
                .map(this::toSearchResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product is not found"));
        return createProductDetailResponse(product);

    }

    @Transactional
    public ProductDetailResponse createProduct(ProductRequest request) {
        User merchant = currentUserProvider.getCurrentMerchant();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found: " + request.getCategoryId()
                ));

        String shopName = userProfileRepository.findByUserId(merchant.getId())
                .map(UserProfile::getShopName)
                .orElse(merchant.getUsername());

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setGender(request.getGender());
        product.setCategory(category);
        product.setMerchant(merchant);
        product.setShopName(shopName);
        product.setStatus(request.getStatus());

        List<ProductVariant> variants = request.getVariants().stream()
                .map(v -> {
                    ProductVariant variant = new ProductVariant();
                    variant.setSize(v.getSize());
                    variant.setStock(v.getStock());
                    variant.setProduct(product);
                    return variant;
                })
                .toList();
        product.setVariants(variants);

        Product saved = productRepository.save(product);
        return createProductDetailResponse(saved);
    }

    @Transactional
    public ProductDetailResponse updateProduct(Long productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        assertOwnership(product);

        mergeVariants(product, request.getVariants());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));

        applyScalarUpdates(product, request, category);

        Product saved = productRepository.save(product);
        return createProductDetailResponse(saved);
    }

    @Transactional
    public ProductDetailResponse deactivateProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        assertOwnership(product);
        product.setStatus(ProductStatus.INACTIVE);
        return createProductDetailResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductSearchResult> getMerchantProducts() {
        User merchant = currentUserProvider.getCurrentMerchant();
        List<Product> products = productRepository.findByMerchantId(merchant.getId());
        return products.stream()
                .map(this::toSearchResult)
                .toList();
    }
    private void applyScalarUpdates(Product product, ProductRequest request, Category category) {
        product.setCategory(category);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setGender(request.getGender());
        product.setStatus(request.getStatus());
    }

    private void mergeVariants(Product product, List<VariantRequest> variants) {
        Map<String, ProductVariant> existingBySize = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getSize, variant -> variant));

        for(VariantRequest updatedVariant : variants) {
            if(existingBySize.containsKey(updatedVariant.getSize())) {
                ProductVariant existing = existingBySize.get(updatedVariant.getSize());
                existing.setStock(updatedVariant.getStock());
                existingBySize.remove(updatedVariant.getSize());
            } else {
                ProductVariant newVariant = new ProductVariant();
                newVariant.setSize(updatedVariant.getSize());
                newVariant.setStock(updatedVariant.getStock());
                newVariant.setProduct(product);
                product.getVariants().add(newVariant);
            }
        }

        product.getVariants().removeAll(existingBySize.values());
    }

    private void assertOwnership(Product product) {
        User merchant = currentUserProvider.getCurrentMerchant();

        if(!product.getMerchant().getId().equals(merchant.getId())) {
            throw new ForbiddenException("You do not have permission to update this product");
        }
    }

    private ProductDetailResponse createProductDetailResponse(Product product) {
        List<ProductVariantDetail> variantDetailList = product.getVariants().stream()
                .map(this::toProductVariantDetail)
                .toList();
        return ProductDetailResponse.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .gender(product.getGender().name())
                .categoryName(product.getCategory().getName())
                .shopName(product.getShopName())
                .status(product.getStatus().name())
                .variants(variantDetailList)
                .build();
    }

    private ProductVariantDetail toProductVariantDetail(ProductVariant productVariant) {
        return ProductVariantDetail.builder()
                .productVariantId(productVariant.getId())
                .size(productVariant.getSize())
                .stock(productVariant.getStock())
                .build();
    }

    private ProductSearchResult toSearchResult(Product product) {
        Long defaultVariantId = product.getVariants().isEmpty() ? null : product.getVariants().get(0).getId(); // Author: Htet Nandar (Grace)
        return ProductSearchResult.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .imageUrl(product.getImageUrl())
                    .shopName(product.getShopName())
                    .categoryName(product.getCategory().getName())
                    .gender(product.getGender().name())
                    .defaultVariantId(defaultVariantId)
                    .status(product.getStatus().name())
                    .build();
    }
}
