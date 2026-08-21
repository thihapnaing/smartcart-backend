package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.CreateMerchantProfileRequest;
import nus.iss.smartcart.backend.model.MerchantProfile;
import nus.iss.smartcart.backend.model.MerchantVerificationStatus;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.repository.MerchantProfileRepository;
import nus.iss.smartcart.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

//Author: Junior

@Service
public class MerchantProfileService {

    private final MerchantProfileRepository merchantProfileRepository;

    private final UserRepository userRepository;

    private final Path uploadDirectory =
            Paths.get("upload", "merchant")
                    .toAbsolutePath()
                    .normalize();

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public MerchantProfileService(
            MerchantProfileRepository merchantProfileRepository,
            UserRepository userRepository) {

        this.merchantProfileRepository =
                merchantProfileRepository;

        this.userRepository =
                userRepository;
    }

    // =========================================================
    // CREATE MERCHANT PROFILE
    // =========================================================

    public MerchantProfile createMerchantProfile(
            CreateMerchantProfileRequest request) {

        validateRequest(request);

        User user = findUser(request.getUserId());

        validateMerchantRole(user);

        validateExistingProfile(user);

        String uen = validateUen(request);

        validateBusinessInformation(request);

        MultipartFile logo = request.getLogo();

        MultipartFile registrationDocument =
                request.getRegistrationDocument();

        validateLogo(logo);

        validateRegistrationDocument(
                registrationDocument
        );

        createUploadDirectory();

        String logoUrl = saveLogo(
                logo,
                user.getId()
        );

        String documentUrl = saveRegistrationDocument(
                registrationDocument,
                user.getId()
        );

        MerchantProfile profile =
                buildMerchantProfile(
                        request,
                        user,
                        uen,
                        logoUrl,
                        documentUrl
                );

        return merchantProfileRepository.save(
                profile
        );
    }

    // =========================================================
    // REQUEST VALIDATION
    // =========================================================

    private void validateRequest(
            CreateMerchantProfileRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request is required."
            );
        }

        if (request.getUserId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID is required."
            );
        }
    }

    // =========================================================
    // FIND USER
    // =========================================================

    private User findUser(Long userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found."
                        )
                );
    }

    // =========================================================
    // CHECK MERCHANT ROLE
    // =========================================================

    private void validateMerchantRole(
            User user) {

        if (user.getRole() != UserRole.MERCHANT) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only merchant accounts can create a merchant profile."
            );
        }
    }

    // =========================================================
    // CHECK EXISTING PROFILE
    // =========================================================

    private void validateExistingProfile(
            User user) {

        if (merchantProfileRepository
                .existsByUserId(user.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Merchant profile already exists."
            );
        }
    }

    // =========================================================
    // UEN VALIDATION
    // =========================================================

    private String validateUen(
            CreateMerchantProfileRequest request) {

        validateRequiredField(
                request.getUen(),
                "UEN"
        );

        String uen =
                request.getUen().trim();

        if (merchantProfileRepository
                .existsByUen(uen)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "UEN is already registered."
            );
        }

        return uen;
    }

    // =========================================================
    // BUSINESS INFORMATION VALIDATION
    // =========================================================

    private void validateBusinessInformation(
            CreateMerchantProfileRequest request) {

        validateRequiredField(
                request.getBusinessName(),
                "Business name"
        );

        validateRequiredField(
                request.getBusinessType(),
                "Business type"
        );

        validateRequiredField(
                request.getBusinessAddress(),
                "Business address"
        );

        validateRequiredField(
                request.getPostalCode(),
                "Postal code"
        );

        validateRequiredField(
                request.getContactNumber(),
                "Contact number"
        );

        validateRequiredField(
                request.getProductCategory(),
                "Product category"
        );

        validateRequiredField(
                request.getBusinessDescription(),
                "Business description"
        );
    }

    // =========================================================
    // REQUIRED FIELD VALIDATION
    // =========================================================

    private void validateRequiredField(
            String value,
            String fieldName) {

        if (value == null ||
                value.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " is required."
            );
        }
    }

    // =========================================================
    // CREATE UPLOAD DIRECTORY
    // =========================================================

    private void createUploadDirectory() {

        try {

            Files.createDirectories(
                    uploadDirectory
            );

        } catch (IOException e) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create upload directory."
            );
        }
    }

    // =========================================================
    // LOGO VALIDATION
    // =========================================================

    private void validateLogo(
            MultipartFile file) {

        // Logo is optional.
        if (file == null ||
                file.isEmpty()) {

            return;
        }

        // 2 MB
        long maxSize =
                2L * 1024L * 1024L;

        if (file.getSize() > maxSize) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business logo must be smaller than 2 MB."
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null ||
                !isAllowedLogoType(contentType)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business logo must be JPG, PNG or WEBP."
            );
        }
    }

    // =========================================================
    // LOGO FILE TYPE
    // =========================================================

    private boolean isAllowedLogoType(
            String contentType) {

        return contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/webp");
    }

    // =========================================================
    // REGISTRATION DOCUMENT VALIDATION
    // =========================================================

    private void validateRegistrationDocument(
            MultipartFile file) {

        if (file == null ||
                file.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business registration document is required."
            );
        }

        // 5 MB
        long maxSize =
                5L * 1024L * 1024L;

        if (file.getSize() > maxSize) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business registration document must be smaller than 5 MB."
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null ||
                !isAllowedDocumentType(contentType)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business registration document must be PDF, JPG or PNG."
            );
        }
    }

    // =========================================================
    // DOCUMENT FILE TYPE
    // =========================================================

    private boolean isAllowedDocumentType(
            String contentType) {

        return contentType.equals("application/pdf") ||
                contentType.equals("image/jpeg") ||
                contentType.equals("image/png");
    }

    // =========================================================
    // SAVE LOGO
    // =========================================================

    private String saveLogo(
            MultipartFile logo,
            Long userId) {

        if (logo == null ||
                logo.isEmpty()) {

            return null;
        }

        return saveFile(
                logo,
                "logo_" + userId
        );
    }

    // =========================================================
    // SAVE REGISTRATION DOCUMENT
    // =========================================================

    private String saveRegistrationDocument(
            MultipartFile document,
            Long userId) {

        return saveFile(
                document,
                "registration_" + userId
        );
    }

    // =========================================================
    // BUILD MERCHANT PROFILE
    // =========================================================

    private MerchantProfile buildMerchantProfile(
            CreateMerchantProfileRequest request,
            User user,
            String uen,
            String logoUrl,
            String documentUrl) {

        MerchantProfile profile =
                new MerchantProfile();

        profile.setUser(user);

        profile.setBusinessName(
                request.getBusinessName().trim()
        );

        profile.setUen(uen);

        profile.setBusinessType(
                request.getBusinessType().trim()
        );

        profile.setBusinessAddress(
                request.getBusinessAddress().trim()
        );

        profile.setPostalCode(
                request.getPostalCode().trim()
        );

        profile.setContactNumber(
                request.getContactNumber().trim()
        );

        profile.setProductCategory(
                request.getProductCategory().trim()
        );

        profile.setBusinessDescription(
                request.getBusinessDescription().trim()
        );

        profile.setLogoUrl(logoUrl);

        profile.setRegistrationDocumentUrl(
                documentUrl
        );

        profile.setPickupAvailable(
                Boolean.TRUE.equals(
                        request.getPickupAvailable()
                )
        );

        profile.setVerificationStatus(
                MerchantVerificationStatus.PENDING
        );

        return profile;
    }

    // =========================================================
    // SAVE FILE
    // =========================================================

    private String saveFile(
            MultipartFile file,
            String prefix) {

        if (file == null ||
                file.isEmpty()) {

            return null;
        }

        String originalFilename =
                file.getOriginalFilename();

        String extension = "";

        if (originalFilename != null &&
                originalFilename.contains(".")) {

            extension =
                    originalFilename
                            .substring(
                                    originalFilename
                                            .lastIndexOf(".")
                            )
                            .toLowerCase();
        }

        String filename =
                prefix +
                        "_" +
                        System.currentTimeMillis() +
                        extension;

        Path target =
                uploadDirectory
                        .resolve(filename)
                        .normalize();

        // Prevent path traversal.
        if (!target.getParent()
                .equals(uploadDirectory)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file name."
            );
        }

        try {

            Files.copy(
                    file.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to save uploaded file."
            );
        }

        return "upload/merchant/" + filename;
    }
}