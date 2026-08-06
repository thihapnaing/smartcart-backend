package nus.iss.smartcart.backend.chat.service;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AiServiceExceptionTest {

    @Test
    void constructor_setsMessageAndIsARuntimeException() {
        AiServiceException exception = new AiServiceException("AI service error: HTTP 500 - boom");

        assertEquals("AI service error: HTTP 500 - boom", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }
}
