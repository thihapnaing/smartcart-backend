package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.ProductDetailResponse;
import nus.iss.smartcart.backend.dto.ProductSearchResult;
import nus.iss.smartcart.backend.dto.ProductVariantDetail;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductVariant;
import nus.iss.smartcart.backend.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
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
    @Transactional
    public List<ProductSearchResult> search(String keyword, String categoryName, Gender gender, boolean newestFirst, int limit) {
        List<Product> products = productRepository.search(keyword, categoryName, gender, newestFirst);
        return products.stream()
                .limit(Math.max(1, limit))
                .map(this::toSearchResult)
                .toList();
    }

    @Transactional
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product is not found"));
        List<ProductVariant> productVariants = product.getVariants();

        return createProductDetailResponse(product, productVariants);

    }

    private ProductDetailResponse createProductDetailResponse(Product product, List<ProductVariant> productVariants) {
        List<ProductVariantDetail> variantDetailList = productVariants.stream()
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
                    .build();
    }
}
