package nus.iss.smartcart.backend.controller;

// Author: Junior

import nus.iss.smartcart.backend.dto.CreateMerchantProfileRequest;
import nus.iss.smartcart.backend.model.MerchantProfile;
import nus.iss.smartcart.backend.service.MerchantProfileService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class MerchantProfileControllerTest {

    @Mock
    private MerchantProfileService merchantProfileService;

    @InjectMocks
    private MerchantProfileController merchantProfileController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(
                                merchantProfileController
                        )
                        .build();
    }

    // SUCCESS
    @Test
    void createMerchantProfile_success()
            throws Exception {

        MockMultipartFile logo =
                new MockMultipartFile(
                        "logo",
                        "logo.png",
                        "image/png",
                        "fake-logo-data"
                                .getBytes()
                );


      // REGISTRATION DOCUMENT
        MockMultipartFile registrationDocument =
                new MockMultipartFile(
                        "registrationDocument",
                        "business-document.pdf",
                        "application/pdf",
                        "fake-pdf-data"
                                .getBytes()
                );

        //RESPONSE
        MerchantProfile profile =
                new MerchantProfile();


        when(
                merchantProfileService
                        .createMerchantProfile(
                                any(
                                        CreateMerchantProfileRequest.class
                                )
                        )
        ).thenReturn(profile);

        mockMvc.perform(
                        multipart(
                                "/api/merchant/profile"
                        )

                                .file(logo)

                                .file(
                                        registrationDocument
                                )

                                .param(
                                        "userId",
                                        "1"
                                )

                                .param(
                                        "businessName",
                                        "SmartCart Fashion"
                                )

                                .param(
                                        "uen",
                                        "202612345A"
                                )

                                .param(
                                        "businessType",
                                        "Retail"
                                )

                                .param(
                                        "businessAddress",
                                        "10 Orchard Road"
                                )

                                .param(
                                        "postalCode",
                                        "238840"
                                )

                                .param(
                                        "contactNumber",
                                        "91234567"
                                )

                                .param(
                                        "productCategory",
                                        "Fashion"
                                )

                                .param(
                                        "businessDescription",
                                        "Fashion products"
                                )

                                .param(
                                        "pickupAvailable",
                                        "true"
                                )
                )

                .andExpect(
                        status().isCreated()
                );


       verify(
                merchantProfileService
        ).createMerchantProfile(
                any(
                        CreateMerchantProfileRequest.class
                )
        );
    }

    //WITHOUT LOGO
    @Test
    void createMerchantProfile_withoutLogo()
            throws Exception {

        // -----------------------------------------------------
        // Required registration document
        // -----------------------------------------------------

        MockMultipartFile registrationDocument =
                new MockMultipartFile(
                        "registrationDocument",
                        "business-document.pdf",
                        "application/pdf",
                        "fake-pdf-data"
                                .getBytes()
                );

        MerchantProfile profile =
                new MerchantProfile();


        when(
                merchantProfileService
                        .createMerchantProfile(
                                any(
                                        CreateMerchantProfileRequest.class
                                )
                        )
        ).thenReturn(profile);

        mockMvc.perform(
                        multipart(
                                "/api/merchant/profile"
                        )

                                .file(
                                        registrationDocument
                                )

                                .param(
                                        "userId",
                                        "1"
                                )

                                .param(
                                        "businessName",
                                        "SmartCart Fashion"
                                )

                                .param(
                                        "uen",
                                        "202612345A"
                                )

                                .param(
                                        "businessType",
                                        "Retail"
                                )

                                .param(
                                        "businessAddress",
                                        "10 Orchard Road"
                                )

                                .param(
                                        "postalCode",
                                        "238840"
                                )

                                .param(
                                        "contactNumber",
                                        "91234567"
                                )

                                .param(
                                        "productCategory",
                                        "Fashion"
                                )

                                .param(
                                        "businessDescription",
                                        "Fashion products"
                                )

                                .param(
                                        "pickupAvailable",
                                        "false"
                                )
                )

                .andExpect(
                        status().isCreated()
                );

        verify(
                merchantProfileService
        ).createMerchantProfile(
                any(
                        CreateMerchantProfileRequest.class
                )
        );
    }


    //REGISTRATION DOCUMENT CHECKED
    @Test
    void createMerchantProfile_missingRegistrationDocument()
            throws Exception {

        mockMvc.perform(
                        multipart(
                                "/api/merchant/profile"
                        )

                                .param(
                                        "userId",
                                        "1"
                                )

                                .param(
                                        "businessName",
                                        "SmartCart Fashion"
                                )

                                .param(
                                        "uen",
                                        "202612345A"
                                )

                                .param(
                                        "businessType",
                                        "Retail"
                                )

                                .param(
                                        "businessAddress",
                                        "10 Orchard Road"
                                )

                                .param(
                                        "postalCode",
                                        "238840"
                                )

                                .param(
                                        "contactNumber",
                                        "91234567"
                                )

                                .param(
                                        "productCategory",
                                        "Fashion"
                                )

                                .param(
                                        "businessDescription",
                                        "Fashion products"
                                )

                                .param(
                                        "pickupAvailable",
                                        "true"
                                )
                )

                .andExpect(
                        status().isBadRequest()
                );
    }

    //MISSING BUSINESS NAME
    @Test
    void createMerchantProfile_missingBusinessName()
            throws Exception {

        MockMultipartFile registrationDocument =
                createRegistrationDocument();


        mockMvc.perform(
                        multipart(
                                "/api/merchant/profile"
                        )

                                .file(
                                        registrationDocument
                                )

                                .param(
                                        "userId",
                                        "1"
                                )

                                // businessName intentionally missing

                                .param(
                                        "uen",
                                        "202612345A"
                                )

                                .param(
                                        "businessType",
                                        "Retail"
                                )

                                .param(
                                        "businessAddress",
                                        "10 Orchard Road"
                                )

                                .param(
                                        "postalCode",
                                        "238840"
                                )

                                .param(
                                        "contactNumber",
                                        "91234567"
                                )

                                .param(
                                        "productCategory",
                                        "Fashion"
                                )

                                .param(
                                        "businessDescription",
                                        "Fashion products"
                                )

                                .param(
                                        "pickupAvailable",
                                        "true"
                                )
                )

                .andExpect(
                        status().isBadRequest()
                );
    }


    //MISSING USERID
    @Test
    void createMerchantProfile_missingUserId()
            throws Exception {

        MockMultipartFile registrationDocument =
                createRegistrationDocument();


        mockMvc.perform(
                        multipart(
                                "/api/merchant/profile"
                        )

                                .file(
                                        registrationDocument
                                )

                                // userId intentionally missing

                                .param(
                                        "businessName",
                                        "SmartCart Fashion"
                                )

                                .param(
                                        "uen",
                                        "202612345A"
                                )

                                .param(
                                        "businessType",
                                        "Retail"
                                )

                                .param(
                                        "businessAddress",
                                        "10 Orchard Road"
                                )

                                .param(
                                        "postalCode",
                                        "238840"
                                )

                                .param(
                                        "contactNumber",
                                        "91234567"
                                )

                                .param(
                                        "productCategory",
                                        "Fashion"
                                )

                                .param(
                                        "businessDescription",
                                        "Fashion products"
                                )

                                .param(
                                        "pickupAvailable",
                                        "true"
                                )
                )

                .andExpect(
                        status().isBadRequest()
                );
    }

    //MISSING UEN
    @Test
    void createMerchantProfile_missingUen()
            throws Exception {

        MockMultipartFile registrationDocument =
                createRegistrationDocument();


        mockMvc.perform(
                        multipart(
                                "/api/merchant/profile"
                        )

                                .file(
                                        registrationDocument
                                )

                                .param(
                                        "userId",
                                        "1"
                                )

                                .param(
                                        "businessName",
                                        "SmartCart Fashion"
                                )

                                // UEN intentionally missing

                                .param(
                                        "businessType",
                                        "Retail"
                                )

                                .param(
                                        "businessAddress",
                                        "10 Orchard Road"
                                )

                                .param(
                                        "postalCode",
                                        "238840"
                                )

                                .param(
                                        "contactNumber",
                                        "91234567"
                                )

                                .param(
                                        "productCategory",
                                        "Fashion"
                                )

                                .param(
                                        "businessDescription",
                                        "Fashion products"
                                )

                                .param(
                                        "pickupAvailable",
                                        "true"
                                )
                )

                .andExpect(
                        status().isBadRequest()
                );
    }

    // UNABLE TO CREATE PROFILE CHECKED
    @Test
    void createMerchantProfile_serviceException()
    {

        MockMultipartFile registrationDocument =
                createRegistrationDocument();


        when(
                merchantProfileService
                        .createMerchantProfile(
                                any(
                                        CreateMerchantProfileRequest.class
                                )
                        )
        ).thenThrow(
                new RuntimeException(
                        "Unable to create merchant profile"
                )
        );


        Exception exception =
                org.junit.jupiter.api.Assertions
                        .assertThrows(
                                Exception.class,
                                () ->
                                        mockMvc.perform(
                                                multipart(
                                                        "/api/merchant/profile"
                                                )

                                                        .file(
                                                                registrationDocument
                                                        )

                                                        .param(
                                                                "userId",
                                                                "1"
                                                        )

                                                        .param(
                                                                "businessName",
                                                                "SmartCart Fashion"
                                                        )

                                                        .param(
                                                                "uen",
                                                                "202612345A"
                                                        )

                                                        .param(
                                                                "businessType",
                                                                "Retail"
                                                        )

                                                        .param(
                                                                "businessAddress",
                                                                "10 Orchard Road"
                                                        )

                                                        .param(
                                                                "postalCode",
                                                                "238840"
                                                        )

                                                        .param(
                                                                "contactNumber",
                                                                "91234567"
                                                        )

                                                        .param(
                                                                "productCategory",
                                                                "Fashion"
                                                        )

                                                        .param(
                                                                "businessDescription",
                                                                "Fashion products"
                                                        )

                                                        .param(
                                                                "pickupAvailable",
                                                                "true"
                                                        )
                                        )
                        );


        assertEquals(
                "Unable to create merchant profile",
                exception
                        .getCause()
                        .getMessage()
        );
    }

    private MockMultipartFile
    createRegistrationDocument() {

        return new MockMultipartFile(
                "registrationDocument",
                "business-document.pdf",
                "application/pdf",
                "fake-pdf-data"
                        .getBytes()
        );
    }
}