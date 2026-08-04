package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long productVariantId);
    List<CartItem> findByCartId(Long cartId);
}
