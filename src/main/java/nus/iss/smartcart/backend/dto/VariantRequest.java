package nus.iss.smartcart.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VariantRequest {
    @NotBlank
    private String size;

    @NotNull
    @Min(0)
    private Integer stock;
}
