package com.url.shortner.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

// Enables @PreAuthorize enforcement inside @WebMvcTest slices, which skip the real WebSecurityConfig by default.
@TestConfiguration
@EnableMethodSecurity
public class MethodSecurityTestConfig {
}
