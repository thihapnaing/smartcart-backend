package nus.iss.smartcart.backend.admin.controller;

// AUTHOR: Htet Nandar(Grace)

import nus.iss.smartcart.backend.admin.dto.AdminDashboardStatsDto;
import nus.iss.smartcart.backend.admin.dto.CategoryCountDto;
import nus.iss.smartcart.backend.admin.dto.GenderCountDto;
import nus.iss.smartcart.backend.admin.service.AdminDashboardService;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CustomUserDetailsService;
import nus.iss.smartcart.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller-slice test for the dashboard stats endpoint - just routing/serialization, the
 * actual number-crunching is covered by AdminDashboardServiceTest. */
@WebMvcTest(AdminDashboardController.class)
class AdminDashboardControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminDashboardService adminDashboardService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void getStats_returnsOkWithDashboardStats() throws Exception {
        AdminDashboardStatsDto stats = AdminDashboardStatsDto.builder()
                .totalRevenue(BigDecimal.valueOf(189))
                .activeListings(36)
                .inactiveListings(4)
                .newListingsThisWeek(4)
                .activeMerchants(1)
                .categoryBreakdown(List.of(new CategoryCountDto("Shoes", 14)))
                .genderSplit(List.of(new GenderCountDto("MEN", 23, 57.5)))
                .recentListings(List.of())
                .build();

        when(adminDashboardService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeListings").value(36))
                .andExpect(jsonPath("$.inactiveListings").value(4))
                .andExpect(jsonPath("$.activeMerchants").value(1))
                .andExpect(jsonPath("$.categoryBreakdown[0].categoryName").value("Shoes"))
                .andExpect(jsonPath("$.genderSplit[0].gender").value("MEN"));
    }
}
