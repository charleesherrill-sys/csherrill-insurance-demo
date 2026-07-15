package com.aegis.integration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Mocked member notification service (email/SMS).
 *
 * <p>Uses the pinned, known-vulnerable log4j-core 2.14.1 (Log4Shell, CVE-2021-44228)
 * as its audit logger. Intentional — see REVIEW.md and pom.xml.
 */
@Service
public class NotificationService extends ExternalCallSupport {

    private static final Logger LOG = LogManager.getLogger(NotificationService.class);

    public void notifyMember(long memberUserId, String message) {
        simulateLatency(80);
        // Message content is logged verbatim through log4j (the vulnerable sink).
        LOG.info("notify member {}: {}", memberUserId, message);
    }
}
