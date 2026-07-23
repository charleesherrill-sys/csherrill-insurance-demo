package com.aegis.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Central configuration holder.
 *
 * <p>The integration credentials and admin bootstrap password are supplied from
 * environment-backed properties with no insecure defaults (CWE-798 remediation).
 * {@link #validate()} additionally rejects blank values so the application fails
 * fast at startup if a required secret is missing or empty.
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
    void validate() {
        requireConfigured("aegis.admin.bootstrap.password", adminBootstrapPassword);
        requireConfigured("aegis.integration.payment-gateway.api-key", paymentGatewayApiKey);
        requireConfigured("aegis.integration.fraud.shared-secret", fraudSharedSecret);
    }

    private static void requireConfigured(String property, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Required secret '" + property + "' is not configured. "
                    + "Set the corresponding environment variable; there is no default.");
        }
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
