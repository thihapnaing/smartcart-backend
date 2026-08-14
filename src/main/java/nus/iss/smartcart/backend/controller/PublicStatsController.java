package nus.iss.smartcart.backend.controller;

// AUTHOR: Htet Nandar(Grace)

import nus.iss.smartcart.backend.admin.service.AdminDashboardService;
import nus.iss.smartcart.backend.dto.PublicStatsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, unauthenticated aggregate numbers (active listings, merchant count, total revenue)
 * for the /admin/login screen - see SecurityConfig's permitAll() for /api/public/**. Deliberately
 * limited to these 3 non-sensitive counts; anything more detailed belongs behind
 * /api/admin/dashboard/stats (AdminDashboardController), which requires an authenticated admin. */
@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "http://localhost:4200")
public class PublicStatsController {

    private final AdminDashboardService adminDashboardService;

    public PublicStatsController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<PublicStatsDto> getStats() {
        return ResponseEntity.ok(adminDashboardService.getPublicStats());
    }
}
