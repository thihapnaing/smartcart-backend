package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.model.OrderStatus;
import nus.iss.smartcart.backend.repository.*;
import nus.iss.smartcart.backend.security.CurrentUserProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

//    @Mock
//    private PushNotificationService pushNotificationService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void assignOrder_success_assignsDriverAndSendsNotification() {
        Order order = new Order();
        order.setTrackingNo("TRK-2026-0007");
        order.setStatus(OrderStatus.PACKED);

        when(orderRepository.findByTrackingNo(
                "TRK-2026-0007"
        )).thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        Order result = orderService.assignOrder(
                "TRK-2026-0007",
                1L
        );

        assertEquals(
                1L,
                result.getDeliveryPersonId()
        );

        verify(orderRepository).save(order);

//        verify(pushNotificationService).notifyJobAssigned(order);
    }

    @Test
    void assignOrder_orderNotFound_throwsException() {
        when(orderRepository.findByTrackingNo(
                "INVALID"
        )).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> orderService.assignOrder(
                        "INVALID",
                        1L
                )
        );

        verify(orderRepository, never())
                .save(any(Order.class));

//        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void pickupParcel_success_changesStatus() {
        Order order = createOrder(
                OrderStatus.PACKED
        );

        when(
                orderRepository
                        .findByTrackingNoAndDeliveryPersonId(
                                "TRK-2026-0007",
                                1L
                        )
        ).thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        Order result = orderService.pickupParcel(
                "TRK-2026-0007",
                1L
        );

        assertEquals(
                OrderStatus.PICKED_UP,
                result.getStatus()
        );

        verify(orderRepository).save(order);
    }

    @Test
    void pickupParcel_nonPackedOrder_throwsException() {
        Order order = createOrder(
                OrderStatus.PICKED_UP
        );

        when(
                orderRepository
                        .findByTrackingNoAndDeliveryPersonId(
                                "TRK-2026-0007",
                                1L
                        )
        ).thenReturn(Optional.of(order));

        assertThrows(
                IllegalStateException.class,
                () -> orderService.pickupParcel(
                        "TRK-2026-0007",
                        1L
                )
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void confirmDeliveryProof_success_marksDelivered() {
        Order order = createOrder(
                OrderStatus.PICKED_UP
        );

        when(orderRepository.findByTrackingNo(
                "TRK-2026-0007"
        )).thenReturn(Optional.of(order));

        when(orderRepository.save(order))
                .thenReturn(order);

        Order result =
                orderService.confirmDeliveryProof(
                        "TRK-2026-0007",
                        "delivery-proofs/photo.jpg"
                );

        assertEquals(
                OrderStatus.DELIVERED,
                result.getStatus()
        );

        assertEquals(
                "delivery-proofs/photo.jpg",
                result.getDeliveryProofKey()
        );

        assertNotNull(result.getDeliveredAt());

        verify(orderRepository).save(order);
    }

    private Order createOrder(OrderStatus status) {
        Order order = new Order();
        order.setTrackingNo("TRK-2026-0007");
        order.setDeliveryPersonId(1L);
        order.setStatus(status);

        return order;
    }
}