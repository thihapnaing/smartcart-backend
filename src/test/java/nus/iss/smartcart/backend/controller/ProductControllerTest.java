package nus.iss.smartcart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nus.iss.smartcart.backend.dto.ProductRequest;
import nus.iss.smartcart.backend.dto.ProductDetailResponse;
import nus.iss.smartcart.backend.dto.VariantRequest;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.ProductStatus;
import nus.iss.smartcart.backend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ProductService productService;

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
}
