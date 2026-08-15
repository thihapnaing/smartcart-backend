package nus.iss.smartcart.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    private String description;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal price;

    @NotNull
    private Gender gender;

    @NotNull
    private Long categoryId;

    @NotNull
    private ProductStatus status;

    private String imageUrl;

    private String color;

    @NotEmpty
    @Valid
    private List<VariantRequest> variants;
}
