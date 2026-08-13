package nus.iss.smartcart.backend.tools.controller;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.tools.service.ToolDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Thin-delegate tests, same style as ChatControllerTest - the real logic lives in
 * ToolDataService (see ToolDataServiceTest); this only checks the controller wires the
 * request's query params through to the service correctly.
 */
@ExtendWith(MockitoExtension.class)
class ToolDataControllerTest {

    @Mock
    private ToolDataService toolDataService;

    @Test
    void orderHistory_passesUserIdThroughAndReturnsServiceResult() {
        ToolDataController controller = new ToolDataController(toolDataService);
        Map<String, Object> expected = Map.of("orderCount", 3);
        when(toolDataService.getOrderHistory(42L)).thenReturn(expected);

        Map<String, Object> actual = controller.orderHistory(42L);

        assertSame(expected, actual);
        verify(toolDataService).getOrderHistory(42L);
    }

    @Test
    void searchProducts_passesAllFiltersThroughToService() {
        ToolDataController controller = new ToolDataController(toolDataService);
        Map<String, Object> expected = Map.of("products", List.of());
        when(toolDataService.searchProducts("Tops", new BigDecimal("50.00"), "tee", 2, true))
            .thenReturn(expected);

        Map<String, Object> actual = controller.searchProducts("Tops", new BigDecimal("50.00"), "tee", 2, true);

        assertSame(expected, actual);
        verify(toolDataService).searchProducts("Tops", new BigDecimal("50.00"), "tee", 2, true);
    }

    @Test
    void searchProducts_allowsAllOptionalFiltersToBeOmitted() {
        ToolDataController controller = new ToolDataController(toolDataService);
        Map<String, Object> expected = Map.of("products", List.of());
        when(toolDataService.searchProducts(null, null, null, 4, false)).thenReturn(expected);

        Map<String, Object> actual = controller.searchProducts(null, null, null, 4, false);

        assertSame(expected, actual);
        verify(toolDataService).searchProducts(null, null, null, 4, false);
    }

    @Test
    void cart_passesUserIdThroughAndReturnsServiceResult() {
        ToolDataController controller = new ToolDataController(toolDataService);
        Map<String, Object> expected = Map.of("itemCount", 2);
        when(toolDataService.getCart(42L)).thenReturn(expected);

        Map<String, Object> actual = controller.cart(42L);

        assertSame(expected, actual);
        verify(toolDataService).getCart(42L);
    }
}
