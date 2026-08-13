package nus.iss.smartcart.backend.dto;

public class ConfirmDeliveryRequest {

    private String fileKey;

    public ConfirmDeliveryRequest() {
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }
}
