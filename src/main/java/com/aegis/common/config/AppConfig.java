package com.aegis.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Central configuration holder.
 *
 * <p>SECURITY (CWE-798): the integration API key, fraud shared secret, and admin
 * bootstrap password are injected from environment-backed properties with no
 * insecure defaults (see {@code application.properties}). If a required secret is
 * missing, Spring placeholder resolution fails and the application will not start.
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
