package com.url.shortner.controllers;

import com.url.shortner.dtos.LoginRequest;
import com.url.shortner.dtos.RegisterRequest;
import com.url.shortner.models.User;
import com.url.shortner.security.jwt.JwtUtils;
import com.url.shortner.service.RateLimiterService;
import com.url.shortner.service.TokenBlacklistService;
import com.url.shortner.service.UserService;
import com.url.shortner.util.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "User registration, login, and logout")
public class AuthController {

    private final UserService userService;

    private final RateLimiterService rateLimiterService;

    private final JwtUtils jwtUtils;

    private final TokenBlacklistService tokenBlacklistService;

    @Operation(summary = "Log in with username and password, receive a JWT")
    @PostMapping("/public/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        rateLimiterService.checkLoginLimit(ClientIpResolver.resolve(request), loginRequest.getUsername());
        return ResponseEntity.ok(userService.authenticateUser(loginRequest));
    }

    @Operation(summary = "Log out, revoking the current JWT server-side so it can no longer be used")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String token = jwtUtils.getJwtFromHeader(request);
        if (token != null) {
            Date expiration = jwtUtils.getExpirationFromJwtToken(token);
            Duration remainingValidity = Duration.between(Instant.now(), expiration.toInstant());
            tokenBlacklistService.blacklist(token, remainingValidity);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Register a new user account")
    @PostMapping("/public/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setEmail(registerRequest.getEmail());
        user.setRole("ROLE_USER");
        userService.registerUser(user);
        return ResponseEntity.ok("User registered Successfully");

    }
}
