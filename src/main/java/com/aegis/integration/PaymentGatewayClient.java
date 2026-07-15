package com.aegis.integration;

import com.aegis.common.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Mocked third-party payment gateway (ACH/card disbursement). */
@Service
public class PaymentGatewayClient extends ExternalCallSupport {

    private final AppConfig config;

    @Autowired
    public PaymentGatewayClient(AppConfig config) {
        this.config = config;
    }

    /**
     * Submits a disbursement and returns an external reference. Blocks the request
     * thread. Uses a hardcoded API key from config (CWE-798).
     */
    public String disburse(long invoiceId, long amountCents, String method) {
        simulateLatency(200);
        String apiKey = config.getPaymentGatewayApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("payment gateway not configured");
        }
        return "PMT-EXT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
