package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    @Query("Select oi FROM OrderItem oi WHERE oi.productVariant.product.merchant.id = :merchantId " +
           "ORDER BY oi.order.orderDate DESC")
    List<OrderItem> findByProductVariantProductMerchantId(@Param("merchantId") Long merchantId);
}
