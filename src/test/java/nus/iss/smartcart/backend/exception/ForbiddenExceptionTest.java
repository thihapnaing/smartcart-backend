package nus.iss.smartcart.backend.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ForbiddenExceptionTest {

    @Test
    void singleArgConstructor_shouldSetMessage() {
        ForbiddenException ex = new ForbiddenException("You do not have permission to update this product");

        assertEquals("You do not have permission to update this product", ex.getMessage());
    }

    @Test
    void singleArgConstructor_shouldDefaultCodeToForbidden() {
        ForbiddenException ex = new ForbiddenException("You do not have permission to update this product");

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void twoArgConstructor_shouldSetMessage() {
        ForbiddenException ex = new ForbiddenException("ADMIN_LOCKED", "Product is locked by admin");

        assertEquals("Product is locked by admin", ex.getMessage());
    }

    @Test
    void forbiddenException_shouldBeARuntimeException() {
        ForbiddenException ex = new ForbiddenException("some message");

        assertInstanceOf(RuntimeException.class, ex);
    }
}
