package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.ImageSearchResponse;
import nus.iss.smartcart.backend.dto.ProductSearchResult;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.Product;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.repository.ProductRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

@Service
public class ImageSearchService {

    private final WebClient webClient;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public ImageSearchService(
            ProductRepository productRepository,
            ProductService productService,
            @Value("${ai.python-service.base-url}") String aiServiceUrl){

        this.productRepository = productRepository;
        this.productService = productService;

        this.webClient = WebClient.builder()
                .baseUrl(aiServiceUrl)
                .build();
    }

    public List<ProductSearchResult> searchByImage(
            MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(
                    "Image file is required"
            );
        }

        try {

            ByteArrayResource imageResource =
                    new ByteArrayResource(image.getBytes()) {

                        @Override
                        public String getFilename() {
                            String filename =
                                    image.getOriginalFilename();

                            return filename != null
                                    ? filename
                                    : "image.jpg";
                        }
                    };

            MultipartBodyBuilder builder =
                    new MultipartBodyBuilder();

            builder.part("image", imageResource)
                    .contentType(MediaType.IMAGE_JPEG)
                    .filename(
                            image.getOriginalFilename() != null
                                    ? image.getOriginalFilename()
                                    : "image.jpg"
                    );

            System.out.println(
                    "========== SENDING TO PYTHON =========="
            );

            ImageSearchResponse aiResponse =
                    webClient.post()
                            .uri("/api/image-search")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(
                                    BodyInserters.fromMultipartData(
                                            builder.build()
                                    )
                            )
                            .retrieve()
                            .bodyToMono(ImageSearchResponse.class)
                            .block();

            System.out.println(
                    "========== PYTHON RESPONSE RECEIVED =========="
            );

            if (aiResponse == null) {
                throw new RuntimeException(
                        "AI service returned no response"
                );
            }

            System.out.println(
                    "Gender: " + aiResponse.getGender()
            );

            System.out.println(
                    "Color: " + aiResponse.getColor()
            );

            System.out.println(
                    "Category: " + aiResponse.getCategory()
            );

            return searchProductsFromPrediction(
                    aiResponse
            );

        } catch (Exception e) {

            System.err.println(
                    "========== IMAGE SEARCH ERROR =========="
            );

            System.err.println(
                    "Exception: " + e.getClass().getName()
            );

            System.err.println(
                    "Message: " + e.getMessage()
            );

            e.printStackTrace();

            System.err.println(
                    "========================================"
            );

            throw new RuntimeException(
                    "Image search failed: " + e.getMessage(),
                    e
            );
        }
    }

    private List<ProductSearchResult> searchProductsFromPrediction(
            ImageSearchResponse aiResponse) {
        System.out.println("Gender: " + aiResponse.getGender());
        System.out.println("Color: " + aiResponse.getColor());
        System.out.println("Category: " + aiResponse.getCategory());
        System.out.println("==================================");

        try {

            Gender gender;

            if ("woman".equalsIgnoreCase(aiResponse.getGender())) {
                gender = Gender.WOMAN;
            } else if ("man".equalsIgnoreCase(aiResponse.getGender())) {
                gender = Gender.MAN;
            } else {
                throw new IllegalArgumentException(
                        "Unknown gender from AI: "
                                + aiResponse.getGender()
                );
            }

            System.out.println("========== SEARCH PARAMETERS ==========");
            System.out.println("Gender enum: " + gender);
            System.out.println("Color: " + aiResponse.getColor());
            System.out.println("Category: " + aiResponse.getCategory());
            System.out.println("Status: " + ProductStatus.ACTIVE);
            System.out.println("=======================================");

            List<Product> products =
                    productRepository.searchByImageAttributes(
                            gender,
                            aiResponse.getColor(),
                            aiResponse.getCategory(),
                            ProductStatus.ACTIVE
                    );

            System.out.println(
                    "Products found: " + products.size()
            );

            return products.stream()
                    .map(product -> {

                        System.out.println(
                                "Converting product ID: "
                                        + product.getId()
                        );

                        return productService.toSearchResult(product);

                    })
                    .toList();

        } catch (Exception e) {

            System.err.println(
                    "========== IMAGE SEARCH DB ERROR =========="
            );

            e.printStackTrace();

            System.err.println(
                    "==========================================="
            );

            throw e;
        }
    }
}