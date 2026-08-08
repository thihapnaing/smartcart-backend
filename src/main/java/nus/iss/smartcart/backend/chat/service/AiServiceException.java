package nus.iss.smartcart.backend.chat.service;

// Author: Htet Nandar (Grace)

/**
 * Thrown when smartcart-ai-service (the Python microservice) returns a non-200 response or
 * otherwise fails to fulfill a chat request. Replaces a generic RuntimeException so callers/logs
 * can distinguish "the AI service itself failed" from other unrelated runtime errors.
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message) {
        super(message);
    }
}
