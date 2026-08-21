package nus.iss.smartcart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nus.iss.smartcart.backend.dto.AddToCartRequest;
import nus.iss.smartcart.backend.dto.CartItemsResponse;
import nus.iss.smartcart.backend.dto.UpdateCartItemRequest;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import nus.iss.smartcart.backend.security.CustomUserDetailsService;
import nus.iss.smartcart.backend.security.JwtService;
import nus.iss.smartcart.backend.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private CartService cartService;
    @MockitoBean private CurrentUserProvider currentUserProvider;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUpCurrentCustomer() {
        User fakeCustomer = mock(User.class);
        when(fakeCustomer.getId()).thenReturn(2L);
        when(currentUserProvider.getCurrentCustomer()).thenReturn(fakeCustomer);
    }

    @Test
    void getCart_returnsOkWithCartData() throws Exception {
        CartItemsResponse fakeResponse = new CartItemsResponse(List.of(), BigDecimal.ZERO);
        when(cartService.getCart(2L)).thenReturn(fakeResponse);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk());
    }

    @Test
    void addToCart_returnsCreatedWithCartData() throws Exception {
        AddToCartRequest request = new AddToCartRequest(1L, 3);
        CartItemsResponse fakeResponse = new CartItemsResponse(List.of(), BigDecimal.ZERO);

        when(cartService.addToCart(2L,1L,3)).thenReturn(fakeResponse);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated());
    }

    @Test
    void updateQuantity_returnsOkWithCartData() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest(5);
        CartItemsResponse response = new CartItemsResponse(List.of(), BigDecimal.ZERO);

        when(cartService.updateQuantity(2L, 1L, 5)).thenReturn(response);

        mockMvc.perform(patch("/api/cart/items/{cartItemId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());
    }


}
