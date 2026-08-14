package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.DeviceTokenRequest;
import nus.iss.smartcart.backend.model.DeliveryDeviceToken;
import nus.iss.smartcart.backend.repository.DeliveryDeviceTokenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {

    private final DeliveryDeviceTokenRepository repository;

    public DeviceTokenController(
            DeliveryDeviceTokenRepository repository
    ) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Void> register(
            @RequestBody DeviceTokenRequest request
    ) {
        DeliveryDeviceToken device =
                repository
                        .findByDeliveryPersonId(
                                request.getDeliveryPersonId()
                        )
                        .orElseGet(DeliveryDeviceToken::new);

        device.setDeliveryPersonId(
                request.getDeliveryPersonId()
        );

        device.setFcmToken(
                request.getFcmToken()
        );

        repository.save(device);

        return ResponseEntity.noContent().build();
    }
}