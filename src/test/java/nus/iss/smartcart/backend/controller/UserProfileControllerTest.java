package nus.iss.smartcart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

import nus.iss.smartcart.backend.dto.CreateUserProfileRequest;
import nus.iss.smartcart.backend.dto.UserProfileForDeliveryDetails;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserProfile;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import nus.iss.smartcart.backend.service.UserProfileService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Updated by Junior

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileController userProfileController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(
                                userProfileController
                        )
                        .build();
        objectMapper =
                new ObjectMapper();
    }

    //UPDATED BY JUNIOR (USER MUST LOGGEDIN)
    @Test
    void getProfile_returnsOkWithProfileData() throws Exception {

        User currentUser = mock(User.class);
        when(currentUser.getId()).thenReturn(2L);
        when(currentUserProvider.getCurrentCustomer()).thenReturn(currentUser);

        UserProfileForDeliveryDetails userProfileForDeliveryDetails = UserProfileForDeliveryDetails.builder()
                        .firstName("John")
                        .lastName("Tan")
                        .address("12 Rainbow Street")
                        .phoneNumber("91234567")
                        .build();
        when(userProfileService.getProfileForDeliveryDetails(2L)).thenReturn(userProfileForDeliveryDetails);
        mockMvc.perform(get("/api/user-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Tan"))
                .andExpect(jsonPath("$.address").value("12 Rainbow Street"))
                .andExpect(jsonPath("$.phoneNumber").value("91234567"));
        verify(currentUserProvider).getCurrentCustomer();
        verify(currentUser).getId();
        verify(userProfileService).getProfileForDeliveryDetails(2L);
    }

    //CREATE PROFILE SUCCESS
    @Test
    void createProfile_success()
            throws Exception {

        // -----------------------------------------------------
        // Request
        // -----------------------------------------------------

        CreateUserProfileRequest request =
                new CreateUserProfileRequest();

        request.setUserId(1L);
        request.setFirstName("Junior");
        request.setLastName("Tan");
        request.setAddress(
                "23 Emerald Hill Road"
        );
        request.setPostalCode("229293");
        request.setPhoneNumber(
                "+6591234567"
        );


        // -----------------------------------------------------
        // Saved profile
        // -----------------------------------------------------

        UserProfile profile =
                mock(UserProfile.class);


        when(
                userProfileService
                        .createProfile(any(
                                CreateUserProfileRequest.class
                        ))
        ).thenReturn(profile);


        // -----------------------------------------------------
        // Request
        // -----------------------------------------------------

        mockMvc.perform(
                        post("/api/user-profile")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                // -----------------------------------------------------
                // Response
                // -----------------------------------------------------

                .andExpect(
                        status().isCreated()
                );


        // -----------------------------------------------------
        // Verify
        // -----------------------------------------------------

        verify(
                userProfileService
        ).createProfile(
                any(CreateUserProfileRequest.class)
        );
    }

    //FIRSTNAME REQUIRED
    @Test
    void createProfile_validationError()
            throws Exception {

        CreateUserProfileRequest request =
                new CreateUserProfileRequest();

        request.setUserId(1L);
        request.setFirstName("");
        request.setLastName("Tan");
        request.setAddress(
                "23 Emerald Hill Road"
        );
        request.setPostalCode("229293");
        request.setPhoneNumber(
                "+6591234567"
        );


        when(
                userProfileService
                        .createProfile(any(
                                CreateUserProfileRequest.class
                        ))
        ).thenThrow(
                new IllegalArgumentException(
                        "First name is required"
                )
        );


        mockMvc.perform(
                        post("/api/user-profile")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isBadRequest()
                )

                .andExpect(
                        content().string(
                                "First name is required"
                        )
                );


        verify(
                userProfileService
        ).createProfile(
                any(CreateUserProfileRequest.class)
        );
    }

    //USER NOT FOUND
    @Test
    void createProfile_userNotFound()
            throws Exception {

        CreateUserProfileRequest request =
                new CreateUserProfileRequest();

        request.setUserId(999L);
        request.setFirstName("Junior");
        request.setLastName("Tan");
        request.setAddress(
                "23 Emerald Hill Road"
        );
        request.setPostalCode("229293");
        request.setPhoneNumber(
                "+6591234567"
        );


        when(
                userProfileService
                        .createProfile(any(
                                CreateUserProfileRequest.class
                        ))
        ).thenThrow(
                new EntityNotFoundException(
                        "User not found"
                )
        );


        mockMvc.perform(
                        post("/api/user-profile")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isNotFound()
                )

                .andExpect(
                        content().string(
                                "User not found"
                        )
                );
    }

    //DUPLICATE USER
    @Test
    void createProfile_duplicateProfile()
            throws Exception {

        CreateUserProfileRequest request =
                new CreateUserProfileRequest();

        request.setUserId(1L);
        request.setFirstName("Junior");
        request.setLastName("Tan");
        request.setAddress(
                "23 Emerald Hill Road"
        );
        request.setPostalCode("229293");
        request.setPhoneNumber(
                "+6591234567"
        );


        when(
                userProfileService
                        .createProfile(any(
                                CreateUserProfileRequest.class
                        ))
        ).thenThrow(
                new IllegalArgumentException(
                        "User profile already exists"
                )
        );


        mockMvc.perform(
                        post("/api/user-profile")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )

                .andExpect(
                        status().isBadRequest()
                )

                .andExpect(
                        content().string(
                                "User profile already exists"
                        )
                );
    }

    // WITH AVATAR SUCCESS
    @Test
    void createProfileWithAvatar_success()
            throws Exception {

        // -----------------------------------------------------
        // User
        // -----------------------------------------------------

        User user =
                mock(User.class);

        when(user.getUsername())
                .thenReturn("Junior");


        when(
                userRepository
                        .findById(1L)
        ).thenReturn(
                Optional.of(user)
        );


        // -----------------------------------------------------
        // Avatar
        // -----------------------------------------------------

        MockMultipartFile avatar =
                new MockMultipartFile(
                        "avatar",
                        "avatar.jpg",
                        "image/jpeg",
                        "fake-image-data"
                                .getBytes()
                );


        // -----------------------------------------------------
        // Service
        // -----------------------------------------------------

        when(
                userProfileService
                        .saveAvatar(
                                eq("Junior"),
                                any(MultipartFile.class)
                        )
        ).thenReturn(
                "upload/Junior-avatar.jpg"
        );


        UserProfile profile =
                mock(UserProfile.class);


        when(
                userProfileService
                        .createProfile(any(
                                CreateUserProfileRequest.class
                        ))
        ).thenReturn(profile);


        // -----------------------------------------------------
        // Request
        // -----------------------------------------------------

        mockMvc.perform(
                        multipart(
                                "/api/user-profile/with-avatar"
                        )

                                .file(avatar)

                                .param(
                                        "userId",
                                        "1"
                                )

                                .param(
                                        "firstName",
                                        "Junior"
                                )

                                .param(
                                        "lastName",
                                        "Tan"
                                )

                                .param(
                                        "address",
                                        "23 Emerald Hill Road"
                                )

                                .param(
                                        "postalCode",
                                        "229293"
                                )

                                .param(
                                        "phoneNumber",
                                        "+6591234567"
                                )
                )

                // -----------------------------------------------------
                // Response
                // -----------------------------------------------------

                .andExpect(
                        status().isCreated()
                );


        // -----------------------------------------------------
        // Verify
        // -----------------------------------------------------

        verify(
                userRepository
        ).findById(1L);


        verify(
                userProfileService
        ).saveAvatar(
                eq("Junior"),
                any(MultipartFile.class)
        );


        verify(
                userProfileService
        ).createProfile(
                any(CreateUserProfileRequest.class)
        );
    }

    //WITHOUT AVATAR SUCCESS
    @Test
    void createProfileWithAvatar_withoutAvatar()
            throws Exception {

        // -----------------------------------------------------
        // User
        // -----------------------------------------------------

        User user =
                mock(User.class);

        when(
                userRepository
                        .findById(1L)
        ).thenReturn(
                Optional.of(user)
        );


        // -----------------------------------------------------
        // Profile
        // -----------------------------------------------------

        UserProfile profile =
                mock(UserProfile.class);

        when(
                userProfileService
                        .createProfile(any(
                                CreateUserProfileRequest.class
                        ))
        ).thenReturn(profile);


        // -----------------------------------------------------
        // Request WITHOUT avatar
        // -----------------------------------------------------

        mockMvc.perform(
                        multipart(
                                "/api/user-profile/with-avatar"
                        )

                                .param(
                                        "userId",
                                        "1"
                                )

                                .param(
                                        "firstName",
                                        "Junior"
                                )

                                .param(
                                        "lastName",
                                        "Tan"
                                )

                                .param(
                                        "address",
                                        "23 Emerald Hill Road"
                                )

                                .param(
                                        "postalCode",
                                        "229293"
                                )

                                .param(
                                        "phoneNumber",
                                        "+6591234567"
                                )
                )

                .andExpect(
                        status().isCreated()
                );


        // -----------------------------------------------------
        // Avatar should NOT be saved
        // -----------------------------------------------------

        verify(
                userProfileService,
                never()
        ).saveAvatar(
                anyString(),
                any(MultipartFile.class)
        );


        // -----------------------------------------------------
        // Profile should be created
        // -----------------------------------------------------

        verify(
                userProfileService
        ).createProfile(
                any(CreateUserProfileRequest.class)
        );


        // -----------------------------------------------------
        // User should be found
        // -----------------------------------------------------

        verify(
                userRepository
        ).findById(1L);
    }

    //AVATAR NOT IMAGE FILE
    @Test
    void createProfileWithAvatar_invalidAvatar()
            throws Exception {

        // -----------------------------------------------------
        // User
        // -----------------------------------------------------

        User user =
                mock(User.class);

        when(user.getUsername())
                .thenReturn("Junior");


        when(
                userRepository
                        .findById(1L)
        ).thenReturn(
                Optional.of(user)
        );


        // -----------------------------------------------------
        // Invalid file
        // -----------------------------------------------------

        MockMultipartFile avatar =
                new MockMultipartFile(
                        "avatar",
                        "document.pdf",
                        "application/pdf",
                        "fake-pdf-data"
                                .getBytes()
                );


        // -----------------------------------------------------
        // Service throws validation error
        // -----------------------------------------------------

        when(
                userProfileService
                        .saveAvatar(
                                eq("Junior"),
                                any(MultipartFile.class)
                        )
        ).thenThrow(
                new IllegalArgumentException(
                        "Avatar must be an image file"
                )
        );


        // -----------------------------------------------------
        // Request
        // -----------------------------------------------------

        mockMvc.perform(
                        multipart(
                                "/api/user-profile/with-avatar"
                        )

                                .file(avatar)

                                .param(
                                        "userId",
                                        "1"
                                )

                                .param(
                                        "firstName",
                                        "Junior"
                                )

                                .param(
                                        "lastName",
                                        "Tan"
                                )

                                .param(
                                        "address",
                                        "23 Emerald Hill Road"
                                )

                                .param(
                                        "postalCode",
                                        "229293"
                                )

                                .param(
                                        "phoneNumber",
                                        "+6591234567"
                                )
                )

                .andExpect(
                        status().isBadRequest()
                )

                .andExpect(
                        content().string(
                                "Avatar must be an image file"
                        )
                );


        // -----------------------------------------------------
        // Profile should NOT be created
        // -----------------------------------------------------

        verify(
                userProfileService,
                never()
        ).createProfile(
                any(CreateUserProfileRequest.class)
        );
    }
}
