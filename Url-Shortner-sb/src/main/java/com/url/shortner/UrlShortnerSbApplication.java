package com.url.shortner;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "SnapURL API",
                version = "1.0",
                description = "URL shortening service with click analytics, link expiration, Redis caching, and rate limiting."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@SpringBootApplication
public class UrlShortnerSbApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortnerSbApplication.class, args);
	}

}
