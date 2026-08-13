package nus.iss.smartcart.backend.tools.controller;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.tools.service.ToolDataService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Internal-only endpoints - NOT called by the Angular frontend, no @CrossOrigin. Only
 * smartcart-ai-service (the nested Python microservice) calls these, proxying its
 * get_order_history/search_products MCP tools to the real database via these two routes.
 */
@RestController
@RequestMapping("/internal/tools")
public class ToolDataController {

    private final ToolDataService toolDataService;

    public ToolDataController(ToolDataService toolDataService) {
        this.toolDataService = toolDataService;
    }

    @GetMapping("/order-history")
    public Map<String, Object> orderHistory(@RequestParam Long userId) {
        return toolDataService.getOrderHistory(userId);
    }

    @GetMapping("/products/search")
    public Map<String, Object> searchProducts(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "4") int limit,
        @RequestParam(defaultValue = "false") boolean newestFirst
    ) {
        return toolDataService.searchProducts(category, maxPrice, query, limit, newestFirst);
    }

    @GetMapping("/cart")
    public Map<String, Object> cart(@RequestParam Long userId) {
        return toolDataService.getCart(userId);
    }
}
