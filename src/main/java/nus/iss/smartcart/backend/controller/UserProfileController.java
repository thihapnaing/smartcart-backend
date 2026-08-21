package nus.iss.smartcart.backend.controller;

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.dto.CreateUserProfileRequest;
import nus.iss.smartcart.backend.dto.UserProfileForDeliveryDetails;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserProfile;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import nus.iss.smartcart.backend.service.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user-profile")
@CrossOrigin(origins = "http://localhost:4200")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    public UserProfileController(UserProfileService userProfileService, CurrentUserProvider currentUserProvider, UserRepository userRepository) {
        this.userProfileService = userProfileService;
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
    }

    @GetMapping()
    public ResponseEntity<UserProfileForDeliveryDetails> getProfile() {
        Long userId = currentUserProvider.getCurrentCustomer().getId();
        return ResponseEntity.ok(userProfileService.getProfileForDeliveryDetails(userId));
    }

    //createProfile w/o :: Junior
    @PostMapping
    public ResponseEntity<Object> createProfile(
            @RequestBody CreateUserProfileRequest request
    ) {

        try {

            UserProfile profile =
                    userProfileService
                            .createProfile(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(profile);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (EntityNotFoundException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "Unable to create user profile."
                    );
        }
    }

    //upload avatar on upload :: Junior
    @PostMapping(
            value = "/with-avatar",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<Object> createProfileWithAvatar(

            @RequestParam("userId")
            Long userId,

            @RequestParam("firstName")
            String firstName,

            @RequestParam("lastName")
            String lastName,

            @RequestParam("address")
            String address,

            @RequestParam("postalCode")
            String postalCode,

            @RequestParam("phoneNumber")
            String phoneNumber,

            @RequestPart(
                    value = "avatar",
                    required = false
            )
            MultipartFile avatar

    ) {

        try {

            // =================================================
            // FIND USER
            // =================================================

            User user =
                    userRepository
                            .findById(userId)
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "User not found"
                                    )
                            );


            // =================================================
            // CREATE REQUEST
            // =================================================

            CreateUserProfileRequest request =
                    new CreateUserProfileRequest();

            request.setUserId(userId);

            request.setFirstName(firstName);

            request.setLastName(lastName);

            request.setAddress(address);

            request.setPostalCode(postalCode);

            request.setPhoneNumber(phoneNumber);


            // =================================================
            // SAVE AVATAR
            // =================================================

            if (avatar != null &&
                    !avatar.isEmpty()) {

                String avatarUrl =
                        userProfileService.saveAvatar(
                                user.getUsername(),
                                avatar
                        );

                request.setAvatarUrl(
                        avatarUrl
                );
            }


            // =================================================
            // SAVE PROFILE
            // =================================================

            UserProfile profile =
                    userProfileService
                            .createProfile(request);


            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(profile);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (EntityNotFoundException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (Exception e) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            "Unable to create user profile."
                    );
        }
    }
}
