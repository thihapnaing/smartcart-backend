package nus.iss.smartcart.backend.service;

// Author: Cecil

import nus.iss.smartcart.backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ImageSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    private ImageSearchService imageSearchService;

    @BeforeEach
    void setUp() {
        // Initialize the service with mocked dependencies and a dummy AI service URL
        imageSearchService = new ImageSearchService(
                productRepository,
                productService,
                "http://localhost:8001"
        );
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
}