package nus.iss.smartcart.backend.admin.controller;



// Author: Htet Nandar (Grace)

import jakarta.validation.Valid;
import nus.iss.smartcart.backend.admin.dto.AdminProductSummaryDto;
import nus.iss.smartcart.backend.admin.dto.UpdateProductStatusRequest;
import nus.iss.smartcart.backend.admin.service.AdminProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** AD-89: "As an admin, I want to moderate product listings so that inappropriate products
 * are removed." Lists every product regardless of status, and lets an admin flip a listing's
 * status (e.g. ACTIVE -> INACTIVE to remove it from customer-facing search/browse). */
@RestController
@RequestMapping("/api/admin/products")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @GetMapping
    public ResponseEntity<List<AdminProductSummaryDto>> getAllProducts() {
        return ResponseEntity.ok(adminProductService.getAllProducts());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminProductSummaryDto> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        return ResponseEntity.ok(adminProductService.updateProductStatus(id, request.getStatus()));
    }
}
