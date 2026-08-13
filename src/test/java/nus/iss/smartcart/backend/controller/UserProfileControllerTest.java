package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.UserProfileForDeliveryDetails;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import nus.iss.smartcart.backend.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import nus.iss.smartcart.backend.security.JwtService;
import nus.iss.smartcart.backend.security.CustomUserDetailsService;
import nus.iss.smartcart.backend.repository.UserRepository;

@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean private UserProfileService userProfileService;
    @MockitoBean private CurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUpCurrentCustomer() {
        User fakeCustomer = mock(User.class);
        when(fakeCustomer.getId()).thenReturn(2L);
        when(currentUserProvider.getCurrentCustomer()).thenReturn(fakeCustomer);
    }

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
