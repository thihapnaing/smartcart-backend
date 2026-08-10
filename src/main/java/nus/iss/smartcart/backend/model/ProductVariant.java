package nus.iss.smartcart.backend.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "product_variant",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "size"})
)
public class ProductVariant {

    public ProductVariant() {
        // Intentionally empty: required by JPA/Hibernate for entity instantiation.
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String size;

    private Integer stock;

    public Long getId() {
        return id;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}
