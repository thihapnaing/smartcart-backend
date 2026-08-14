package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.dto.ProductVectorDTO;
import nus.iss.smartcart.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

//@Repository
public interface ProductVectorRepository extends JpaRepository<Product, Long> {

    @Query(value = """
            SELECT
                p.id AS productId,
                p.name AS productName,
                p.description AS description,
                p.gender AS gender,
                p.price AS price,
                p.image_url AS imageUrl,
                c.name AS category,
                COALESCE(GROUP_CONCAT(DISTINCT pv.size ORDER BY pv.size SEPARATOR ', '), 'N/A') AS availableSizes,
                CAST(COALESCE(SUM(pv.stock), 0) AS SIGNED) AS totalStock
            FROM product p
            LEFT JOIN category c ON p.category_id = c.id
            LEFT JOIN product_variant pv ON p.id = pv.product_id
            GROUP BY p.id
            """, nativeQuery = true)
    List<ProductVectorDTO> getProductVectorData();
}
