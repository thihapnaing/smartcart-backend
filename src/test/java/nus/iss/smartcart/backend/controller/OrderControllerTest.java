package nus.iss.smartcart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nus.iss.smartcart.backend.dto.CheckoutRequest;
import nus.iss.smartcart.backend.dto.CheckoutResponse;
import nus.iss.smartcart.backend.dto.DeliveryDetails;
import nus.iss.smartcart.backend.dto.MerchantOrderItemResponse;
import nus.iss.smartcart.backend.model.OrderStatus;
import nus.iss.smartcart.backend.model.PaymentMethod;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import nus.iss.smartcart.backend.security.CustomUserDetailsService;
import nus.iss.smartcart.backend.security.JwtService;
import nus.iss.smartcart.backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private OrderService orderService;
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
    void checkout_returnsCreatedWithOrderList() throws Exception {
        DeliveryDetails deliveryDetails = DeliveryDetails.builder()
                .firstName("John")
                .lastName("Tan")
                .shippingAddress("12 Rainbow Street")
                .phoneNumber("91234567")
                .build();

        CheckoutResponse orderForMerchantA = CheckoutResponse.builder()
                .orderId(1L)
                .cartItemDetails(List.of())
                .totalAmount(BigDecimal.valueOf(50))
                .orderStatus(OrderStatus.PAID)
                .deliveryDetails(deliveryDetails)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        CheckoutResponse orderForMerchantB = CheckoutResponse.builder()
                .orderId(2L)
                .cartItemDetails(List.of())
                .totalAmount(BigDecimal.valueOf(30))
                .orderStatus(OrderStatus.PAID)
                .deliveryDetails(deliveryDetails)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        CheckoutRequest request = new CheckoutRequest();

        when(orderService.checkout(eq(2L), any())).thenReturn(List.of(orderForMerchantA, orderForMerchantB));

        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].orderId").value(1L))
                .andExpect(jsonPath("$[1].orderId").value(2L));
    }

    @Test
    void getMerchantOrderItems_returnsOkWithResults() throws Exception {
        MerchantOrderItemResponse fakeItem = MerchantOrderItemResponse.builder()
                .orderId(1L)
                .productName("White Tee")
                .size("S")
                .quantity(10)
                .unitPrice(BigDecimal.valueOf(1))
                .subtotal(BigDecimal.valueOf(10))
                .orderStatus("PAID")
                .buyerFirstName("John")
                .buyerLastName("Tan")
                .build();

        when(orderService.getMerchantOrderItems()).thenReturn(List.of(fakeItem));
        mockMvc.perform(get("/api/orders/merchant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productName").value("White Tee"))
                .andExpect(jsonPath("$[0].buyerFirstName").value("John"));
    }
}
