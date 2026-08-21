package nus.iss.smartcart.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ImageUploadResponse {
    private String imageUrl;
}
