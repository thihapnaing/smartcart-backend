package nus.iss.smartcart.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageSearchRequest {

    private String gender;
    private String color;
    private String category;
}