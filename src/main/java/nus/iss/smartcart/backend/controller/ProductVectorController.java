package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.dto.ProductVectorDTO;
import nus.iss.smartcart.backend.service.ProductVectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductVectorController {

    private final ProductVectorService productVectorService;

    public ProductVectorController(ProductVectorService productVectorService) {
        this.productVectorService = productVectorService;
    }

    @GetMapping("/vector-export")
    public ResponseEntity<List<ProductVectorDTO>> exportProductsForVector() {
        List<ProductVectorDTO> vectorData = productVectorService.getProductsForVectorStore();
        return ResponseEntity.ok(vectorData);
    }
}