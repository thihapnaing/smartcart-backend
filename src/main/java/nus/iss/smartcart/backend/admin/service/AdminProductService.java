package nus.iss.smartcart.backend.admin.service;

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.admin.dto.AdminProductSummaryDto;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.repository.CartItemRepository;
import nus.iss.smartcart.backend.repository.ProductRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

// AUTHOR: Htet Nandar(Grace)
@Service
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CurrentUserProvider currentUserProvider;
    private final CartItemRepository cartItemRepository;

    public AdminProductService(ProductRepository productRepository, CurrentUserProvider currentUserProvider,
                               CartItemRepository cartItemRepository) {
        this.productRepository = productRepository;
        this.currentUserProvider = currentUserProvider;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public List<AdminProductSummaryDto> getAllProducts(){
        currentUserProvider.getCurrentAdmin(); // Ensure the current user is an admin
        return productRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional
    public AdminProductSummaryDto updateProductStatus(Long productId, ProductStatus status) {
        User admin = currentUserProvider.getCurrentAdmin();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        product.setStatus(status);
        product.setAdminLocked(status == ProductStatus.INACTIVE);
        // Who made this change and when - see Product.lastModifiedByAdmin's javadoc.
        product.setLastModifiedByAdmin(admin);
        product.setLastModifiedAt(LocalDateTime.now(ZoneId.of("Asia/Singapore")));
        Product saved = productRepository.save(product);

        // Deactivating must pull the product out of every cart it's sitting in right now -
        // otherwise it can still reach checkout even though it's hidden everywhere else.
        if (status == ProductStatus.INACTIVE) {
            cartItemRepository.deleteByProductVariant_Product_Id(productId);
        }

        return toSummaryDto(saved);
    }

    private AdminProductSummaryDto toSummaryDto(Product product) {
        return AdminProductSummaryDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory().getName())
                .shopName(product.getShopName())
                .gender(product.getGender() != null ? product.getGender().name() : null)
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .createdAt(product.getCreatedAt())
                .merchantId(product.getMerchant() != null ? product.getMerchant().getId() : null)
                .lastModifiedByAdminUsername(
                        product.getLastModifiedByAdmin() != null ? product.getLastModifiedByAdmin().getUsername() : null)
                .lastModifiedAt(product.getLastModifiedAt())
                .build();
    }
}
