package nus.iss.smartcart.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageSearchResponse {

    private String prediction;

    private String searchLabel;

    private String gender;

    @JsonProperty("query_color")
    private String color;

    private String category;

    public ImageSearchResponse() {
    }
}