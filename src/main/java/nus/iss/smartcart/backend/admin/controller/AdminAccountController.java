package nus.iss.smartcart.backend.admin.controller;

// Author: Htet Nandar (Grace)

import jakarta.validation.Valid;
import nus.iss.smartcart.backend.admin.dto.AdminAccountDto;
import nus.iss.smartcart.backend.admin.dto.CreateAdminRequest;
import nus.iss.smartcart.backend.admin.service.AdminAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin-invites-admin: the only way an ADMIN account can be created after the first one is
 * seeded (see data.sql). There's no public "admin register" form - SecurityConfig gates every
 * /api/admin/** path (including this one) behind hasRole("ADMIN"), so only someone already
 * signed in as an admin can reach this endpoint. */
@RestController
@RequestMapping("/api/admin/admins")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    public ResponseEntity<List<AdminAccountDto>> getAllAdmins() {
        return ResponseEntity.ok(adminAccountService.getAllAdmins());
    }

    @PostMapping
    public ResponseEntity<AdminAccountDto> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        AdminAccountDto created = adminAccountService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
