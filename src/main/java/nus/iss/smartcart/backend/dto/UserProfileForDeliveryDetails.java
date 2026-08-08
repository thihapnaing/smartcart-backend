package nus.iss.smartcart.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileForDeliveryDetails {
    private String firstName;
    private String lastName;
    private String address;
    private String phoneNumber;
}
