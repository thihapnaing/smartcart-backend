package nus.iss.smartcart.backend.model;

import jakarta.persistence.*;

@Entity
@Table (
        name = "cart_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_variant_id"})
)
public class CartItem {

    public CartItem() {}

    public CartItem(Cart cart, ProductVariant productVariant, Integer quantity) {
        this.cart = cart;
        this.productVariant = productVariant;
        this.quantity = quantity;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    private Integer quantity;

    //Getters and Setters

    public Long getId() {
        return id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public ProductVariant getProductVariant() {
        return productVariant;
    }

    public void setProductVariant(ProductVariant productVariant) {
        this.productVariant = productVariant;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
