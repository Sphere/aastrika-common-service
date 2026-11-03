package org.aastrika.config;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Equivalent of the proxy's firebase-manager.ts: builds the Firebase app from project id, client
 * email and private key only (no client_id / private_key_id needed, like the Node SDK's cert()).
 * The bean is lazy, so the service still starts if the credentials are not configured;
 * only sending fails.
 */
@Configuration
public class FirebaseConfig {

    @Bean
    @Lazy
    public FirebaseMessaging firebaseMessaging(
            @Value("${firebase.project-id}") String projectId,
            @Value("${firebase.client-email}") String clientEmail,
            @Value("${firebase.private-key}") String privateKey) throws IOException {

        FirebaseApp app;
        if (FirebaseApp.getApps().isEmpty()) {
            ServiceAccountCredentials credentials = ServiceAccountCredentials.newBuilder()
                    .setProjectId(projectId)
                    .setClientEmail(clientEmail)
                    .setPrivateKey(parsePrivateKey(privateKey))
                    .build();
            app = FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build());
        } else {
            app = FirebaseApp.getInstance();
        }
        return FirebaseMessaging.getInstance(app);
    }

    /** Parses a PEM "-----BEGIN PRIVATE KEY-----" (PKCS#8) string, with literal "\n" sequences allowed. */
    private static PrivateKey parsePrivateKey(String pem) throws IOException {
        String cleaned = pem == null ? "" : pem.trim();
        if (cleaned.length() > 1 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        cleaned = cleaned
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        if (cleaned.isEmpty()) {
            throw new IOException("firebase.private-key is empty - set FIREBASE_PRIVATE_KEY");
        }
        try {
            byte[] der = Base64.getDecoder().decode(cleaned);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IOException("firebase.private-key is not a valid PKCS#8 private key: " + e.getMessage(), e);
        }
    }
}