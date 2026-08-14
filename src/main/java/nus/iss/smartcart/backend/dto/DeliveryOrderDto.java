package nus.iss.smartcart.backend.dto;

import java.time.LocalDateTime;

public record DeliveryOrderDto(
        Long id,
        String firstName,
        String lastName,
        String status,
        String trackingNo,
        Long deliveryPersonId,
        String deliveryPersonName,
        LocalDateTime deliveredAt
) {
}