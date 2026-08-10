package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    // Author: Htet Nandar (Grace)
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
    
    
    @Query("SELECT DISTINCT oi.productVariant.product.name FROM OrderItem oi WHERE oi.order.user.id = :userId")
    List<String> findPurchasedProductNamesByUserId(@Param("userId") Long userId);
    
}
