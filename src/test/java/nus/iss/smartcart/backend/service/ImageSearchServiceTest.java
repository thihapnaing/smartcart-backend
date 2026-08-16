package nus.iss.smartcart.backend.service;

// Author: Cecil
// Updated: Junior

import com.sun.net.httpserver.HttpServer;

import nus.iss.smartcart.backend.dto.ImageSearchResponse;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.repository.ProductRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private MultipartFile image;

    @InjectMocks
    private ImageSearchService imageSearchService;

    private HttpServer httpServer;
    private int serverPort;

    @BeforeEach //updated by Junior
    void setUp() throws IOException {

        httpServer =
                HttpServer.create(
                        new InetSocketAddress(0),
                        0
                );

        serverPort =
                httpServer
                        .getAddress()
                        .getPort();

        httpServer.start();

        WebClient testWebClient =
                WebClient.builder()
                        .baseUrl(
                                "http://localhost:"
                                        + serverPort
                        )
                        .build();


        ReflectionTestUtils.setField(
                imageSearchService,
                "webClient",
                testWebClient
        );
    }

    //CLEAN UP
    @AfterEach
    void tearDown() {

        if (httpServer != null) {

            httpServer.stop(0);
        }
    }

    @Test
    void searchByImage_nullImage_throwsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> imageSearchService.searchByImage(null)
        );

        assertEquals("Image file is required", exception.getMessage());
    }

    @Test
    void searchByImage_emptyImage_throwsIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                new byte[0]
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> imageSearchService.searchByImage(emptyFile)
        );

        assertEquals("Image file is required", exception.getMessage());
    }

    @Test
    void searchByImage_invalidFileContent_throwsRuntimeException() {
        // A valid multipart file with content, but WebClient will fail to connect 
        // to http://localhost:8001, triggering the catch block and RuntimeException.
        MockMultipartFile validFile = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> imageSearchService.searchByImage(validFile)
        );

        assertTrue(exception.getMessage().contains("Image search failed"));
    }

    //PYTHON RESPONSE MISSING
    @Test
    void searchByImage_aiReturnsNoResponse()
            throws Exception {

        startAiServer(
                ""
        );


        setupValidImage();


        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                imageSearchService
                                        .searchByImage(image)
                );


        assertTrue(
                exception.getMessage()
                        .contains(
                                "Image search failed"
                        )
        );
    }

    //HTTP ERROR
    @Test
    void searchByImage_aiServiceError()
            throws Exception {

        httpServer.createContext(
                "/api/image-search",
                exchange -> {

                    String response =
                            """
                            {
                              "message": "AI service unavailable"
                            }
                            """;


                    exchange.sendResponseHeaders(
                            500,
                            response.getBytes(
                                    StandardCharsets.UTF_8
                            ).length
                    );


                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(
                                response.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );
                    }
                }
        );


        setupValidImage();


        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                imageSearchService
                                        .searchByImage(image)
                );


        assertTrue(
                exception.getMessage()
                        .contains(
                                "Image search failed"
                        )
        );


        verifyNoInteractions(
                productRepository
        );
    }

    //NO PRODUCT FOUND
    @Test
    void searchByImage_noProductsFound()
            throws Exception {

        String aiResponse =
                """
                {
                  "prediction": "woman purple shoe",
                  "searchLabel": "purple shoe",
                  "gender": "woman",
                  "color": "purple",
                  "category": "Shoe",
                  "products": []
                }
                """;


        startAiServer(aiResponse);


        setupValidImage();


        when(
                productRepository
                        .searchByImageAttributes(
                                Gender.WOMEN,
                                null,
                                "Shoe",
                                ProductStatus.ACTIVE
                        )
        ).thenReturn(
                List.of()
        );


        ImageSearchResponse response =
                imageSearchService
                        .searchByImage(image);


        assertNotNull(response);


        assertEquals(
                "woman purple shoe",
                response.getPrediction()
        );


        assertNotNull(
                response.getProducts()
        );


        assertTrue(
                response.getProducts().isEmpty()
        );


        verify(
                productRepository
        ).searchByImageAttributes(
                Gender.WOMEN,
                null,
                "Shoe",
                ProductStatus.ACTIVE
        );


        verifyNoInteractions(
                productService
        );
    }

    //AI SERVICE
    private void startAiServer(String response) {

        httpServer.createContext(
                "/api/image-search",
                exchange -> {

                    byte[] responseBytes =
                            response.getBytes(
                                    StandardCharsets.UTF_8
                            );

                    exchange.getResponseHeaders()
                            .set(
                                    "Content-Type",
                                    "application/json"
                            );

                    exchange.sendResponseHeaders(
                            200,
                            responseBytes.length
                    );

                    try (OutputStream output =
                                 exchange.getResponseBody()) {

                        output.write(responseBytes);
                    }
                }
        );
    }

    //SETUP VALID IMAGE
    private void setupValidImage() throws IOException {

        byte[] imageBytes =
                "fake-image-data"
                        .getBytes(StandardCharsets.UTF_8);

        when(image.isEmpty())
                .thenReturn(false);

        when(image.getBytes())
                .thenReturn(imageBytes);

        when(image.getOriginalFilename())
                .thenReturn("test-image.jpg");

        when(image.getContentType())
                .thenReturn("image/jpeg");
    }

}