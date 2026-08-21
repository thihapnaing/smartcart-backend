package nus.iss.smartcart.backend.admin.controller;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.admin.dto.AdminDashboardStatsDto;
import nus.iss.smartcart.backend.admin.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Overview stats for the admin dashboard landing page (revenue, listing counts, merchant
 * count, category/gender breakdowns, recent listings). */
@RestController
@RequestMapping("/api/admin/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsDto> getStats() {
        return ResponseEntity.ok(adminDashboardService.getStats());
    }
}
