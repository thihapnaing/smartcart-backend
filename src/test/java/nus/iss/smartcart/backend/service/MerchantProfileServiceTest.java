package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.CreateMerchantProfileRequest;
import nus.iss.smartcart.backend.model.MerchantProfile;
import nus.iss.smartcart.backend.model.MerchantVerificationStatus;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.repository.MerchantProfileRepository;
import nus.iss.smartcart.backend.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantProfileServiceTest {

    @Mock
    private MerchantProfileRepository merchantProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CreateMerchantProfileRequest request;

    private MerchantProfileService service;

    private final Path uploadDirectory =
            Paths.get("upload", "merchant")
                    .toAbsolutePath()
                    .normalize();

    @BeforeEach
    void setUp() {
        service = new MerchantProfileService(
                merchantProfileRepository,
                userRepository
        );
    }

    @AfterEach
    void cleanupUploadedFiles() throws IOException {
        if (Files.exists(uploadDirectory)) {
            try (var files = Files.list(uploadDirectory)) {
                files.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Test cleanup only.
                    }
                });
            }

            Files.deleteIfExists(uploadDirectory);
        }
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    @Test
    void createMerchantProfile_shouldRejectNullRequest() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(null)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "Request is required.",
                exception.getReason()
        );

        verifyNoInteractions(
                merchantProfileRepository,
                userRepository
        );
    }

    @Test
    void createMerchantProfile_shouldRejectMissingUserId() {

        when(request.getUserId()).thenReturn(null);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "User ID is required.",
                exception.getReason()
        );

        verifyNoInteractions(
                merchantProfileRepository,
                userRepository
        );
    }

    // =========================================================
    // USER VALIDATION
    // =========================================================

    @Test
    void createMerchantProfile_shouldRejectUnknownUser() {

        when(request.getUserId()).thenReturn(99L);

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        assertEquals(
                "User not found.",
                exception.getReason()
        );

        verify(userRepository).findById(99L);

        verifyNoInteractions(
                merchantProfileRepository
        );
    }

    @Test
    void createMerchantProfile_shouldRejectNonMerchantUser() {

        when(request.getUserId()).thenReturn(1L);

        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CUSTOMER);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        assertEquals(
                "Only merchant accounts can create a merchant profile.",
                exception.getReason()
        );

        verify(userRepository).findById(1L);

        verifyNoInteractions(
                merchantProfileRepository
        );
    }

    @Test
    void createMerchantProfile_shouldRejectExistingProfile() {

        when(request.getUserId()).thenReturn(1L);

        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.MERCHANT);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(merchantProfileRepository.existsByUserId(1L))
                .thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "Merchant profile already exists.",
                exception.getReason()
        );

        verify(merchantProfileRepository)
                .existsByUserId(1L);

        verify(merchantProfileRepository, never())
                .save(any(MerchantProfile.class));
    }

    // =========================================================
    // UEN VALIDATION
    // =========================================================

    @Test
    void createMerchantProfile_shouldRejectMissingUen() {

        when(request.getUserId()).thenReturn(1L);

        User user = merchantUser(1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(merchantProfileRepository.existsByUserId(1L))
                .thenReturn(false);

        when(request.getUen()).thenReturn(" ");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                "UEN is required.",
                exception.getReason()
        );

        verify(merchantProfileRepository, never())
                .existsByUen(anyString());
    }

    @Test
    void createMerchantProfile_shouldRejectDuplicateUen() {

        when(request.getUserId()).thenReturn(1L);

        User user = merchantUser(1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(merchantProfileRepository.existsByUserId(1L))
                .thenReturn(false);

        when(request.getUen())
                .thenReturn(" 201912345A ");

        when(merchantProfileRepository.existsByUen("201912345A"))
                .thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "UEN is already registered.",
                exception.getReason()
        );

        verify(merchantProfileRepository)
                .existsByUen("201912345A");
    }

    // =========================================================
    // BUSINESS INFORMATION VALIDATION
    // =========================================================

    @Test
    void createMerchantProfile_shouldRejectMissingBusinessName() {

        prepareRequestBeforeBusinessValidation();

        when(request.getBusinessName()).thenReturn(" ");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Business name is required.",
                exception.getReason()
        );
    }

    @Test
    void createMerchantProfile_shouldRejectMissingBusinessType() {

        prepareRequestBeforeBusinessValidation();

        when(request.getBusinessName()).thenReturn("SmartCart Store");
        when(request.getBusinessType()).thenReturn(" ");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Business type is required.",
                exception.getReason()
        );
    }

    @Test
    void createMerchantProfile_shouldRejectMissingBusinessAddress() {

        prepareRequestBeforeBusinessValidation();

        when(request.getBusinessName()).thenReturn("SmartCart Store");
        when(request.getBusinessType()).thenReturn("Retail");
        when(request.getBusinessAddress()).thenReturn(" ");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Business address is required.",
                exception.getReason()
        );
    }

    @Test
    void createMerchantProfile_shouldRejectMissingPostalCode() {

        prepareRequestBeforeBusinessValidation();

        when(request.getBusinessName()).thenReturn("SmartCart Store");
        when(request.getBusinessType()).thenReturn("Retail");
        when(request.getBusinessAddress()).thenReturn("Singapore");
        when(request.getPostalCode()).thenReturn(" ");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Postal code is required.",
                exception.getReason()
        );
    }

    @Test
    void createMerchantProfile_shouldRejectMissingContactNumber() {

        prepareRequestBeforeBusinessValidation();

        when(request.getBusinessName()).thenReturn("SmartCart Store");
        when(request.getBusinessType()).thenReturn("Retail");
        when(request.getBusinessAddress()).thenReturn("Singapore");
        when(request.getPostalCode()).thenReturn("123456");
        when(request.getContactNumber()).thenReturn(" ");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Contact number is required.",
                exception.getReason()
        );
    }

    @Test
    void createMerchantProfile_shouldRejectMissingProductCategory() {

        prepareRequestBeforeBusinessValidation();

        when(request.getBusinessName()).thenReturn("SmartCart Store");
        when(request.getBusinessType()).thenReturn("Retail");
        when(request.getBusinessAddress()).thenReturn("Singapore");
        when(request.getPostalCode()).thenReturn("123456");
        when(request.getContactNumber()).thenReturn("91234567");
        when(request.getProductCategory()).thenReturn(" ");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Product category is required.",
                exception.getReason()
        );
    }

    @Test
    void createMerchantProfile_shouldRejectMissingBusinessDescription() {

        prepareRequestBeforeBusinessValidation();

        when(request.getBusinessName()).thenReturn("SmartCart Store");
        when(request.getBusinessType()).thenReturn("Retail");
        when(request.getBusinessAddress()).thenReturn("Singapore");
        when(request.getPostalCode()).thenReturn("123456");
        when(request.getContactNumber()).thenReturn("91234567");
        when(request.getProductCategory()).thenReturn("Fashion");
        when(request.getBusinessDescription()).thenReturn(" ");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Business description is required.",
                exception.getReason()
        );
    }

    // =========================================================
    // REGISTRATION DOCUMENT VALIDATION
    // =========================================================

    @Test
    void createMerchantProfile_shouldRejectMissingRegistrationDocument() {

        prepareRequestBeforeFileValidation();

        when(request.getLogo()).thenReturn(null);
        when(request.getRegistrationDocument()).thenReturn(null);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Business registration document is required.",
                exception.getReason()
        );

        verify(merchantProfileRepository, never())
                .save(any(MerchantProfile.class));
    }

    @Test
    void createMerchantProfile_shouldRejectEmptyRegistrationDocument() {

        prepareRequestBeforeFileValidation();

        MultipartFile document =
                mock(MultipartFile.class);

        when(request.getLogo()).thenReturn(null);
        when(request.getRegistrationDocument())
                .thenReturn(document);

        when(document.isEmpty()).thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Business registration document is required.",
                exception.getReason()
        );
    }

    @Test
    void createMerchantProfile_shouldRejectRegistrationDocumentLargerThan5Mb() {

        prepareRequestBeforeFileValidation();

        MultipartFile document =
                mock(MultipartFile.class);

        when(request.getLogo()).thenReturn(null);
        when(request.getRegistrationDocument())
                .thenReturn(document);

        when(document.isEmpty()).thenReturn(false);
        when(document.getSize())
                .thenReturn(5L * 1024L * 1024L + 1);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Business registration document must be smaller than 5 MB.",
                exception.getReason()
        );
    }

    @Test
    void createMerchantProfile_shouldRejectInvalidRegistrationDocumentType() {

        prepareRequestBeforeFileValidation();

        MultipartFile document =
                mock(MultipartFile.class);

        when(request.getLogo()).thenReturn(null);
        when(request.getRegistrationDocument())
                .thenReturn(document);

        when(document.isEmpty()).thenReturn(false);
        when(document.getSize()).thenReturn(1000L);
        when(document.getContentType())
                .thenReturn("text/plain");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.createMerchantProfile(request)
                );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        assertEquals(
                "Business registration document must be PDF, JPG or PNG.",
                exception.getReason()
        );
    }

    // =========================================================
    // SUCCESSFUL CREATION
    // =========================================================

    @Test
    void createMerchantProfile_shouldCreateProfileSuccessfully() {

        prepareRequestBeforeFileValidation();

        MultipartFile document =
                validRegistrationDocument();

        when(request.getLogo())
                .thenReturn(null);

        when(request.getRegistrationDocument())
                .thenReturn(document);

        when(request.getPickupAvailable())
                .thenReturn(true);

        when(merchantProfileRepository.save(
                any(MerchantProfile.class)
        )).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        MerchantProfile result =
                service.createMerchantProfile(request);

        assertNotNull(result);

        assertEquals(
                "SmartCart Store",
                result.getBusinessName()
        );

        assertEquals(
                "201912345A",
                result.getUen()
        );

        assertEquals(
                "Retail",
                result.getBusinessType()
        );

        assertEquals(
                "Singapore",
                result.getBusinessAddress()
        );

        assertEquals(
                "123456",
                result.getPostalCode()
        );

        assertEquals(
                "91234567",
                result.getContactNumber()
        );

        assertEquals(
                "Fashion",
                result.getProductCategory()
        );

        assertEquals(
                "Online fashion store",
                result.getBusinessDescription()
        );

        assertTrue(
                result.getPickupAvailable()
        );

        assertEquals(
                MerchantVerificationStatus.PENDING,
                result.getVerificationStatus()
        );

        verify(merchantProfileRepository)
                .save(any(MerchantProfile.class));
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private User merchantUser(Long userId) {

        User user = new User();

        user.setId(userId);
        user.setRole(UserRole.MERCHANT);

        return user;
    }

    private void prepareRequestBeforeBusinessValidation() {

        when(request.getUserId()).thenReturn(1L);

        User user = merchantUser(1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(merchantProfileRepository.existsByUserId(1L))
                .thenReturn(false);

        when(request.getUen())
                .thenReturn("201912345A");

        when(merchantProfileRepository.existsByUen("201912345A"))
                .thenReturn(false);
    }

    private void prepareRequestBeforeFileValidation() {

        prepareRequestBeforeBusinessValidation();

        when(request.getBusinessName())
                .thenReturn("SmartCart Store");

        when(request.getBusinessType())
                .thenReturn("Retail");

        when(request.getBusinessAddress())
                .thenReturn("Singapore");

        when(request.getPostalCode())
                .thenReturn("123456");

        when(request.getContactNumber())
                .thenReturn("91234567");

        when(request.getProductCategory())
                .thenReturn("Fashion");

        when(request.getBusinessDescription())
                .thenReturn("Online fashion store");
    }

    private MultipartFile validRegistrationDocument() {

        MultipartFile document =
                mock(MultipartFile.class);

        when(document.isEmpty()).thenReturn(false);
        when(document.getSize()).thenReturn(1000L);
        when(document.getContentType())
                .thenReturn("application/pdf");
        when(document.getOriginalFilename())
                .thenReturn("registration.pdf");

        try {
            when(document.getInputStream())
                    .thenReturn(
                            new java.io.ByteArrayInputStream(
                                    "test document".getBytes()
                            )
                    );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return document;
    }
}
