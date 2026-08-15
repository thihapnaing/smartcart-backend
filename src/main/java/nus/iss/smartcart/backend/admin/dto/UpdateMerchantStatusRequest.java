package nus.iss.smartcart.backend.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nus.iss.smartcart.backend.model.UserStatus;

// AUTHOR: Htet Nandar(Grace)
@Getter
@Setter
public class UpdateMerchantStatusRequest {

    @NotNull
    private UserStatus status;
}
