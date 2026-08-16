package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.CreateMerchantProfileRequest;
import nus.iss.smartcart.backend.model.MerchantProfile;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.repository.MerchantProfileRepository;
import nus.iss.smartcart.backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantProfileServiceTest {

    @Mock
    private MerchantProfileRepository merchantProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MerchantProfileService merchantProfileService;

    private User merchantUser;

    @BeforeEach
    void setUp() {

        merchantUser = mock(User.class);

        lenient().when(merchantUser.getId())
                .thenReturn(1L);

        lenient().when(merchantUser.getRole())
                .thenReturn(UserRole.MERCHANT);
    }

    // SUCCESSFUL MERCHANT PROFILE CREATION
    @Test
    void createMerchantProfile_success() throws Exception {

        CreateMerchantProfileRequest request =
                createValidRequest();

        MultipartFile document =
                createPdfFile();

        lenient().when(request.getRegistrationDocument())
                .thenReturn(document);

        lenient().when(request.getLogo())
                .thenReturn(null);

        prepareValidUser();

        MerchantProfile savedProfile =
                new MerchantProfile();

        when(merchantProfileRepository.save(
                any(MerchantProfile.class)
        )).thenReturn(savedProfile);

        MerchantProfile result =
                merchantProfileService.createMerchantProfile(
                        request
                );

        assertNotNull(result);

        assertEquals(
                savedProfile,
                result
        );

        verify(userRepository)
                .findById(1L);

        verify(merchantProfileRepository)
                .existsByUserId(1L);

        verify(merchantProfileRepository)
                .existsByUen("UEN123456");

        verify(merchantProfileRepository)
                .save(any(MerchantProfile.class));
    }

    // USER ID REQUIRED
    @Test
    void createMerchantProfile_userIdRequired() {

        CreateMerchantProfileRequest request =
                mock(CreateMerchantProfileRequest.class);

        when(request.getUserId())
                .thenReturn(null);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "User ID is required.",
                exception.getReason()
        );

        verifyNoInteractions(
                userRepository,
                merchantProfileRepository
        );
    }

    // USER NOT FOUND
    @Test
    void createMerchantProfile_userNotFound() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                404,
                exception.getStatusCode().value()
        );

        assertEquals(
                "User not found.",
                exception.getReason()
        );
    }

    // USER IS NOT MERCHANT
    @Test
    void createMerchantProfile_userIsNotMerchant() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(merchantUser));

        when(merchantUser.getRole())
                .thenReturn(UserRole.CUSTOMER);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                403,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Only merchant accounts can create a merchant profile.",
                exception.getReason()
        );
    }

    // PROFILE ALREADY EXISTS
    @Test
    void createMerchantProfile_profileAlreadyExists() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(merchantUser));

        when(merchantProfileRepository.existsByUserId(1L))
                .thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                409,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Merchant profile already exists.",
                exception.getReason()
        );
    }

    // BUSINESS NAME REQUIRED
    @Test
    void createMerchantProfile_businessNameRequired() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(request.getBusinessName())
                .thenReturn("");

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Business name is required.",
                exception.getReason()
        );
    }

    // UEN REQUIRED
    @Test
    void createMerchantProfile_uenRequired() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(request.getUen())
                .thenReturn("");

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "UEN is required.",
                exception.getReason()
        );
    }

    // DUPLICATE UEN
    @Test
    void createMerchantProfile_duplicateUen() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        prepareValidUser();

        when(merchantProfileRepository.existsByUen(
                "UEN123456"
        )).thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                409,
                exception.getStatusCode().value()
        );

        assertEquals(
                "UEN is already registered.",
                exception.getReason()
        );
    }

    // BUSINESS TYPE REQUIRED
    @Test
    void createMerchantProfile_businessTypeRequired() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(request.getBusinessType())
                .thenReturn("");

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Business type is required.",
                exception.getReason()
        );
    }


    // BUSINESS ADDRESS REQUIRED
    @Test
    void createMerchantProfile_businessAddressRequired() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(request.getBusinessAddress())
                .thenReturn("");

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Business address is required.",
                exception.getReason()
        );
    }

    // POSTAL CODE REQUIRED
    @Test
    void createMerchantProfile_postalCodeRequired() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(request.getPostalCode())
                .thenReturn("");

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Postal code is required.",
                exception.getReason()
        );
    }

    // CONTACT NUMBER REQUIRED
    @Test
    void createMerchantProfile_contactNumberRequired() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(request.getContactNumber())
                .thenReturn("");

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Contact number is required.",
                exception.getReason()
        );
    }

    // PRODUCT CATEGORY REQUIRED
    @Test
    void createMerchantProfile_productCategoryRequired() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(request.getProductCategory())
                .thenReturn("");

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Product category is required.",
                exception.getReason()
        );
    }


    // BUSINESS DESCRIPTION REQUIRED
    @Test
    void createMerchantProfile_businessDescriptionRequired() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(request.getBusinessDescription())
                .thenReturn("");

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Business description is required.",
                exception.getReason()
        );
    }

    // REGISTRATION DOCUMENT REQUIRED
    @Test
    void createMerchantProfile_registrationDocumentRequired() {

        CreateMerchantProfileRequest request =
                createValidRequest();

        when(request.getRegistrationDocument())
                .thenReturn(null);

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Business registration document is required.",
                exception.getReason()
        );
    }

    // INVALID LOGO TYPE
    @Test
    void createMerchantProfile_invalidLogoType()
            throws Exception {

        CreateMerchantProfileRequest request =
                createValidRequest();

        MultipartFile logo =
                createFile(
                        "logo.gif",
                        "image/gif",
                        1000
                );

        MultipartFile document =
                createPdfFile();

        lenient().when(request.getLogo())
                .thenReturn(logo);

        lenient().when(request.getRegistrationDocument())
                .thenReturn(document);

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Business logo must be JPG, PNG or WEBP.",
                exception.getReason()
        );
    }

    // LOGO TOO LARGE
    @Test
    void createMerchantProfile_logoTooLarge()
            throws Exception {

        CreateMerchantProfileRequest request =
                createValidRequest();

        MultipartFile logo =
                createFile(
                        "logo.jpg",
                        "image/jpeg",
                        2L * 1024L * 1024L + 1
                );

        MultipartFile document =
                createPdfFile();

        lenient().when(request.getLogo())
                .thenReturn(logo);

        lenient().when(request.getRegistrationDocument())
                .thenReturn(document);

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Business logo must be smaller than 2 MB.",
                exception.getReason()
        );
    }

    // INVALID REGISTRATION DOCUMENT TYPE
    @Test
    void createMerchantProfile_invalidDocumentType()
            throws Exception {

        CreateMerchantProfileRequest request =
                createValidRequest();

        MultipartFile document =
                createFile(
                        "document.txt",
                        "text/plain",
                        1000
                );

        lenient().when(request.getRegistrationDocument())
                .thenReturn(document);

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Business registration document must be PDF, JPG or PNG.",
                exception.getReason()
        );
    }

    // REGISTRATION DOCUMENT TOO LARGE
    @Test
    void createMerchantProfile_documentTooLarge()
            throws Exception {

        CreateMerchantProfileRequest request =
                createValidRequest();

        MultipartFile document =
                createFile(
                        "registration.pdf",
                        "application/pdf",
                        5L * 1024L * 1024L + 1
                );

        lenient().when(request.getRegistrationDocument())
                .thenReturn(document);

        prepareValidUser();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                merchantProfileService
                                        .createMerchantProfile(request)
                );

        assertEquals(
                400,
                exception.getStatusCode().value()
        );

        assertEquals(
                "Business registration document must be smaller than 5 MB.",
                exception.getReason()
        );
    }

    // VALID LOGO + DOCUMENT
    @Test
    void createMerchantProfile_validLogo()
            throws Exception {

        CreateMerchantProfileRequest request =
                createValidRequest();

        MultipartFile logo =
                createFile(
                        "logo.png",
                        "image/png",
                        1000
                );

        MultipartFile document =
                createPdfFile();

        lenient().when(request.getLogo())
                .thenReturn(logo);

        lenient().when(request.getRegistrationDocument())
                .thenReturn(document);

        prepareValidUser();

        MerchantProfile savedProfile =
                new MerchantProfile();

        when(merchantProfileRepository.save(
                any(MerchantProfile.class)
        )).thenReturn(savedProfile);

        MerchantProfile result =
                merchantProfileService.createMerchantProfile(
                        request
                );

        assertNotNull(result);

        assertEquals(
                savedProfile,
                result
        );

        verify(merchantProfileRepository)
                .save(any(MerchantProfile.class));
    }

    // VALID REQUEST
    private CreateMerchantProfileRequest createValidRequest() {

        CreateMerchantProfileRequest request =
                mock(CreateMerchantProfileRequest.class);

        lenient().when(request.getUserId())
                .thenReturn(1L);

        lenient().when(request.getBusinessName())
                .thenReturn("Smart Fashion Store");

        lenient().when(request.getUen())
                .thenReturn("UEN123456");

        lenient().when(request.getBusinessType())
                .thenReturn("Retail");

        lenient().when(request.getBusinessAddress())
                .thenReturn("123 Orchard Road");

        lenient().when(request.getPostalCode())
                .thenReturn("238888");

        lenient().when(request.getContactNumber())
                .thenReturn("+6591234567");

        lenient().when(request.getProductCategory())
                .thenReturn("Clothing");

        lenient().when(request.getBusinessDescription())
                .thenReturn(
                        "Smart fashion products for customers."
                );

        lenient().when(request.getPickupAvailable())
                .thenReturn(true);

        return request;
    }

    // VALID USER / REPOSITORY SETUP
    private void prepareValidUser() {

        lenient().when(userRepository.findById(1L))
                .thenReturn(Optional.of(merchantUser));

        lenient().when(
                merchantProfileRepository.existsByUserId(1L)
        ).thenReturn(false);

        lenient().when(
                merchantProfileRepository.existsByUen(
                        "UEN123456"
                )
        ).thenReturn(false);
    }

    // CREATE MOCK FILE
    private MultipartFile createFile(
            String filename,
            String contentType,
            long size
    ) throws Exception {

        MultipartFile file =
                mock(MultipartFile.class);

        lenient().when(file.isEmpty())
                .thenReturn(false);

        lenient().when(file.getOriginalFilename())
                .thenReturn(filename);

        lenient().when(file.getContentType())
                .thenReturn(contentType);

        lenient().when(file.getSize())
                .thenReturn(size);

        lenient().when(file.getInputStream())
                .thenReturn(
                        new ByteArrayInputStream(
                                "test file"
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        )
                        )
                );

        return file;
    }

    // CREATE PDF FILE
    private MultipartFile createPdfFile()
            throws Exception {

        return createFile(
                "registration.pdf",
                "application/pdf",
                1000
        );
    }
}