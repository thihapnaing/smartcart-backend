package nus.iss.smartcart.backend.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nus.iss.smartcart.backend.model.ProductStatus;

// AUTHOR: Htet Nandar(Grace)
@Getter
@Setter
public class UpdateProductStatusRequest {

    @NotNull
    private ProductStatus status;
}
