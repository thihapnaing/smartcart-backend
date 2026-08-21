package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
    
    @Query("SELECT DISTINCT ci.productVariant.product.name FROM CartItem ci WHERE ci.cart.user.id = :userId")
    List<String> findProductNamesByUserId(@Param("userId") Long userId);
    
}
