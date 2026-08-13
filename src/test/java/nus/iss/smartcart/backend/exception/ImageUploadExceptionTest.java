package nus.iss.smartcart.backend.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ImageUploadExceptionTest {
    @Test
    void shouldCreateExceptionWithMessage() {
        String message = "Failed to upload image";

        ImageUploadException exception = new ImageUploadException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        String message = "Failed to upload image";
        Throwable cause = new RuntimeException("Storage error");

        ImageUploadException exception =
                new ImageUploadException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
