package com.url.shortner.controllers;

import com.url.shortner.dtos.ClickEventDTO;
import com.url.shortner.dtos.UrlMappingDTO;
import com.url.shortner.dtos.UrlRequest;
import com.url.shortner.models.ClickEvent;
import com.url.shortner.models.User;
import com.url.shortner.service.UrlMappingService;
import com.url.shortner.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/urls")
@Validated
@RequiredArgsConstructor
@Tag(name = "URL Management", description = "Create, list, update, delete, and analyze short URLs")
@SecurityRequirement(name = "bearerAuth")
public class UrlMappingController {

    private final UrlMappingService urlMappingService;

    private final UserService userService;

    @Operation(summary = "Create a short URL, optionally with an expiration duration")
    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UrlMappingDTO> createShortUrl(@Valid @RequestBody UrlRequest request,
                                                        Principal principal) {
        User user = userService.findByUsername(principal.getName());
        UrlMappingDTO urlMappingDTO = urlMappingService.createShortUrl(request.getOriginalUrl(), request.getExpiresInDays(), user);
        return ResponseEntity.ok(urlMappingDTO);
    }

    @Operation(summary = "List all short URLs owned by the logged-in user")
    @GetMapping("/myurls")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UrlMappingDTO>> getUserUrls(Principal principal) {
        User user = userService.findByUsername(principal.getName());
        List<UrlMappingDTO> urls = urlMappingService.getUrlsByUser(user);
        return ResponseEntity.ok(urls);

    }

    @Operation(summary = "Get per-day click counts for a short URL within a date range")
    @GetMapping("/analytics/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClickEventDTO>> getUrlAnalytics(@PathVariable @NotBlank String shortUrl,
                                                               @RequestParam("startDate") String startDate,
                                                               @RequestParam("endDate") String endDate
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        List<ClickEventDTO> clickEventDTOS = urlMappingService.getClickEventsByDate(shortUrl, start, end);
        return ResponseEntity.ok(clickEventDTOS);
    }

    @Operation(summary = "Get total clicks per day across all the logged-in user's URLs within a date range")
    @GetMapping("/totalClicks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<LocalDate, Long>> getTotalClicksByDate(Principal principal,
                                                                     @RequestParam("startDate") String startDate,
                                                                     @RequestParam("endDate") String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        User user = userService.findByUsername(principal.getName());
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        Map<LocalDate, Long> totalClicks = urlMappingService.getTotalClicksByUserAndDate(user, start, end);
        return ResponseEntity.ok(totalClicks);
    }

    @Operation(summary = "Delete a short URL owned by the logged-in user")
    @DeleteMapping("/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> deleteShortUrl(@PathVariable @NotBlank String shortUrl,
                                                              Principal principal) {
        User user = userService.findByUsername(principal.getName());
        urlMappingService.deleteUrlMapping(shortUrl, user);

        // Return a success message
        Map<String, String> response = new HashMap<>();
        response.put("message", "Short URL deleted successfully");
        response.put("shortUrl", shortUrl);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update the destination URL for an existing short URL, keeping the short code unchanged")
    @PutMapping("/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UrlMappingDTO> updateOriginalUrl(@PathVariable @NotBlank String shortUrl,
                                                           @Valid @RequestBody UrlRequest request,
                                                           Principal principal) {
        User user = userService.findByUsername(principal.getName());
        UrlMappingDTO updatedUrlMapping = urlMappingService.updateOriginalUrl(shortUrl, request.getOriginalUrl(), user);
        return ResponseEntity.ok(updatedUrlMapping);
    }

}
