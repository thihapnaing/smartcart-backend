package nus.iss.smartcart.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeliveryDetails {
    private String firstName;
    private String lastName;
    private String shippingAddress;
    private String phoneNumber;
}
