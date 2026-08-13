package nus.iss.smartcart.backend.dto;

//Author: Junior

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

//Author: Junior

@Getter
@Setter
public class ImageSearchResponse {

    private String prediction;

    private String searchLabel;

    private String gender;

    @JsonProperty("query_color")
    private String color;

    private String category;

    private List<ProductSearchResult> products;

    public ImageSearchResponse() {
    }
}