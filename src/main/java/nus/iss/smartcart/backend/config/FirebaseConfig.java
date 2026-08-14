package nus.iss.smartcart.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
// Tan pang wee - for delivery notification
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebase() throws IOException {

        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        InputStream serviceAccount =
                getClass()
                        .getClassLoader()
                        .getResourceAsStream(
                                "firebase-service-account.json"
                        );

        if (serviceAccount == null) {
            throw new IllegalStateException(
                    "firebase-service-account.json not found"
            );
        }

        try (serviceAccount) {
            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(
                                    GoogleCredentials.fromStream(
                                            serviceAccount
                                    )
                            )
                            .build();

            FirebaseApp.initializeApp(options);
        }

        System.out.println(
                "Firebase Admin initialized"
        );
    }
}