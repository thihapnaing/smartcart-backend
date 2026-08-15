package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.CreateMerchantProfileRequest;
import nus.iss.smartcart.backend.model.*;
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

@Service
public class MerchantProfileService {

    private final MerchantProfileRepository merchantProfileRepository;
    private final UserRepository userRepository;


    private final Path uploadDirectory =
            Paths.get("upload", "merchant")
                    .toAbsolutePath()
                    .normalize();


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

        // -----------------------------------------------------
        // USER ID
        // -----------------------------------------------------

        if (request.getUserId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID is required."
            );
        }


        // -----------------------------------------------------
        // FIND USER
        // -----------------------------------------------------

        User user =
                userRepository.findById(
                        request.getUserId()
                ).orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found."
                        )
                );


        // -----------------------------------------------------
        // CHECK MERCHANT ROLE
        // -----------------------------------------------------

        if (user.getRole() != UserRole.MERCHANT) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only merchant accounts can create a merchant profile."
            );
        }


        // -----------------------------------------------------
        // CHECK EXISTING PROFILE
        // -----------------------------------------------------

        if (
                merchantProfileRepository
                        .existsByUserId(user.getId())
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Merchant profile already exists."
            );
        }


        // -----------------------------------------------------
        // BUSINESS NAME
        // -----------------------------------------------------

        if (
                request.getBusinessName() == null ||
                        request.getBusinessName().trim().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business name is required."
            );
        }


        // -----------------------------------------------------
        // UEN
        // -----------------------------------------------------

        if (
                request.getUen() == null ||
                        request.getUen().trim().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "UEN is required."
            );
        }


        String uen =
                request.getUen().trim();


        if (
                merchantProfileRepository
                        .existsByUen(uen)
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "UEN is already registered."
            );
        }


        // -----------------------------------------------------
        // BUSINESS TYPE
        // -----------------------------------------------------

        if (
                request.getBusinessType() == null ||
                        request.getBusinessType().trim().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business type is required."
            );
        }


        // -----------------------------------------------------
        // BUSINESS ADDRESS
        // -----------------------------------------------------

        if (
                request.getBusinessAddress() == null ||
                        request.getBusinessAddress().trim().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business address is required."
            );
        }


        // -----------------------------------------------------
        // POSTAL CODE
        // -----------------------------------------------------

        if (
                request.getPostalCode() == null ||
                        request.getPostalCode().trim().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Postal code is required."
            );
        }


        // -----------------------------------------------------
        // CONTACT NUMBER
        // -----------------------------------------------------

        if (
                request.getContactNumber() == null ||
                        request.getContactNumber().trim().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Contact number is required."
            );
        }


        // -----------------------------------------------------
        // PRODUCT CATEGORY
        // -----------------------------------------------------

        if (
                request.getProductCategory() == null ||
                        request.getProductCategory().trim().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product category is required."
            );
        }


        // -----------------------------------------------------
        // BUSINESS DESCRIPTION
        // -----------------------------------------------------

        if (
                request.getBusinessDescription() == null ||
                        request.getBusinessDescription().trim().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business description is required."
            );
        }


        // -----------------------------------------------------
        // REGISTRATION DOCUMENT
        // -----------------------------------------------------

        if (
                request.getRegistrationDocument() == null ||
                        request.getRegistrationDocument().isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business registration document is required."
            );
        }


        // -----------------------------------------------------
        // VALIDATE LOGO
        // -----------------------------------------------------

        MultipartFile logo =
                request.getLogo();

        validateLogo(logo);


        // -----------------------------------------------------
        // VALIDATE REGISTRATION DOCUMENT
        // -----------------------------------------------------

        MultipartFile document =
                request.getRegistrationDocument();

        validateRegistrationDocument(document);


        // -----------------------------------------------------
        // CREATE UPLOAD DIRECTORY
        // -----------------------------------------------------

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


        // -----------------------------------------------------
        // SAVE LOGO
        // -----------------------------------------------------

        String logoUrl = null;

        if (
                logo != null &&
                        !logo.isEmpty()
        ) {

            logoUrl =
                    saveFile(
                            logo,
                            "logo_" + user.getId()
                    );
        }


        // -----------------------------------------------------
        // SAVE BUSINESS DOCUMENT
        // -----------------------------------------------------

        String documentUrl =
                saveFile(
                        document,
                        "registration_" + user.getId()
                );


        // -----------------------------------------------------
        // CREATE PROFILE
        // -----------------------------------------------------

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


        // -----------------------------------------------------
        // SAVE DATABASE
        // -----------------------------------------------------

        return merchantProfileRepository.save(
                profile
        );
    }


    // =========================================================
    // LOGO VALIDATION
    // =========================================================

    private void validateLogo(
            MultipartFile file) {

        if (
                file == null ||
                        file.isEmpty()
        ) {

            // Logo is optional.
            return;
        }


        // 2 MB

        long maxSize =
                2L * 1024L * 1024L;


        if (
                file.getSize() > maxSize
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business logo must be smaller than 2 MB."
            );
        }


        String contentType =
                file.getContentType();


        if (
                contentType == null ||
                        !(
                                contentType.equals("image/jpeg") ||
                                        contentType.equals("image/png") ||
                                        contentType.equals("image/webp")
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business logo must be JPG, PNG or WEBP."
            );
        }
    }


    // =========================================================
    // DOCUMENT VALIDATION
    // =========================================================

    private void validateRegistrationDocument(
            MultipartFile file) {

        // 5 MB

        long maxSize =
                5L * 1024L * 1024L;


        if (
                file.getSize() > maxSize
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business registration document must be smaller than 5 MB."
            );
        }


        String contentType =
                file.getContentType();


        if (
                contentType == null ||
                        !(
                                contentType.equals("application/pdf") ||
                                        contentType.equals("image/jpeg") ||
                                        contentType.equals("image/png")
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Business registration document must be PDF, JPG or PNG."
            );
        }
    }


    // =========================================================
    // SAVE FILE
    // =========================================================

    private String saveFile(
            MultipartFile file,
            String prefix) {

        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();

        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename
                    .substring(originalFilename.lastIndexOf("."))
                    .toLowerCase();
        }

        String filename =
                prefix +
                        "_" +
                        System.currentTimeMillis() +
                        extension;

        Path target = uploadDirectory
                .resolve(filename)
                .normalize();

        // Prevent path traversal
        if (!target.getParent().equals(uploadDirectory)) {
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