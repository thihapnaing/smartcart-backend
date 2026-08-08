package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.UserProfileForDeliveryDetails;
import nus.iss.smartcart.backend.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-profile")
@CrossOrigin(origins = "http://localhost:4200")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping()
    public ResponseEntity<UserProfileForDeliveryDetails> getProfile() {
        Long userId = 2L; //to replace with authenticated user once auth is implemented
        return ResponseEntity.ok(userProfileService.getProfileForDeliveryDetails(userId));
    }
}
