package com.health.system.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretStartupValidatorTest {

    @Test
    void shouldThrowInProdWhenSecretIsPlaceholder() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(env);
        ReflectionTestUtils.setField(validator, "secret", "PLEASE_CHANGE_IN_PRODUCTION");
        ReflectionTestUtils.setField(validator, "failFastOnWeakSecretInProd", true);
        ReflectionTestUtils.setField(validator, "minSecretLength", 32);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateJwtSecret);
        assertNotNull(ex);
    }

    @Test
    void shouldPassInDevEvenWhenSecretIsWeak() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(env);
        ReflectionTestUtils.setField(validator, "secret", "short");
        ReflectionTestUtils.setField(validator, "failFastOnWeakSecretInProd", true);
        ReflectionTestUtils.setField(validator, "minSecretLength", 32);

        assertDoesNotThrow(validator::validateJwtSecret);
    }

    @Test
    void shouldPassInProdWhenSecretIsStrong() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(env);
        ReflectionTestUtils.setField(validator, "secret", "f3f0a9439b6a4cb1a95d6f64b0d8e5ac");
        ReflectionTestUtils.setField(validator, "failFastOnWeakSecretInProd", true);
        ReflectionTestUtils.setField(validator, "minSecretLength", 32);

        assertDoesNotThrow(validator::validateJwtSecret);
    }
}
