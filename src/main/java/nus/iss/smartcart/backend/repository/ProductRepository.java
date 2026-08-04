package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))"
    )
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    List<Product> findByNameContainingIgnoreCaseAndCategoryId(String keyword, Long categoryId);

}
