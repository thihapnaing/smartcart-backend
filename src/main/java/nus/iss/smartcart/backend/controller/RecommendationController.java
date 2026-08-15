package nus.iss.smartcart.backend.controller;

//Author: Cecil

import nus.iss.smartcart.backend.dto.RecommendationResultDTO;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.service.RecommendationOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;


@RestController
@RequestMapping("/api/v1/recommendations")
@CrossOrigin(origins = "http://localhost:4200")
public class RecommendationController {

    private final RecommendationOrchestratorService recommendationService;
    
    // Inject UserRepository to look up the database ID of the authenticated user
    private final UserRepository userRepository;

    public RecommendationController(RecommendationOrchestratorService recommendationService,
    								UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
    }
    
    
    // The endpoint is now /api/v1/recommendations
    @GetMapping
    public ResponseEntity<RecommendationResultDTO> getRecommendations(Principal principal) {
        
        // Spring Security automatically extracts the username/email from the valid JWT
        String username = principal.getName();
        
        // Look up the secure user entity to get their Long ID
        // Note: Check whether UserRepository uses findByEmail or findByUsername!
        User user = userRepository.findByEmail(username) 
                .orElseThrow(() -> new RuntimeException("Secure user not found: " + username));

        // Pass the securely retrieved ID to your existing service logic
        return ResponseEntity.ok(recommendationService.getRecommendationsForUser(user.getId()));
    }
    
}