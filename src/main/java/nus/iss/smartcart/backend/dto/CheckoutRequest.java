package nus.iss.smartcart.backend.dto;

import nus.iss.smartcart.backend.model.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {

    public CheckoutRequest() {}

    private String firstName;
    private String lastName;
    private String shippingAddress;
    private String phoneNumber;
    private PaymentMethod paymentMethod;


}
