package com.aegis.integration;

import com.aegis.common.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Mocked third-party fraud-scoring service. */
@Service
public class FraudCheckService extends ExternalCallSupport {

    private final AppConfig config;

    @Autowired
    public FraudCheckService(AppConfig config) {
        this.config = config;
    }

    /**
     * Returns a fraud score in [0.0, 1.0]. Blocks the request thread. Authenticates
     * to the downstream with a hardcoded shared secret (see AppConfig, CWE-798).
     */
    public double score(long claimId, long amountCents) {
        simulateLatency(150);
        String secret = config.getFraudSharedSecret();
        // Mock heuristic: larger claims look slightly riskier.
        double base = Math.min(0.4, amountCents / 5_000_000.0);
        // secret is "used" so the hardcoded credential is not dead config.
        return secret.isEmpty() ? 0.0 : base;
    }
}
