package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.CreateMerchantProfileRequest;
import nus.iss.smartcart.backend.model.MerchantProfile;
import nus.iss.smartcart.backend.service.MerchantProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/merchant")
@CrossOrigin(origins = "http://localhost:4200")
public class MerchantProfileController {

    private final MerchantProfileService merchantProfileService;

    public MerchantProfileController(
            MerchantProfileService merchantProfileService) {

        this.merchantProfileService =
                merchantProfileService;
    }


    // =========================================================
    // CREATE MERCHANT PROFILE
    // =========================================================

    @PostMapping(
            value = "/profile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<MerchantProfile> createMerchantProfile(

            @RequestParam("userId")
            Long userId,

            @RequestParam("businessName")
            String businessName,

            @RequestParam("uen")
            String uen,

            @RequestParam("businessType")
            String businessType,

            @RequestParam("businessAddress")
            String businessAddress,

            @RequestParam("postalCode")
            String postalCode,

            @RequestParam("contactNumber")
            String contactNumber,

            @RequestParam("productCategory")
            String productCategory,

            @RequestParam("businessDescription")
            String businessDescription,

            @RequestParam("pickupAvailable")
            Boolean pickupAvailable,

            @RequestPart(
                    value = "logo",
                    required = false
            )
            MultipartFile logo,

            @RequestPart(
                    value = "registrationDocument",
                    required = true
            )
            MultipartFile registrationDocument
    ) {

        CreateMerchantProfileRequest request =
                new CreateMerchantProfileRequest();

        request.setUserId(userId);

        request.setBusinessName(
                businessName
        );

        request.setUen(
                uen
        );

        request.setBusinessType(
                businessType
        );

        request.setBusinessAddress(
                businessAddress
        );

        request.setPostalCode(
                postalCode
        );

        request.setContactNumber(
                contactNumber
        );

        request.setProductCategory(
                productCategory
        );

        request.setBusinessDescription(
                businessDescription
        );

        request.setPickupAvailable(
                pickupAvailable
        );

        request.setLogo(
                logo
        );

        request.setRegistrationDocument(
                registrationDocument
        );


        MerchantProfile profile =
                merchantProfileService
                        .createMerchantProfile(
                                request
                        );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(profile);
    }
}