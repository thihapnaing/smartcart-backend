package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long productVariantId);
    List<CartItem> findByCartId(Long cartId);

    // Author: Htet Nandar (Grace)
    // Wipes this product out of every customer's cart, not just one - CartItem -> ProductVariant
    // -> Product, so this reaches every size/variant of the deactivated product at once.
    void deleteByProductVariant_Product_Id(Long productId);
}
