package nus.iss.smartcart.backend.service;

import jakarta.persistence.EntityNotFoundException;

import nus.iss.smartcart.backend.dto.CreateUserProfileRequest;
import nus.iss.smartcart.backend.dto.UserProfileForDeliveryDetails;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserProfile;
import nus.iss.smartcart.backend.repository.UserProfileRepository;
import nus.iss.smartcart.backend.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UserProfileService {

    private final Path uploadDirectory =
            Paths.get("upload");

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    public UserProfileService(UserProfileRepository userProfileRepository, UserRepository userRepository) {
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileForDeliveryDetails getProfileForDeliveryDetails(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile is not found"));
        return UserProfileForDeliveryDetails.builder()
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .address(profile.getAddress())
                .phoneNumber(profile.getPhoneNumber())
                .build();
    }

    //create_profile :: Junior
    @Transactional
    public UserProfile createProfile(
            CreateUserProfileRequest request
    ) {

        if (request.getUserId() == null) {

            throw new IllegalArgumentException(
                    "User ID is required"
            );
        }


        if (request.getFirstName() == null ||
                request.getFirstName().isBlank()) {

            throw new IllegalArgumentException(
                    "First name is required"
            );
        }


        if (request.getLastName() == null ||
                request.getLastName().isBlank()) {

            throw new IllegalArgumentException(
                    "Last name is required"
            );
        }


        if (request.getAddress() == null ||
                request.getAddress().isBlank()) {

            throw new IllegalArgumentException(
                    "Address is required"
            );
        }


        if (request.getPostalCode() == null ||
                request.getPostalCode().isBlank()) {

            throw new IllegalArgumentException(
                    "Postal code is required"
            );
        }


        if (request.getPhoneNumber() == null ||
                request.getPhoneNumber().isBlank()) {

            throw new IllegalArgumentException(
                    "Phone number is required"
            );
        }

        //find user
        User user = userRepository
                .findById(request.getUserId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found"
                        )
                );

        //check duplicate account
        if (userProfileRepository
                .findByUserId(request.getUserId())
                .isPresent()) {

            throw new IllegalArgumentException(
                    "User profile already exists"
            );
        }

        //create
        UserProfile profile =
                new UserProfile();


        profile.setUser(user);


        profile.setFirstName(
                request
                        .getFirstName()
                        .trim()
        );


        profile.setLastName(
                request
                        .getLastName()
                        .trim()
        );


        profile.setAddress(
                request
                        .getAddress()
                        .trim()
        );


        profile.setPostalCode(
                request
                        .getPostalCode()
                        .trim()
        );


        profile.setPhoneNumber(
                request
                        .getPhoneNumber()
                        .trim()
        );

        //Avatar
        if (request.getAvatarUrl() != null &&
                !request.getAvatarUrl().isBlank()) {

            profile.setAvatarUrl(
                    request
                            .getAvatarUrl()
                            .trim()
            );
        }


        return userProfileRepository.save(
                profile
        );
    }
    public String saveAvatar(
            String username,
            MultipartFile avatar
    ) {

        if (avatar == null ||
                avatar.isEmpty()) {

            return null;
        }


        // Check image
        if (avatar.getContentType() == null ||
                !avatar
                        .getContentType()
                        .startsWith("image/")) {

            throw new IllegalArgumentException(
                    "Avatar must be an image file"
            );
        }


        // Clean username
        String safeUsername =
                username
                        .replaceAll(
                                "[^a-zA-Z0-9_-]",
                                "_"
                        );


        // Example:
        // Junior-avatar.jpg

        String filename =
                safeUsername +
                        "-avatar.jpg";


        try {

            Path uploadDirectory =
                    Paths.get("upload");


            // Create upload folder
            Files.createDirectories(
                    uploadDirectory
            );


            Path target =
                    uploadDirectory
                            .resolve(filename);


            // Save file
            Files.write(
                    target,
                    avatar.getBytes()
            );


            System.out.println(
                    "Avatar saved: " +
                            target.toAbsolutePath()
            );


            return "upload/" + filename;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to save avatar",
                    e
            );
        }
    }
}