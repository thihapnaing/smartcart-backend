package nus.iss.smartcart.backend.service;

// Author: Junior

import nus.iss.smartcart.backend.dto.ImageSearchResponse;
import nus.iss.smartcart.backend.dto.ProductSearchResult;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.repository.ProductRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ImageSearchService {

    private final WebClient webClient;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public ImageSearchService(
            ProductRepository productRepository,
            ProductService productService,
            @Value("${ai.python-service.base-url}") String aiServiceUrl) {

        this.productRepository = productRepository;
        this.productService = productService;

        this.webClient = WebClient.builder()
                .baseUrl(aiServiceUrl)
                .build();
    }

    // =========================================================
    // IMAGE SEARCH
    // =========================================================

    public ImageSearchResponse searchByImage(
            MultipartFile image) {

        // =====================================================
        // VALIDATE IMAGE
        // =====================================================

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(
                    "Image file is required"
            );
        }

        try {

            // =================================================
            // GET ORIGINAL FILENAME
            // =================================================

            String originalFilename =
                    image.getOriginalFilename();

            final String filename =
                    originalFilename != null
                            && !originalFilename.isBlank()
                            ? originalFilename
                            : "image.jpg";

            // =================================================
            // CREATE IMAGE RESOURCE
            // =================================================

            ByteArrayResource imageResource =
                    new ByteArrayResource(
                            image.getBytes()
                    ) {

                        @Override
                        public String getFilename() {
                            return filename;
                        }
                    };

            // =================================================
            // GET CONTENT TYPE
            // =================================================

            String contentType =
                    image.getContentType();

            final MediaType imageMediaType;

            if (contentType == null
                    || contentType.isBlank()) {

                imageMediaType =
                        MediaType.IMAGE_JPEG;

            } else {

                imageMediaType =
                        MediaType.parseMediaType(
                                contentType
                        );
            }

            // =================================================
            // BUILD MULTIPART REQUEST
            // =================================================

            MultipartBodyBuilder builder =
                    new MultipartBodyBuilder();

            builder.part(
                            "image",
                            imageResource
                    )
                    .contentType(imageMediaType)
                    .filename(filename);

            // =================================================
            // SEND IMAGE TO PYTHON AI SERVICE
            // =================================================

            ImageSearchResponse aiResponse =
                    webClient.post()
                            .uri("/api/image-search")
                            .contentType(
                                    MediaType.MULTIPART_FORM_DATA
                            )
                            .body(
                                    BodyInserters.fromMultipartData(
                                            builder.build()
                                    )
                            )
                            .retrieve()
                            .bodyToMono(
                                    ImageSearchResponse.class
                            )
                            .block();

            // =================================================
            // CHECK AI RESPONSE
            // =================================================

            if (aiResponse == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI service returned no response"
                );
            }

            // =================================================
            // SEARCH MYSQL
            // =================================================

            List<ProductSearchResult> products =
                    searchProductsFromPrediction(
                            aiResponse
                    );

            // =================================================
            // ADD PRODUCTS TO RESPONSE
            // =================================================

            aiResponse.setProducts(
                    products
            );

            return aiResponse;

        } catch (Exception e) {

            // =================================================
            // ERROR HANDLING
            // =================================================

            throw new RuntimeException(
                    "Image search failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // SEARCH MYSQL USING AI PREDICTION
    // =========================================================

    private List<ProductSearchResult>
    searchProductsFromPrediction(
            ImageSearchResponse aiResponse) {

        // =====================================================
        // CONVERT GENDER
        // =====================================================

        Gender gender;

        String aiGender =
                aiResponse.getGender();

        if ("woman".equalsIgnoreCase(aiGender)
                || "women".equalsIgnoreCase(aiGender)) {

            gender = Gender.WOMEN;

        } else if ("man".equalsIgnoreCase(aiGender)
                || "men".equalsIgnoreCase(aiGender)) {

            gender = Gender.MEN;

        } else {

            throw new IllegalArgumentException(
                    "Unknown gender from AI: "
                            + aiGender
            );
        }

        // =====================================================
        // SEARCH PRODUCTS
        // =====================================================

        List<Product> products =
                productRepository.searchByImageAttributes(
                        gender,
                        aiResponse.getColor(),
                        aiResponse.getCategory(),
                        ProductStatus.ACTIVE
                );

        // =====================================================
        // CONVERT PRODUCTS TO SEARCH RESULTS
        // =====================================================

        return products.stream()
                .map(productService::toSearchResult)
                .toList();
    }
}