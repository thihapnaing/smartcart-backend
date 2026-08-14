package nus.iss.smartcart.backend.controller;

// AUTHOR: Htet Nandar(Grace)

import nus.iss.smartcart.backend.admin.service.AdminDashboardService;
import nus.iss.smartcart.backend.dto.PublicStatsDto;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CustomUserDetailsService;
import nus.iss.smartcart.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller-slice test for the public stats endpoint - just routing/serialization, the
 * number-crunching is covered by AdminDashboardServiceTest#getPublicStats* cases. */
@WebMvcTest(PublicStatsController.class)
class PublicStatsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminDashboardService adminDashboardService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void getStats_returnsOkWithPublicStats() throws Exception {
        PublicStatsDto stats = PublicStatsDto.builder()
                .activeListings(36)
                .activeMerchants(1)
                .totalRevenue(BigDecimal.valueOf(189))
                .build();

        when(adminDashboardService.getPublicStats()).thenReturn(stats);

        mockMvc.perform(get("/api/public/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeListings").value(36))
                .andExpect(jsonPath("$.activeMerchants").value(1))
                .andExpect(jsonPath("$.totalRevenue").value(189));
    }
}
