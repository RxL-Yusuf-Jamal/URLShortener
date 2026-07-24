package com.url.shortner;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    // Random, test-only secret - never used outside this ephemeral Testcontainers-backed context.
    private static final String TEST_JWT_SECRET =
            "6vAh15HD6P1D3bJtInCBJNWRdC7IRu+IcbKEUQouRmuGCJAL08QPRIyeoNhwf3cRX2aKwyJrI9zs0VI4oTlamQ==";

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("frontend.url", () -> "http://localhost:5173");
    }
}
