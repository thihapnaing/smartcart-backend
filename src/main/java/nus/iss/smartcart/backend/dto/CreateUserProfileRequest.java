package nus.iss.smartcart.backend.dto;

// Author: Junior

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserProfileRequest {

    private Long userId;

    private String firstName;

    private String lastName;

    private String address;

    private String postalCode;

    private String phoneNumber;

    private String avatarUrl;

    public CreateUserProfileRequest() {
        // Required by the controller to construct the request DTO
        // before assigning multipart form values.
    }
}