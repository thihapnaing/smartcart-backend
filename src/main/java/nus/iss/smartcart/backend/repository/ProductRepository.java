package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.Product;
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
    //Added a status filter to only return 'ACTIVE' listings
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND (" +
            "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))) AND " +
            "(:categoryName IS NULL OR LOWER(p.category.name) = LOWER(:categoryName)) AND " +
            "(:gender IS NULL OR p.gender = :gender) " +
            "ORDER BY CASE WHEN :newestFirst = true THEN p.createdAt END DESC, p.id ASC"
    )
    List<Product> search(@Param("keyword") String keyword,
                          @Param("categoryName") String categoryName,
                          @Param("gender") Gender gender,
                          @Param("newestFirst") boolean newestFirst);

}
