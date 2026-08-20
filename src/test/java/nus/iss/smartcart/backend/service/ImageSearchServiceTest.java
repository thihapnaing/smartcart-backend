package nus.iss.smartcart.backend.service;

// Author: Junior

import nus.iss.smartcart.backend.dto.ImageSearchResponse;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageSearchServiceTest {

    private ProductRepository productRepository;
    private ProductService productService;
    private ImageSearchService imageSearchService;

    private Method searchProductsFromPredictionMethod;

    @BeforeEach
    void setUp() throws Exception {

        productRepository = mock(ProductRepository.class);
        productService = mock(ProductService.class);

        imageSearchService = new ImageSearchService(
                productRepository,
                productService,
                "http://localhost:8000"
        );

        searchProductsFromPredictionMethod =
                ImageSearchService.class.getDeclaredMethod(
                        "searchProductsFromPrediction",
                        ImageSearchResponse.class
                );

        searchProductsFromPredictionMethod.setAccessible(true);
    }


    // =========================================================
    // HELPER
    // =========================================================

    @SuppressWarnings("unchecked")
    private List<?> invokeSearchProducts(
            ImageSearchResponse aiResponse) {

        try {

            return (List<?>) searchProductsFromPredictionMethod.invoke(
                    imageSearchService,
                    aiResponse
            );

        } catch (InvocationTargetException e) {

            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException(cause);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }


    private ImageSearchResponse mockAiResponse(
            String gender,
            String color,
            String category) {

        ImageSearchResponse response =
                mock(ImageSearchResponse.class);

        when(response.getGender())
                .thenReturn(gender);

        when(response.getColor())
                .thenReturn(color);

        when(response.getCategory())
                .thenReturn(category);

        return response;
    }


    // =========================================================
    // SHIRT -> TOPS
    // =========================================================

    @Test
    void searchByImage_shirtMapsToTops() {

        ImageSearchResponse response =
                mockAiResponse(
                        "women",
                        "black",
                        "shirt"
                );

        when(productRepository.searchByImageAttributes(
                Gender.WOMEN,
                "black",
                "Tops",
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        List<?> products =
                invokeSearchProducts(response);

        assertNotNull(products);
        assertTrue(products.isEmpty());

        verify(productRepository).searchByImageAttributes(
                Gender.WOMEN,
                "black",
                "Tops",
                ProductStatus.ACTIVE
        );
    }


    // =========================================================
    // PANTS -> BOTTOMS
    // =========================================================

    @Test
    void searchByImage_pantsMapsToBottoms() {

        ImageSearchResponse response =
                mockAiResponse(
                        "women",
                        "black",
                        "pants"
                );

        when(productRepository.searchByImageAttributes(
                Gender.WOMEN,
                "black",
                "Bottoms",
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        List<?> products =
                invokeSearchProducts(response);

        assertNotNull(products);
        assertTrue(products.isEmpty());

        verify(productRepository).searchByImageAttributes(
                Gender.WOMEN,
                "black",
                "Bottoms",
                ProductStatus.ACTIVE
        );
    }


    // =========================================================
    // SHOE -> SHOES
    // =========================================================

    @Test
    void searchByImage_shoeMapsToShoes() {

        ImageSearchResponse response =
                mockAiResponse(
                        "women",
                        null,
                        "shoe"
                );

        /*
         * IMPORTANT:
         * Database category is "Shoes", not "Shoe".
         *
         * This test specifically protects against the CI failure
         * caused by stubbing "Shoe" while production code sends
         * "Shoes".
         */
        when(productRepository.searchByImageAttributes(
                Gender.WOMEN,
                null,
                "Shoes",
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        List<?> products =
                invokeSearchProducts(response);

        assertNotNull(products);
        assertTrue(products.isEmpty());

        verify(productRepository).searchByImageAttributes(
                Gender.WOMEN,
                null,
                "Shoes",
                ProductStatus.ACTIVE
        );
    }


    // =========================================================
    // MEN GENDER
    // =========================================================

    @Test
    void searchByImage_menMapsToMenGender() {

        ImageSearchResponse response =
                mockAiResponse(
                        "men",
                        "blue",
                        "shirt"
                );

        when(productRepository.searchByImageAttributes(
                Gender.MEN,
                "blue",
                "Tops",
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        List<?> products =
                invokeSearchProducts(response);

        assertNotNull(products);
        assertTrue(products.isEmpty());

        verify(productRepository).searchByImageAttributes(
                Gender.MEN,
                "blue",
                "Tops",
                ProductStatus.ACTIVE
        );
    }


    // =========================================================
    // WOMAN SINGULAR
    // =========================================================

    @Test
    void searchByImage_womanMapsToWomenGender() {

        ImageSearchResponse response =
                mockAiResponse(
                        "woman",
                        "red",
                        "shirt"
                );

        when(productRepository.searchByImageAttributes(
                Gender.WOMEN,
                "red",
                "Tops",
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        List<?> products =
                invokeSearchProducts(response);

        assertNotNull(products);
        assertTrue(products.isEmpty());

        verify(productRepository).searchByImageAttributes(
                Gender.WOMEN,
                "red",
                "Tops",
                ProductStatus.ACTIVE
        );
    }


    // =========================================================
    // CATEGORY ALIASES
    // =========================================================

    @Test
    void searchByImage_tshirtMapsToTops() {

        ImageSearchResponse response =
                mockAiResponse(
                        "women",
                        "black",
                        "t-shirt"
                );

        when(productRepository.searchByImageAttributes(
                Gender.WOMEN,
                "black",
                "Tops",
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        invokeSearchProducts(response);

        verify(productRepository).searchByImageAttributes(
                Gender.WOMEN,
                "black",
                "Tops",
                ProductStatus.ACTIVE
        );
    }


    @Test
    void searchByImage_trousersMapsToBottoms() {

        ImageSearchResponse response =
                mockAiResponse(
                        "men",
                        "blue",
                        "trousers"
                );

        when(productRepository.searchByImageAttributes(
                Gender.MEN,
                "blue",
                "Bottoms",
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        invokeSearchProducts(response);

        verify(productRepository).searchByImageAttributes(
                Gender.MEN,
                "blue",
                "Bottoms",
                ProductStatus.ACTIVE
        );
    }


    @Test
    void searchByImage_shoesMapsToShoes() {

        ImageSearchResponse response =
                mockAiResponse(
                        "men",
                        "black",
                        "shoes"
                );

        when(productRepository.searchByImageAttributes(
                Gender.MEN,
                "black",
                "Shoes",
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        invokeSearchProducts(response);

        verify(productRepository).searchByImageAttributes(
                Gender.MEN,
                "black",
                "Shoes",
                ProductStatus.ACTIVE
        );
    }


    // =========================================================
    // UNKNOWN CATEGORY
    // =========================================================

    @Test
    void searchByImage_unknownCategoryUsesOriginalCategory() {

        ImageSearchResponse response =
                mockAiResponse(
                        "women",
                        "purple",
                        "dress"
                );

        when(productRepository.searchByImageAttributes(
                Gender.WOMEN,
                "purple",
                "dress",
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        invokeSearchProducts(response);

        verify(productRepository).searchByImageAttributes(
                Gender.WOMEN,
                "purple",
                "dress",
                ProductStatus.ACTIVE
        );
    }


    // =========================================================
    // NO PRODUCTS FOUND
    // =========================================================

    @Test
    void searchByImage_noProductsFound() {

        ImageSearchResponse response =
                mockAiResponse(
                        "women",
                        null,
                        "shoe"
                );

        when(productRepository.searchByImageAttributes(
                Gender.WOMEN,
                null,
                "Shoes",
                ProductStatus.ACTIVE
        )).thenReturn(List.of());

        List<?> products =
                invokeSearchProducts(response);

        assertNotNull(products);
        assertTrue(products.isEmpty());

        verify(productRepository).searchByImageAttributes(
                Gender.WOMEN,
                null,
                "Shoes",
                ProductStatus.ACTIVE
        );
    }


    // =========================================================
    // INVALID GENDER
    // =========================================================

    @Test
    void searchByImage_invalidGenderThrowsException() {

        ImageSearchResponse response =
                mockAiResponse(
                        "unknown",
                        "black",
                        "shirt"
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> invokeSearchProducts(response)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Unknown gender from AI")
        );

        verifyNoInteractions(productRepository);
    }


    // =========================================================
    // NULL IMAGE
    // =========================================================

    @Test
    void searchByImage_nullImageThrowsException() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> imageSearchService.searchByImage(null)
                );

        assertEquals(
                "Image file is required",
                exception.getMessage()
        );

        verifyNoInteractions(productRepository);
    }


    // =========================================================
    // EMPTY IMAGE
    // =========================================================

    @Test
    void searchByImage_emptyImageThrowsException() {

        MultipartFile image =
                mock(MultipartFile.class);

        when(image.isEmpty()).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> imageSearchService.searchByImage(image)
                );

        assertEquals(
                "Image file is required",
                exception.getMessage()
        );

        verifyNoInteractions(productRepository);
    }
}
