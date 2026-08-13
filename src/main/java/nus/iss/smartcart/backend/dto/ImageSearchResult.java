package nus.iss.smartcart.backend.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

//Author: Junior

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class ImageSearchResult {

    private Long productId;
    private Double similarity;

    public ImageSearchResult() {
    }
}
