package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.service.HomeContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/home")
@CrossOrigin(origins = "http://localhost:4200") // Allow Angular frontend
public class HomeContentController {

    private final HomeContentService homeContentService;

    public HomeContentController(HomeContentService homeContentService) {
        this.homeContentService = homeContentService;
    }

    @GetMapping("/trends/lookbook")
    public ResponseEntity<Map<String, Object>> getLookbook() {
        return ResponseEntity.ok(homeContentService.getLookbookTrends());
    }

    @GetMapping("/merchants/spotlight")
    public ResponseEntity<Map<String, Object>> getMerchantSpotlight() {
        return ResponseEntity.ok(homeContentService.getMerchantSpotlight());
    }
}