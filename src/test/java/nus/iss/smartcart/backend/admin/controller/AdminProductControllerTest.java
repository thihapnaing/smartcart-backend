package nus.iss.smartcart.backend.admin.controller;

// AUTHOR: Htet Nandar(Grace)

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.admin.dto.AdminProductSummaryDto;
import nus.iss.smartcart.backend.admin.service.AdminProductService;
import nus.iss.smartcart.backend.model.ProductStatus;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests for the moderation endpoints - request routing, status codes, and
 * the @NotNull validation on UpdateProductStatusRequest (missing/absent status must 400 before
 * the service is ever reached).
 */
@WebMvcTest(AdminProductController.class)
class AdminProductControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminProductService adminProductService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private UserRepository userRepository;

    private AdminProductSummaryDto sampleDto(Long id, String status) {
        return AdminProductSummaryDto.builder()
                .id(id)
                .name("Classic Crew Tee")
                .price(BigDecimal.valueOf(19.9))
                .imageUrl("/assets/products/tee-crew.jpg")
                .categoryName("Tops")
                .shopName("SmartCart Official")
                .gender("MEN")
                .status(status)
                .build();
    }

    @Test
    void getAllProducts_returnsOkWithProductList() throws Exception {
        when(adminProductService.getAllProducts())
                .thenReturn(List.of(sampleDto(1L, "ACTIVE"), sampleDto(2L, "INACTIVE")));

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].status").value("INACTIVE"));
    }

    @Test
    void updateProductStatus_validRequest_returnsOkWithUpdatedProduct() throws Exception {
        when(adminProductService.updateProductStatus(1L, ProductStatus.INACTIVE))
                .thenReturn(sampleDto(1L, "INACTIVE"));

        mockMvc.perform(patch("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void updateProductStatus_validRequest_responseExposesWhoMadeTheChange() throws Exception {
        AdminProductSummaryDto dto = AdminProductSummaryDto.builder()
                .id(1L)
                .name("Classic Crew Tee")
                .price(BigDecimal.valueOf(19.9))
                .imageUrl("/assets/products/tee-crew.jpg")
                .categoryName("Tops")
                .shopName("SmartCart Official")
                .gender("MEN")
                .status("INACTIVE")
                .lastModifiedByAdminUsername("grace_admin")
                .lastModifiedAt(LocalDateTime.now())
                .build();
        when(adminProductService.updateProductStatus(1L, ProductStatus.INACTIVE)).thenReturn(dto);

        mockMvc.perform(patch("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastModifiedByAdminUsername").value("grace_admin"));
    }

    @Test
    void updateProductStatus_missingStatus_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProductStatus_invalidStatusValue_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_REAL_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProductStatus_notFound_returns404() throws Exception {
        when(adminProductService.updateProductStatus(99L, ProductStatus.INACTIVE))
                .thenThrow(new EntityNotFoundException("Product not found: 99"));

        mockMvc.perform(patch("/api/admin/products/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isNotFound());
    }
}
