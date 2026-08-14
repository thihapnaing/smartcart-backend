package nus.iss.smartcart.backend.service;

import org.springframework.stereotype.Service;

import nus.iss.smartcart.backend.model.DeliveryDeviceToken;
import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.repository.DeliveryDeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
public class PushNotificationService {

    private final DeliveryDeviceTokenRepository tokenRepository;

    public PushNotificationService(
            DeliveryDeviceTokenRepository tokenRepository
    ) {
        this.tokenRepository = tokenRepository;
    }

    // requires src/main/resources/firebase-service-account.json
    public void notifyJobAssigned(Order order) {

        DeliveryDeviceToken device =
                tokenRepository
                        .findByDeliveryPersonId(
                                order.getDeliveryPersonId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No FCM token for delivery person: " +
                                                order.getDeliveryPersonId()
                                )
                        );

        try {
            Message message =
                    Message.builder()
                            .setToken(device.getFcmToken())
                            .setNotification(
                                    Notification.builder()
                                            .setTitle("New job assigned")
                                            .setBody(
                                                    "Order " +
                                                            order.getTrackingNo() +
                                                            " has been assigned to you"
                                            )
                                            .build()
                            )
                            .putData(
                                    "trackingNo",
                                    order.getTrackingNo()
                            )
                            .build();

            String messageId =
                    FirebaseMessaging
                            .getInstance()
                            .send(message);

            System.out.println(
                    "FCM notification sent: " + messageId
            );

        } catch (FirebaseMessagingException e) {
            System.err.println(
                    "FCM notification failed: " +
                            e.getMessagingErrorCode()
            );

            e.printStackTrace();

            throw new RuntimeException(
                    "FCM notification failed",
                    e
            );
        }
    }
}