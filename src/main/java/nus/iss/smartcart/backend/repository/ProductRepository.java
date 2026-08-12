package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND (" +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))"
    )
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    List<Product> findByNameContainingIgnoreCaseAndCategoryId(String keyword, Long categoryId);

    // Author: Htet Nandar (Grace)
    /**
     * Powers the AI chat's "new arrivals"/category/gender filters. All filter params are optional
     * (pass null to skip) - keyword matches name/description, category/gender match by name/enum,
     * newestFirst true sorts by createdAt desc, false leaves default ordering (by id).
     */
    @Query("SELECT p FROM Product p WHERE " +
            "p.status = :status AND " +
            "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:categoryName IS NULL OR LOWER(p.category.name) = LOWER(:categoryName)) AND " +
            "(:gender IS NULL OR p.gender = :gender) " +
            "ORDER BY CASE WHEN :newestFirst = true THEN p.createdAt END DESC, p.id ASC"
    )
    List<Product> search(@Param("keyword") String keyword,
                          @Param("categoryName") String categoryName,
                          @Param("gender") Gender gender,
                          @Param("newestFirst") boolean newestFirst,
                          @Param("status") ProductStatus status);

    List<Product> findByMerchantId(Long merchantId);

    // Admin moderation queue - deliberately unfiltered by status, so admins can see and
    // reactivate previously-deactivated listings too, not just the active ones.
    List<Product> findAllByOrderByCreatedAtDesc();

    //Author: Junior
    @Query("""
    SELECT DISTINCT p
    FROM Product p
    JOIN p.category c
    LEFT JOIN FETCH p.variants v
    WHERE p.status = :status
      AND (:gender IS NULL OR p.gender = :gender)
      AND (:color IS NULL OR LOWER(p.color) = LOWER(:color))
      AND (:category IS NULL OR LOWER(c.name) = LOWER(:category))
    ORDER BY p.createdAt DESC
    """)
    List<Product> searchByImageAttributes(
            @Param("gender") Gender gender,
            @Param("color") String color,
            @Param("category") String category,
            @Param("status") ProductStatus status
    );

}
