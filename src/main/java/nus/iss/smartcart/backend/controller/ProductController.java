package nus.iss.smartcart.backend.controller;
import nus.iss.smartcart.backend.dto.ProductDetailResponse;
import nus.iss.smartcart.backend.dto.ProductSearchResult;
import nus.iss.smartcart.backend.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductSearchResult>> searchProductsByKeyword(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchByKeyword(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetail(id));
    }
}
