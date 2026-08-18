package nus.iss.smartcart.backend.service;

import jakarta.persistence.EntityNotFoundException;

import nus.iss.smartcart.backend.dto.CreateUserProfileRequest;
import nus.iss.smartcart.backend.dto.UserProfileForDeliveryDetails;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserProfile;
import nus.iss.smartcart.backend.repository.UserProfileRepository;
import nus.iss.smartcart.backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserProfileRepository userProfileRepository;

    @Mock private UserRepository userRepository;

    @InjectMocks private UserProfileService userProfileService;

    private User testUser;

    @BeforeEach
    void setUp() {

        testUser = mock(User.class);

        lenient()
                .when(testUser.getId())
                .thenReturn(1L);
    }

    // GET PROFILE FOR DELIVERY
    @Test
    void getProfileForDeliveryDetails_success() {

        UserProfile profile =
                new UserProfile();

        profile.setFirstName("Junior");
        profile.setLastName("Tan");
        profile.setAddress("123 Orchard Road");
        profile.setPhoneNumber("+6591234567");


        when(userProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(profile));


        UserProfileForDeliveryDetails result =
                userProfileService
                        .getProfileForDeliveryDetails(1L);


        assertNotNull(result);

        assertEquals(
                "Junior",
                result.getFirstName()
        );

        assertEquals(
                "Tan",
                result.getLastName()
        );

        assertEquals(
                "123 Orchard Road",
                result.getAddress()
        );

        assertEquals(
                "+6591234567",
                result.getPhoneNumber()
        );


        verify(userProfileRepository)
                .findByUserId(1L);
    }

    @Test
    void getUserProfile_noUserProfileFound_throwsEntityNotFoundException() {
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userProfileService.getProfileForDeliveryDetails(1L));
    }

    @Test
    void getUserProfile_userProfileFound_returnsPopulatedResponse() {
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(userProfile));
        when(userProfile.getFirstName()).thenReturn("John");
        when(userProfile.getLastName()).thenReturn("Smith");
        when(userProfile.getAddress()).thenReturn("123 Rainbow Street");
        when(userProfile.getPhoneNumber()).thenReturn("91234567");
        UserProfileForDeliveryDetails response = userProfileService.getProfileForDeliveryDetails(1L);
        assertEquals("John", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals("123 Rainbow Street", response.getAddress());
        assertEquals("91234567", response.getPhoneNumber());
    }

    // CREATE PROFILE SUCCESS
    @Test
    void createProfile_success() {

        CreateUserProfileRequest request =
                createValidRequest();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(userProfileRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        UserProfile savedProfile =
                new UserProfile();

        when(userProfileRepository.save(
                any(UserProfile.class)
        )).thenReturn(savedProfile);

        UserProfile result =
                userProfileService.createProfile(
                        request
                );

        assertNotNull(result);
        assertEquals(
                savedProfile,
                result
        );

        verify(userRepository)
                .findById(1L);

        verify(userProfileRepository)
                .findByUserId(1L);

        verify(userProfileRepository)
                .save(any(UserProfile.class));
    }

    //ID IS MISSING
    @Test
    void createProfile_userIdRequired() {

        CreateUserProfileRequest request =
                mock(CreateUserProfileRequest.class);


        when(request.getUserId())
                .thenReturn(null);


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "User ID is required",
                exception.getMessage()
        );


        verifyNoInteractions(
                userRepository,
                userProfileRepository
        );
    }

    //FIRSTNAME IS MISSING
    @Test
    void createProfile_firstNameRequired() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getFirstName())
                .thenReturn(null);


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "First name is required",
                exception.getMessage()
        );
    }

    //FIRSTNAME IS BLANK
    @Test
    void createProfile_firstNameBlank() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getFirstName())
                .thenReturn("   ");


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "First name is required",
                exception.getMessage()
        );
    }

    @Test
    void createProfile_lastNameRequired() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getLastName())
                .thenReturn(null);


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "Last name is required",
                exception.getMessage()
        );
    }

    //LASTNAME BLANK
    @Test
    void createProfile_lastNameBlank() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getLastName())
                .thenReturn("   ");


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "Last name is required",
                exception.getMessage()
        );
    }

    //ADDRESS
    @Test
    void createProfile_addressRequired() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getAddress())
                .thenReturn(null);


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "Address is required",
                exception.getMessage()
        );
    }

    // ADDRESS BLANK
    @Test
    void createProfile_addressBlank() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getAddress())
                .thenReturn("   ");


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "Address is required",
                exception.getMessage()
        );
    }


    //POSTAL CODE REQUIRED
    @Test
    void createProfile_postalCodeRequired() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getPostalCode())
                .thenReturn(null);


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "Postal code is required",
                exception.getMessage()
        );
    }

    //POSTAL CODE BLANK
    @Test
    void createProfile_postalCodeBlank() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getPostalCode())
                .thenReturn("   ");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );

        assertEquals(
                "Postal code is required",
                exception.getMessage()
        );
    }

   //PHONE NUMBER REQUIRED
    @Test
    void createProfile_phoneNumberRequired() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getPhoneNumber())
                .thenReturn(null);


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "Phone number is required",
                exception.getMessage()
        );
    }

    //PHONE NUMBER BLANK
    @Test
    void createProfile_phoneNumberBlank() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getPhoneNumber())
                .thenReturn("   ");


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "Phone number is required",
                exception.getMessage()
        );
    }

    // USER NOT FOUND
    @Test
    void createProfile_userNotFound() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());


        EntityNotFoundException exception =
                assertThrows(
                        EntityNotFoundException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "User not found",
                exception.getMessage()
        );


        verify(userRepository)
                .findById(1L);
    }

    //USER PROFILE EXIST
    @Test
    void createProfile_profileAlreadyExists() {

        CreateUserProfileRequest request =
                createValidRequest();


        UserProfile existingProfile =
                new UserProfile();


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));


        when(userProfileRepository.findByUserId(1L))
                .thenReturn(
                        Optional.of(existingProfile)
                );


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .createProfile(request)
                );


        assertEquals(
                "User profile already exists",
                exception.getMessage()
        );


        verify(userRepository)
                .findById(1L);

        verify(userProfileRepository)
                .findByUserId(1L);
    }


    /**
     * Test 17:
     * Avatar URL is saved when provided.
     */
    @Test
    void createProfile_avatarUrlSaved() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getAvatarUrl())
                .thenReturn(
                        " upload/Junior-avatar.jpg "
                );


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));


        when(userProfileRepository.findByUserId(1L))
                .thenReturn(Optional.empty());


        when(userProfileRepository.save(
                any(UserProfile.class)
        )).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );


        UserProfile result =
                userProfileService.createProfile(
                        request
                );


        assertNotNull(result);

        assertEquals(
                "upload/Junior-avatar.jpg",
                result.getAvatarUrl()
        );


        verify(userProfileRepository)
                .save(any(UserProfile.class));
    }


    /**
     * Test 18:
     * Avatar URL is not set when null.
     */
    @Test
    void createProfile_avatarUrlNull() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getAvatarUrl())
                .thenReturn(null);


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));


        when(userProfileRepository.findByUserId(1L))
                .thenReturn(Optional.empty());


        when(userProfileRepository.save(
                any(UserProfile.class)
        )).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );


        UserProfile result =
                userProfileService.createProfile(
                        request
                );


        assertNotNull(result);

        assertNull(
                result.getAvatarUrl()
        );
    }

    //TRIMMED INPUT VALUE
    @Test
    void createProfile_valuesAreTrimmed() {

        CreateUserProfileRequest request =
                createValidRequest();


        when(request.getFirstName())
                .thenReturn("  Junior  ");

        when(request.getLastName())
                .thenReturn("  Tan  ");

        when(request.getAddress())
                .thenReturn("  123 Orchard Road  ");

        when(request.getPostalCode())
                .thenReturn("  238888  ");

        when(request.getPhoneNumber())
                .thenReturn("  +6591234567  ");


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));


        when(userProfileRepository.findByUserId(1L))
                .thenReturn(Optional.empty());


        when(userProfileRepository.save(
                any(UserProfile.class)
        )).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );


        UserProfile result =
                userProfileService.createProfile(
                        request
                );


        assertEquals(
                "Junior",
                result.getFirstName()
        );

        assertEquals(
                "Tan",
                result.getLastName()
        );

        assertEquals(
                "123 Orchard Road",
                result.getAddress()
        );

        assertEquals(
                "238888",
                result.getPostalCode()
        );

        assertEquals(
                "+6591234567",
                result.getPhoneNumber()
        );
    }


    // =========================================================
    // SAVE AVATAR
    // =========================================================


    /**
     * Test 20:
     * Null avatar returns null.
     */
    @Test
    void saveAvatar_nullAvatar() {

        String result =
                userProfileService.saveAvatar(
                        "Junior",
                        null
                );


        assertNull(result);
    }


    /**
     * Test 21:
     * Empty avatar returns null.
     */
    @Test
    void saveAvatar_emptyAvatar()
    {

        MultipartFile avatar =
                mock(MultipartFile.class);


        when(avatar.isEmpty())
                .thenReturn(true);


        String result =
                userProfileService.saveAvatar(
                        "Junior",
                        avatar
                );


        assertNull(result);
    }


    /**
     * Test 22:
     * Avatar must be an image.
     */
    @Test
    void saveAvatar_invalidFileType() {

        MultipartFile avatar =
                mock(MultipartFile.class);


        when(avatar.isEmpty())
                .thenReturn(false);


        when(avatar.getContentType())
                .thenReturn("application/pdf");


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                userProfileService
                                        .saveAvatar(
                                                "Junior",
                                                avatar
                                        )
                );


        assertEquals(
                "Avatar must be an image file",
                exception.getMessage()
        );
    }


    /**
     * Test 23:
     * Valid image avatar.
     */
    @Test
    void saveAvatar_success()
            throws Exception {

        MultipartFile avatar =
                mock(MultipartFile.class);


        byte[] imageBytes =
                "test image".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                );


        when(avatar.isEmpty())
                .thenReturn(false);


        when(avatar.getContentType())
                .thenReturn("image/jpeg");


        when(avatar.getBytes())
                .thenReturn(imageBytes);


        String result =
                userProfileService.saveAvatar(
                        "Junior",
                        avatar
                );


        assertNotNull(result);


        assertEquals(
                "upload/Junior-avatar.jpg",
                result
        );


        verify(avatar)
                .getBytes();
    }


    /**
     * Test 24:
     * Username is cleaned before creating filename.
     * Example:
     * Junior@Test
     * becomes
     * Junior_Test-avatar.jpg
     */
    @Test
    void saveAvatar_usernameIsSanitized()
            throws Exception {

        MultipartFile avatar =
                mock(MultipartFile.class);


        when(avatar.isEmpty())
                .thenReturn(false);


        when(avatar.getContentType())
                .thenReturn("image/png");


        when(avatar.getBytes())
                .thenReturn(
                        "test image"
                                .getBytes(
                                        java.nio.charset.StandardCharsets.UTF_8
                                )
                );


        String result =
                userProfileService.saveAvatar(
                        "Junior@Test",
                        avatar
                );


        assertNotNull(result);


        assertEquals(
                "upload/Junior_Test-avatar.jpg",
                result
        );
    }

    // CREATE VALID REQUEST
    private CreateUserProfileRequest createValidRequest() {

        CreateUserProfileRequest request =
                mock(CreateUserProfileRequest.class);


        lenient()
                .when(request.getUserId())
                .thenReturn(1L);


        lenient()
                .when(request.getFirstName())
                .thenReturn("Junior");


        lenient()
                .when(request.getLastName())
                .thenReturn("Tan");


        lenient()
                .when(request.getAddress())
                .thenReturn("123 Orchard Road");


        lenient()
                .when(request.getPostalCode())
                .thenReturn("238888");


        lenient()
                .when(request.getPhoneNumber())
                .thenReturn("+6591234567");


        lenient()
                .when(request.getAvatarUrl())
                .thenReturn(null);


        return request;
    }
}
