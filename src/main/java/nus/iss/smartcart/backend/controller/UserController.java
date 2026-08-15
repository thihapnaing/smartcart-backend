package nus.iss.smartcart.backend.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nus.iss.smartcart.backend.dto.DeliveryPersonDto;
import nus.iss.smartcart.backend.service.UserService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(
            value = "/delivery-men",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<DeliveryPersonDto>>getDeliveryPeople() {

        List<DeliveryPersonDto> deliveryPersonel =
                userService.getDeliveryPersonel();

        return ResponseEntity.ok(deliveryPersonel);
    }
}