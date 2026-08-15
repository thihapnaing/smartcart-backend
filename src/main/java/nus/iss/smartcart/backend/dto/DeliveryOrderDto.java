package nus.iss.smartcart.backend.dto;

import java.time.LocalDateTime;

public record DeliveryOrderDto(
        Long id,
        String firstName,
        String lastName,
        String shippingAddress,
        String phoneNumber,
        String status,
        String trackingNo,
        Long deliveryPersonId,
        LocalDateTime deliveredAt,
        String deliveryProofKey
) {
}