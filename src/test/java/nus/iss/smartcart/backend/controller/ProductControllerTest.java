package nus.iss.smartcart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nus.iss.smartcart.backend.dto.ImageUploadResponse;
import nus.iss.smartcart.backend.dto.ProductDetailResponse;
import nus.iss.smartcart.backend.dto.ProductRequest;
import nus.iss.smartcart.backend.dto.ProductSearchResult;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.service.ImageSearchService;
import nus.iss.smartcart.backend.service.ImageUploadService;
import nus.iss.smartcart.backend.service.ProductService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ImageSearchService imageSearchService;

    @MockitoBean
    private ImageUploadService imageUploadService;


    // ============================================================
    // GET /api/products/search
    // ============================================================

    @Test
    void searchProductsByKeyword_returnsOkWithResults() throws Exception {

        ProductSearchResult result =
                searchResult(1L, "White Tee");

        when(productService.searchByKeyword("shirt"))
                .thenReturn(List.of(result));

        mockMvc.perform(
                        get("/api/products/search")
                                .param("keyword", "shirt")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("White Tee"));

        verify(productService)
                .searchByKeyword("shirt");
    }


    @Test
    void searchProductsByKeyword_noResults_returnsEmptyList()
            throws Exception {

        when(productService.searchByKeyword("unknown"))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/products/search")
                                .param("keyword", "unknown")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(productService)
                .searchByKeyword("unknown");
    }


    // ============================================================
    // GET /api/products/browse
    // ============================================================

    @Test
    void browse_withAllParams_returnsOkWithResults()
            throws Exception {

        ProductSearchResult result =
                searchResult(1L, "Red Shirt");

        when(productService.search(
                "shirt",
                "Tops",
                Gender.MEN,
                true,
                10
        )).thenReturn(List.of(result));

        mockMvc.perform(
                        get("/api/products/browse")
                                .param("keyword", "shirt")
                                .param("category", "Tops")
                                .param("gender", "MEN")
                                .param("newestFirst", "true")
                                .param("limit", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Red Shirt"));

        verify(productService).search(
                "shirt",
                "Tops",
                Gender.MEN,
                true,
                10
        );
    }


    @Test
    void browse_withDefaultParams_passesDefaultsToService()
            throws Exception {

        when(productService.search(
                null,
                null,
                null,
                false,
                20
        )).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/products/browse")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(productService).search(
                null,
                null,
                null,
                false,
                20
        );
    }


    // ============================================================
    // GET /api/products/{id}
    // ============================================================

    @Test
    void getProductDetail_returnsOkWithProductData()
            throws Exception {

        ProductDetailResponse response =
                detailResponse(10L, "Blue Shirt");

        when(productService.getProductDetail(10L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/products/10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.name").value("Blue Shirt"));

        verify(productService)
                .getProductDetail(10L);
    }


    // ============================================================
    // POST /api/products
    // ============================================================

    @Test
    void createProduct_returnsCreatedWithProductData()
            throws Exception {

        ProductRequest request = new ProductRequest();

        request.setName("Black Shirt");
        request.setDescription("Black shirt");
        request.setPrice(BigDecimal.valueOf(80));
        request.setGender(Gender.MEN);
        request.setCategoryId(3L);
        request.setImageUrl("/images/black.jpg");

        ProductDetailResponse response =
                detailResponse(50L, "Black Shirt");

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(50))
                .andExpect(jsonPath("$.name").value("Black Shirt"));

        verify(productService)
                .createProduct(any(ProductRequest.class));
    }


    // ============================================================
    // PUT /api/products/{id}
    // ============================================================

    @Test
    void updateProduct_returnsOkWithProductData()
            throws Exception {

        ProductRequest request = new ProductRequest();

        request.setName("Updated Shirt");
        request.setDescription("Updated description");
        request.setPrice(BigDecimal.valueOf(90));
        request.setGender(Gender.WOMEN);
        request.setCategoryId(3L);
        request.setImageUrl("/images/updated.jpg");

        ProductDetailResponse response =
                detailResponse(10L, "Updated Shirt");

        when(productService.updateProduct(
                eq(10L),
                any(ProductRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/products/10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.name").value("Updated Shirt"));

        verify(productService).updateProduct(
                eq(10L),
                any(ProductRequest.class)
        );
    }


    // ============================================================
    // DELETE /api/products/{id}
    // ============================================================

    @Test
    void deactivateProduct_returnsOkWithProductData()
            throws Exception {

        ProductDetailResponse response =
                detailResponse(10L, "Inactive Shirt");

        when(productService.deactivateProduct(10L))
                .thenReturn(response);

        mockMvc.perform(
                        delete("/api/products/10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.name").value("Inactive Shirt"));

        verify(productService)
                .deactivateProduct(10L);
    }


    // ============================================================
    // GET /api/products/own
    // ============================================================

    @Test
    void getMerchantProducts_returnsOkWithResults()
            throws Exception {

        ProductSearchResult result =
                searchResult(20L, "Merchant Shirt");

        when(productService.getMerchantProducts())
                .thenReturn(List.of(result));

        mockMvc.perform(
                        get("/api/products/own")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].name")
                        .value("Merchant Shirt"));

        verify(productService)
                .getMerchantProducts();
    }


    @Test
    void getMerchantProducts_noProducts_returnsEmptyList()
            throws Exception {

        when(productService.getMerchantProducts())
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/products/own")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(productService)
                .getMerchantProducts();
    }


    // ============================================================
    // POST /api/products/image-upload
    // ============================================================

    @Test
    void uploadImage_validFile_returnsOkWithImageUrl()
            throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "shirt.jpg",
                        MediaType.IMAGE_JPEG_VALUE,
                        "fake-image".getBytes()
                );

        when(imageUploadService.uploadImage(any()))
                .thenReturn("/assets/products/shirt.jpg");

        mockMvc.perform(
                        multipart("/api/products/image-upload")
                                .file(file)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.imageUrl")
                                .value("/assets/products/shirt.jpg")
                );

        verify(imageUploadService)
                .uploadImage(any());
    }


    // ============================================================
    // POST /api/products/search/image
    // ============================================================

    @Test
    void searchByImage_validImage_returnsOkWithResults()
            throws Exception {

        MockMultipartFile image =
                new MockMultipartFile(
                        "image",
                        "red-shirt.jpg",
                        MediaType.IMAGE_JPEG_VALUE,
                        "fake-image".getBytes()
                );

        ProductSearchResult result =
                searchResult(30L, "Red Shirt");

        when(imageSearchService.searchByImage(any()))
                .thenReturn(List.of(result));

        mockMvc.perform(
                        multipart("/api/products/search/image")
                                .file(image)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30))
                .andExpect(jsonPath("$[0].name")
                        .value("Red Shirt"));

        verify(imageSearchService)
                .searchByImage(any());
    }


    @Test
    void searchByImage_noResults_returnsEmptyList()
            throws Exception {

        MockMultipartFile image =
                new MockMultipartFile(
                        "image",
                        "test.jpg",
                        MediaType.IMAGE_JPEG_VALUE,
                        "fake-image".getBytes()
                );

        when(imageSearchService.searchByImage(any()))
                .thenReturn(List.of());

        mockMvc.perform(
                        multipart("/api/products/search/image")
                                .file(image)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(imageSearchService)
                .searchByImage(any());
    }


    // ============================================================
    // Helper methods
    // ============================================================

    private ProductSearchResult searchResult(
            Long id,
            String name
    ) {

        ProductSearchResult result =
                new ProductSearchResult();

        result.setId(id);
        result.setName(name);
        result.setDescription("Test product");
        result.setPrice(BigDecimal.valueOf(50));
        result.setImageUrl(
                "/assets/products/product.jpg"
        );
        result.setShopName("SmartCart Shop");
        result.setCategoryName("Tops");
        result.setGender("MEN");
        result.setColor("RED");
        result.setStatus("ACTIVE");
        result.setDefaultVariantId(null);

        return result;
    }


    private ProductDetailResponse detailResponse(
            Long id,
            String name
    ) {

        ProductDetailResponse response =
                new ProductDetailResponse();

        response.setProductId(id);
        response.setName(name);
        response.setDescription("Test product");
        response.setPrice(BigDecimal.valueOf(50));
        response.setImageUrl(
                "/assets/products/product.jpg"
        );
        response.setShopName("SmartCart Shop");
        response.setCategoryName("Tops");
        response.setGender("MEN");
        response.setStatus("ACTIVE");

        return response;
    }
}