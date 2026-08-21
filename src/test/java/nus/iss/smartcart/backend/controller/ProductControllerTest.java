package nus.iss.smartcart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nus.iss.smartcart.backend.dto.ProductRequest;
import nus.iss.smartcart.backend.dto.ProductDetailResponse;
import nus.iss.smartcart.backend.dto.ProductSearchResult;
import nus.iss.smartcart.backend.dto.VariantRequest;
import nus.iss.smartcart.backend.exception.ImageUploadException;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CustomUserDetailsService;
import nus.iss.smartcart.backend.security.JwtService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ProductService productService;
    @MockitoBean private ImageUploadService imageUploadService;
    @MockitoBean private ImageSearchService imageSearchService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void search_returnsOKWithProductData() throws Exception {
        when(productService.searchByKeyword("shirt")).thenReturn(List.of());
        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "shirt")
                )
                .andExpect(status().isOk());
    }

    @Test
    void browse_withAllParams_returnsOkWithResults() throws Exception {
        when(productService.search("shirt", "Tops", Gender.MEN, true, 10))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/products/browse")
                        .param("keyword", "shirt")
                        .param("category", "Tops")
                        .param("gender", "MEN")
                        .param("newestFirst", "true")
                        .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void browse_withNoParams_usesDefaults() throws Exception {
        when(productService.search(null, null, null, false, 20))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/products/browse"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductDetail_returnsOkWithProductData() throws Exception {
        ProductDetailResponse productDetailResponse =
                ProductDetailResponse.builder()
                        .productId(1L)
                        .name("White Tee")
                        .description("soft and white")
                        .price(BigDecimal.ZERO)
                        .imageUrl("")
                        .categoryName("Tops")
                        .shopName("SmartCart")
                        .variants(List.of())
                        .build();
        when(productService.getProductDetail(1L))
                .thenReturn(productDetailResponse);
        mockMvc.perform(get("/api/products/1")
        ).andExpect(status().isOk());
    }

    @Test
    void createProduct_returnsCreatedWithProductData() throws Exception {
        VariantRequest variant = new VariantRequest();
        variant.setSize("S");
        variant.setStock(10);
        ProductRequest request =
                ProductRequest.builder()
                        .name("White Tee")
                        .description("Soft and made of cotton")
                        .price(BigDecimal.valueOf(1))
                        .gender(Gender.MEN)
                        .categoryId(1L)
                        .status(ProductStatus.ACTIVE)
                        .variants(List.of(variant))
                        .build();

        when(productService.createProduct(any())).thenReturn(ProductDetailResponse.builder().build());
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated());
    }

    @Test
    void createProduct_missingName_returnsBadRequest() throws Exception {
        VariantRequest variant = new VariantRequest();
        variant.setSize("S");
        variant.setStock(10);
        ProductRequest request = ProductRequest.builder()
                .name("")
                .description("Soft and made of cotton")
                .price(BigDecimal.valueOf(1))
                .gender(Gender.MEN)
                .categoryId(1L)
                .status(ProductStatus.ACTIVE)
                .variants(List.of(variant))
                .build();

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void updateProduct_returnsOkWithProductData() throws Exception {
        VariantRequest variant = new VariantRequest();
        variant.setSize("S");
        variant.setStock(10);
        ProductRequest request = ProductRequest.builder()
                .name("White Tee")
                .description("Soft and made of cotton")
                .price(BigDecimal.valueOf(1))
                .gender(Gender.MEN)
                .categoryId(1L)
                .status(ProductStatus.ACTIVE)
                .variants(List.of(variant))
                .build();
        ProductDetailResponse fakeResponse = ProductDetailResponse.builder()
                .productId(1L)
                .name("White Tee")
                .status("ACTIVE")
                .variants(List.of())
                .build();

        when(productService.updateProduct(eq(1L), any())).thenReturn(fakeResponse);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("White Tee"));
    }

    @Test
    void updateProduct_missingName_returnsBadRequest() throws Exception {
        VariantRequest variant = new VariantRequest();
        variant.setSize("S");
        variant.setStock(10);
        ProductRequest request = ProductRequest.builder()
                .name("")
                .description("Soft and made of cotton")
                .price(BigDecimal.valueOf(1))
                .gender(Gender.MEN)
                .categoryId(1L)
                .status(ProductStatus.ACTIVE)
                .variants(List.of(variant))
                .build();

        mockMvc.perform(put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void deactivateProduct_returnsOkWithProductData() throws Exception {
        ProductDetailResponse productDetailResponse =
                ProductDetailResponse.builder()
                        .productId(1L)
                        .name("White Tee")
                        .description("soft and white")
                        .price(BigDecimal.ZERO)
                        .imageUrl("")
                        .gender("MEN")
                        .categoryName("Tops")
                        .shopName("SmartCart")
                        .status("INACTIVE")
                        .variants(List.of())
                        .build();
        when(productService.deactivateProduct(1L)).thenReturn(productDetailResponse);
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void getMerchantProducts_returnsOkWithResults() throws Exception {
        ProductSearchResult fakeResult = ProductSearchResult.builder()
                .id(1L)
                .name("White Tee")
                .status("ACTIVE")
                .build();

        when(productService.getMerchantProducts()).thenReturn(List.of(fakeResult));

        mockMvc.perform(get("/api/products/own"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("White Tee"));
    }

    @Test
    void uploadImage_validFile_returnsOKWithImageUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "fake-image-content".getBytes()
        );

        when(imageUploadService.uploadImage(any())).thenReturn("https://res.cloudinary.com/demo/sample.jpg");
        mockMvc.perform(multipart("/api/products/image-upload")
                        .file(file)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://res.cloudinary.com/demo/sample.jpg"));
    }

    @Test
    void uploadImage_serviceThrowsException_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "fake-content".getBytes());

        when(imageUploadService.uploadImage(any())).thenThrow(new ImageUploadException("Only PNG and JPEG images are allowed"));

        mockMvc.perform(multipart("/api/products/image-upload").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activateProduct_returnsOKWithProductData() throws Exception {
        ProductDetailResponse response = ProductDetailResponse.builder()
                .productId(1L)
                .name("White Tee")
                .description("soft and white")
                .price(BigDecimal.ZERO)
                .imageUrl("")
                .gender("MEN")
                .categoryName("Tops")
                .shopName("SmartCart")
                .status("ACTIVE")
                .variants(List.of())
                .build();
        when(productService.activateProduct(1L)).thenReturn(response);
        mockMvc.perform(patch("/api/products/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}