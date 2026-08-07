package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductSummaryDtoTest {

    @Test
    void gettersAndSetters_roundTripAllFields() {
        ProductSummaryDto dto = new ProductSummaryDto();

        dto.setProductId(1L);
        dto.setName("Tee");
        dto.setPrice(new BigDecimal("19.99"));
        dto.setImageUrl("tee.jpg");
        dto.setCategory("Tops");
        dto.setDefaultVariantId(5L);

        assertEquals(1L, dto.getProductId());
        assertEquals("Tee", dto.getName());
        assertEquals(new BigDecimal("19.99"), dto.getPrice());
        assertEquals("tee.jpg", dto.getImageUrl());
        assertEquals("Tops", dto.getCategory());
        assertEquals(5L, dto.getDefaultVariantId());
    }
}
