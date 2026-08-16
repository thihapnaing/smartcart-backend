package nus.iss.smartcart.backend.admin.controller;

// AUTHOR: Htet Nandar(Grace)

import nus.iss.smartcart.backend.admin.dto.AdminAccountDto;
import nus.iss.smartcart.backend.admin.service.AdminAccountService;
import nus.iss.smartcart.backend.exception.ForbiddenException;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CustomUserDetailsService;
import nus.iss.smartcart.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice tests for admin-invites-admin: request routing, status codes, and the
 * @NotBlank/@Email validation on CreateAdminRequest (a malformed body must 400 before the
 * service is ever reached), mirroring AdminMerchantControllerTest/AdminProductControllerTest.
 */
@WebMvcTest(AdminAccountController.class)
class AdminAccountControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminAccountService adminAccountService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private UserRepository userRepository;

    private static final String VALID_BODY =
            "{\"username\":\"newadmin\",\"email\":\"newadmin@smartcart.demo\"}";

    private AdminAccountDto sampleDto() {
        return AdminAccountDto.builder()
                .id(5L)
                .username("newadmin")
                .email("newadmin@smartcart.demo")
                .status("ACTIVE")
                .createdAt(LocalDateTime.of(2026, Month.AUGUST, 16, 10, 0))
                .temporaryPassword("123456")
                .build();
    }

    @Test
    void getAllAdmins_returnsOkWithAdminList() throws Exception {
        AdminAccountDto pending = AdminAccountDto.builder()
                .id(2L)
                .username("secondadmin")
                .email("secondadmin@smartcart.demo")
                .status("ACTIVE")
                .createdAt(LocalDateTime.of(2026, Month.AUGUST, 16, 10, 0))
                .mustChangePassword(true)
                .build();
        AdminAccountDto settled = AdminAccountDto.builder()
                .id(1L)
                .username("firstadmin")
                .email("firstadmin@smartcart.demo")
                .status("ACTIVE")
                .createdAt(LocalDateTime.of(2026, Month.JANUARY, 1, 9, 0))
                .mustChangePassword(false)
                .build();
        when(adminAccountService.getAllAdmins()).thenReturn(List.of(pending, settled));

        mockMvc.perform(get("/api/admin/admins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].mustChangePassword").value(true))
                .andExpect(jsonPath("$[0].temporaryPassword").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].mustChangePassword").value(false));
    }

    @Test
    void getAllAdmins_callerNotAnAdmin_returnsForbidden() throws Exception {
        when(adminAccountService.getAllAdmins())
                .thenThrow(new ForbiddenException("This action requires a ADMIN account."));

        mockMvc.perform(get("/api/admin/admins"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAdmin_validRequest_returnsCreatedWithNewAdminAndTemporaryPassword() throws Exception {
        when(adminAccountService.createAdmin(any())).thenReturn(sampleDto());

        mockMvc.perform(post("/api/admin/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("newadmin"))
                .andExpect(jsonPath("$.email").value("newadmin@smartcart.demo"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.temporaryPassword").value("123456"));
    }

    @Test
    void createAdmin_missingUsername_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newadmin@smartcart.demo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAdmin_malformedEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newadmin\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAdmin_emailAlreadyRegistered_returnsBadRequest() throws Exception {
        when(adminAccountService.createAdmin(any()))
                .thenThrow(new IllegalArgumentException("Email is already registered"));

        mockMvc.perform(post("/api/admin/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAdmin_callerNotAnAdmin_returnsForbidden() throws Exception {
        when(adminAccountService.createAdmin(any()))
                .thenThrow(new ForbiddenException("This action requires a ADMIN account."));

        mockMvc.perform(post("/api/admin/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }
}
