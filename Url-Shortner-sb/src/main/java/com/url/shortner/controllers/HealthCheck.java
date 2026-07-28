package com.url.shortner.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Basic liveness check")
public class HealthCheck {

    @Operation(summary = "Liveness check")
    @GetMapping("/health" )
    public String healthCheck(){
        return "OK";
    }
}
