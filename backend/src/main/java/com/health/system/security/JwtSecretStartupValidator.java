package com.health.system.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Component
public class JwtSecretStartupValidator {

    private static final Set<String> DISALLOWED_SECRETS = Set.of(
            "healthSystemSecretKeyhealthSystemSecretKey",
            "PLEASE_CHANGE_IN_PRODUCTION",
            "PLEASE_CHANGE_ME_32CHARS_MINIMUM"
    );

    private final Environment environment;

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${security.jwt.fail-fast-on-weak-secret-in-prod:true}")
    private boolean failFastOnWeakSecretInProd;

    @Value("${security.jwt.min-secret-length:32}")
    private int minSecretLength;

    public JwtSecretStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateJwtSecret() {
        if (!failFastOnWeakSecretInProd || !isProdProfileActive()) {
            return;
        }

        String normalized = secret == null ? "" : secret.trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        boolean weak = normalized.isEmpty()
                || normalized.length() < Math.max(32, minSecretLength)
                || DISALLOWED_SECRETS.contains(normalized)
                || upper.contains("PLEASE_CHANGE")
                || upper.contains("CHANGE_ME");

        if (weak) {
            throw new IllegalStateException(
                    "JWT secret is weak or placeholder in production. " +
                    "Set a strong JWT_SECRET (>= 32 chars) before startup."
            );
        }
    }

    private boolean isProdProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
    }
}