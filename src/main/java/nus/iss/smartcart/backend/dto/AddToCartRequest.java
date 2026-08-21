package nus.iss.smartcart.backend.dto;

public class AddToCartRequest {

    public AddToCartRequest() {}

    private Long productVariantId;
    private Integer quantity;

    public AddToCartRequest(Long productVariantId, Integer quantity) {
        this.productVariantId = productVariantId;
        this.quantity = quantity;
    }

    public Long getProductVariantId() {
        return productVariantId;
    }

    public void setProductVariantId(Long productVariantId) {
        this.productVariantId = productVariantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
