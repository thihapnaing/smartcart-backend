package nus.iss.smartcart.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ImageSearchResponse {

    private String prediction;

    @JsonProperty("query_color")
    private String queryColor;

    private Integer total;

    private List<ImageSearchResult> results;

    public ImageSearchResponse() {
    }

    public String getPrediction() {
        return prediction;
    }

    public void setPrediction(String prediction) {
        this.prediction = prediction;
    }

    public String getQueryColor() {
        return queryColor;
    }

    public void setQueryColor(String queryColor) {
        this.queryColor = queryColor;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<ImageSearchResult> getResults() {
        return results;
    }

    public void setResults(List<ImageSearchResult> results) {
        this.results = results;
    }
}
