package nus.iss.smartcart.backend.admin.controller;

// AUTHOR: Htet Nandar(Grace)

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.admin.dto.AdminMerchantDetailDto;
import nus.iss.smartcart.backend.admin.dto.AdminMerchantSummaryDto;
import nus.iss.smartcart.backend.admin.service.AdminMerchantService;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CustomUserDetailsService;
import nus.iss.smartcart.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests for the merchant management endpoints - request routing, status
 * codes, and the @NotNull validation on UpdateMerchantStatusRequest (missing/absent status
 * must 400 before the service is ever reached), mirroring AdminProductControllerTest.
 */
@WebMvcTest(AdminMerchantController.class)
class AdminMerchantControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminMerchantService adminMerchantService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private UserRepository userRepository;

    private AdminMerchantSummaryDto sampleDto(Long id, String status) {
        return AdminMerchantSummaryDto.builder()
                .id(id)
                .username("acme")
                .email("acme@smartcart.demo")
                .status(status)
                .createdAt(LocalDateTime.of(2026, Month.AUGUST, 1, 9, 0))
                .listingCount(3)
                .build();
    }

    private AdminMerchantDetailDto sampleDetailDto(Long id, String status) {
        return AdminMerchantDetailDto.builder()
                .id(id)
                .username("acme")
                .email("acme@smartcart.demo")
                .status(status)
                .createdAt(LocalDateTime.of(2026, Month.AUGUST, 1, 9, 0))
                .listingCount(3)
                .orderCount(7)
                .revenue(new BigDecimal("199.50"))
                .build();
    }

    @Test
    void getAllMerchants_returnsOkWithMerchantList() throws Exception {
        when(adminMerchantService.getAllMerchants())
                .thenReturn(List.of(sampleDto(1L, "SUSPENDED"), sampleDto(2L, "ACTIVE")));

        mockMvc.perform(get("/api/admin/merchants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("SUSPENDED"))
                .andExpect(jsonPath("$[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].listingCount").value(3));
    }

    @Test
    void updateMerchantStatus_validRequest_returnsOkWithUpdatedMerchant() throws Exception {
        when(adminMerchantService.updateMerchantStatus(1L, UserStatus.ACTIVE))
                .thenReturn(sampleDto(1L, "ACTIVE"));

        mockMvc.perform(patch("/api/admin/merchants/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void updateMerchantStatus_missingStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/merchants/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMerchantStatus_invalidStatusValue_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/merchants/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_REAL_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMerchantDetail_returnsOkWithOrdersAndRevenue() throws Exception {
        when(adminMerchantService.getMerchantDetail(1L)).thenReturn(sampleDetailDto(1L, "ACTIVE"));

        mockMvc.perform(get("/api/admin/merchants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.listingCount").value(3))
                .andExpect(jsonPath("$.orderCount").value(7))
                .andExpect(jsonPath("$.revenue").value(199.50));
    }

    @Test
    void updateMerchantStatus_validRequest_responseExposesWhoMadeTheChange() throws Exception {
        AdminMerchantSummaryDto dto = AdminMerchantSummaryDto.builder()
                .id(1L)
                .username("acme")
                .email("acme@smartcart.demo")
                .status("SUSPENDED")
                .createdAt(LocalDateTime.of(2026, Month.AUGUST, 1, 9, 0))
                .listingCount(3)
                .lastModifiedByAdminUsername("grace_admin")
                .lastModifiedAt(LocalDateTime.now())
                .build();
        when(adminMerchantService.updateMerchantStatus(1L, UserStatus.SUSPENDED)).thenReturn(dto);

        mockMvc.perform(patch("/api/admin/merchants/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastModifiedByAdminUsername").value("grace_admin"));
    }

    @Test
    void getMerchantDetail_responseExposesWhoMadeTheChange() throws Exception {
        AdminMerchantDetailDto dto = AdminMerchantDetailDto.builder()
                .id(1L)
                .username("acme")
                .email("acme@smartcart.demo")
                .status("SUSPENDED")
                .createdAt(LocalDateTime.of(2026, Month.AUGUST, 1, 9, 0))
                .listingCount(3)
                .orderCount(7)
                .revenue(new BigDecimal("199.50"))
                .lastModifiedByAdminUsername("grace_admin")
                .lastModifiedAt(LocalDateTime.now())
                .build();
        when(adminMerchantService.getMerchantDetail(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/admin/merchants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastModifiedByAdminUsername").value("grace_admin"));
    }

    @Test
    void getMerchantDetail_notFound_returns404() throws Exception {
        when(adminMerchantService.getMerchantDetail(99L))
                .thenThrow(new EntityNotFoundException("Merchant not found: 99"));

        mockMvc.perform(get("/api/admin/merchants/99"))
                .andExpect(status().isNotFound());
    }
}
