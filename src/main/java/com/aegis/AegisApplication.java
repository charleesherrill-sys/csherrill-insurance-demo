package com.aegis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aegis Claims Platform — application entry point.
 *
 * <p>This is a representative extract of a much larger legacy monolith (see README.md).
 * The subsystems live under {@code com.aegis.*} as separate packages:
 * auth, policy, claims, billing, document, admin, reporting, batch, integration.
 */
@SpringBootApplication
@EnableScheduling
public class AegisApplication {

    public static void main(String[] args) {
        SpringApplication.run(AegisApplication.class, args);
    }
}
