package nus.iss.smartcart.backend.admin.controller;

// Author: Htet Nandar (Grace)

import jakarta.validation.Valid;
import nus.iss.smartcart.backend.admin.dto.AdminMerchantDetailDto;
import nus.iss.smartcart.backend.admin.dto.AdminMerchantSummaryDto;
import nus.iss.smartcart.backend.admin.dto.UpdateMerchantStatusRequest;
import nus.iss.smartcart.backend.admin.service.AdminMerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** "As an admin, I want to manage merchant accounts so that I can suspend accounts that
 * violate policy and reinstate them later." Lists every merchant account regardless of
 * status, and lets an admin move one through its lifecycle (see AdminMerchantService for the
 * allowed ACTIVE/SUSPENDED/INACTIVE transitions). */
@RestController
@RequestMapping("/api/admin/merchants")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminMerchantController {

    private final AdminMerchantService adminMerchantService;

    public AdminMerchantController(AdminMerchantService adminMerchantService) {
        this.adminMerchantService = adminMerchantService;
    }

    @GetMapping
    public ResponseEntity<List<AdminMerchantSummaryDto>> getAllMerchants() {
        return ResponseEntity.ok(adminMerchantService.getAllMerchants());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminMerchantDetailDto> getMerchantDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminMerchantService.getMerchantDetail(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminMerchantSummaryDto> updateMerchantStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMerchantStatusRequest request) {
        return ResponseEntity.ok(adminMerchantService.updateMerchantStatus(id, request.getStatus()));
    }
}
