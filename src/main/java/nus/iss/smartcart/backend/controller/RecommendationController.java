package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.RecommendationResultDTO;
import nus.iss.smartcart.backend.service.RecommendationOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@CrossOrigin(origins = "http://localhost:4200")
public class RecommendationController {

    private final RecommendationOrchestratorService recommendationService;

    public RecommendationController(RecommendationOrchestratorService recommendationService) {
        this.recommendationService = recommendationService;
    }

    // Update the ResponseEntity generic type
    @GetMapping("/{userId}")
    public ResponseEntity<RecommendationResultDTO> getRecommendations(@PathVariable Long userId) {
        return ResponseEntity.ok(recommendationService.getRecommendationsForUser(userId));
    }
}