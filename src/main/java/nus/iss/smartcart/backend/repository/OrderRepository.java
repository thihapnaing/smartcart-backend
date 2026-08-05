package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    // Author: Htet Nandar (Grace)
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
}
