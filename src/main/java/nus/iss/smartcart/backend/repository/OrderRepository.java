package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    // Author: Htet Nandar (Grace)
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
    
    
    @Query("SELECT DISTINCT oi.productVariant.product.name FROM OrderItem oi WHERE oi.order.user.id = :userId")
    List<String> findPurchasedProductNamesByUserId(@Param("userId") Long userId);

    // use by delivery app
    List<Order> findByDeliveryPersonId(
            Long deliveryPersonId
    );

    List<Order>
    findByDeliveryPersonIdAndStatusInOrderByIdDesc(
            Long deliveryPersonId,
            Collection<OrderStatus> statuses
    );

    List<Order>
    findByDeliveryPersonIdAndStatusOrderByDeliveredAtDesc(
            Long deliveryPersonId,
            OrderStatus status
    );

    Optional<Order> findByTrackingNo(String trackingNo);

    Optional<Order> findByTrackingNoAndDeliveryPersonId(
            String trackingNo,
            Long deliveryPersonId
    );
    
}
