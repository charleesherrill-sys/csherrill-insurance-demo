package com.aegis.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Central configuration holder.
 *
 * <p>All integration credentials and sensitive bootstrap values must be supplied
 * via environment variables. The application fails fast on startup if any required
 * secret is absent (no hardcoded fallbacks).
 */
@Component
public class AppConfig {

    @Value("${aegis.documents.root}")
    private String documentsRoot;

    @Value("${aegis.admin.bootstrap.user}")
    private String adminBootstrapUser;

    @Value("${aegis.admin.bootstrap.password}")
    private String adminBootstrapPassword;

    @Value("${aegis.integration.payment-gateway.url}")
    private String paymentGatewayUrl;

    @Value("${aegis.integration.payment-gateway.api-key}")
    private String paymentGatewayApiKey;

    @Value("${aegis.integration.fraud.shared-secret}")
    private String fraudSharedSecret;

    @PostConstruct
    public void validateRequiredSecrets() {
        List<String> missing = new ArrayList<>();
        if (isBlank(adminBootstrapPassword))  missing.add("AEGIS_ADMIN_PASSWORD");
        if (isBlank(adminBootstrapUser))      missing.add("AEGIS_ADMIN_USER");
        if (isBlank(paymentGatewayUrl))       missing.add("AEGIS_PAYMENT_GATEWAY_URL");
        if (isBlank(paymentGatewayApiKey))    missing.add("AEGIS_PAYMENT_GATEWAY_API_KEY");
        if (isBlank(fraudSharedSecret))       missing.add("AEGIS_FRAUD_SHARED_SECRET");
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "Required secret environment variable(s) not set: " + missing
                + ". Set them before starting the application.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public String getDocumentsRoot() {
        return documentsRoot;
    }

    public String getAdminBootstrapUser() {
        return adminBootstrapUser;
    }

    public String getAdminBootstrapPassword() {
        return adminBootstrapPassword;
    }

    public String getPaymentGatewayUrl() {
        return paymentGatewayUrl;
    }

    public String getPaymentGatewayApiKey() {
        return paymentGatewayApiKey;
    }

    public String getFraudSharedSecret() {
        return fraudSharedSecret;
    }
}
