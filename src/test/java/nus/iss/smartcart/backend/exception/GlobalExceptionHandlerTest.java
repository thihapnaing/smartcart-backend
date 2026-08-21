package nus.iss.smartcart.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404WithMessage() {
        EntityNotFoundException ex = new EntityNotFoundException("Cart not found");
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Cart not found", response.getBody().getMessage());
        assertEquals(404, response.getBody().getStatus());
    }

    @Test
    void handleGeneric_returns500WithMessage() {
        Exception ex = new Exception();
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertEquals(500, response.getBody().getStatus());
    }

    @Test
    void handleConflict_returns409WithMessage() {
        IllegalStateException ex = new IllegalStateException("Cannot checkout an empty cart");
        ResponseEntity<ErrorResponse> response = handler.handleConflict(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Cannot checkout an empty cart", response.getBody().getMessage());
        assertEquals(409, response.getBody().getStatus());
    }

    @Test
    void handleBadRequest_returns400WithMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Requested quantity exceeds available stock");
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Requested quantity exceeds available stock", response.getBody().getMessage());
        assertEquals(400, response.getBody().getStatus());
    }

    @Test
    void handleForbiddenRequest_returns403WithMessage() {
        ForbiddenException ex = new ForbiddenException("You do not have permission to update this product");
        ResponseEntity<ErrorResponse> response = handler.handleForbiddenRequest(ex);
        assertEquals("You do not have permission to update this product", response.getBody().getMessage());
        assertEquals(403, response.getBody().getStatus());
    }
}
