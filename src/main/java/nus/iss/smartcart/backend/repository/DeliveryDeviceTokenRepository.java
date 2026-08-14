package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.DeliveryDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeliveryDeviceTokenRepository
        extends JpaRepository<DeliveryDeviceToken, Long> {

    Optional<DeliveryDeviceToken>
    findByDeliveryPersonId(Long deliveryPersonId);
}