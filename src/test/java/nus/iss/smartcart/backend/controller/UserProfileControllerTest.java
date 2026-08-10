package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.UserProfileForDeliveryDetails;
import nus.iss.smartcart.backend.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    void getProfile_returnsOkWithProfileData() throws Exception {
        UserProfileForDeliveryDetails userProfileForDeliveryDetails = UserProfileForDeliveryDetails.builder()
                        .firstName("John")
                        .lastName("Tan")
                        .address("12 Rainbow Street")
                        .phoneNumber("91234567")
                        .build();
        when(userProfileService.getProfileForDeliveryDetails(2L)).thenReturn(userProfileForDeliveryDetails);
        mockMvc.perform(get("/api/user-profile"))
                .andExpect(status().isOk());
    }
}
