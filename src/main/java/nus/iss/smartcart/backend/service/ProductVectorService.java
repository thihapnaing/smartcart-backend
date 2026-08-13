package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.ProductVectorDTO;
import nus.iss.smartcart.backend.repository.ProductVectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductVectorService {

    private final ProductVectorRepository productVectorRepository;

    public ProductVectorService(ProductVectorRepository productVectorRepository) {
        this.productVectorRepository = productVectorRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductVectorDTO> getProductsForVectorStore() {
        return productVectorRepository.getProductVectorData();
    }
}