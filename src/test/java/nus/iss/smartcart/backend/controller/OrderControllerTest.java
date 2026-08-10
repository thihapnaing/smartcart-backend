package nus.iss.smartcart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nus.iss.smartcart.backend.dto.CheckoutRequest;
import nus.iss.smartcart.backend.dto.CheckoutResponse;
import nus.iss.smartcart.backend.dto.DeliveryDetails;
import nus.iss.smartcart.backend.model.OrderStatus;
import nus.iss.smartcart.backend.model.PaymentMethod;
import nus.iss.smartcart.backend.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void checkout_returnsCreatedWithOrderData() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        DeliveryDetails deliveryDetails = DeliveryDetails.builder()
                .firstName("John")
                .lastName("Tan")
                .shippingAddress("12 Rainbow Street")
                .phoneNumber("91234567")
                .build();
        CheckoutResponse response = CheckoutResponse.builder()
                .orderId(1L)
                .cartItemDetails(List.of())
                .totalAmount(BigDecimal.ZERO)
                .orderStatus(OrderStatus.PAID)
                .deliveryDetails(deliveryDetails)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();
        when(orderService.checkout(2L,request)).thenReturn(response);
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated());
    }

    @Test
    void getOrderDetail_returnsOKWithOrderData() throws Exception {
        DeliveryDetails deliveryDetails = DeliveryDetails.builder()
                .firstName("John")
                .lastName("Tan")
                .shippingAddress("12 Rainbow Street")
                .phoneNumber("91234567")
                .build();
        CheckoutResponse response = CheckoutResponse.builder()
                .orderId(1L)
                .cartItemDetails(List.of())
                .totalAmount(BigDecimal.ZERO)
                .orderStatus(OrderStatus.PAID)
                .deliveryDetails(deliveryDetails)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();
        when(orderService.getOrderDetail(1L, 2L)).thenReturn(response);
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk());
    }
}
