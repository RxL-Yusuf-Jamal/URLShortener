package com.url.shortner.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlRequest {

    @NotBlank(message = "Original URL cannot be blank")
    @URL(message = "Original URL must be a valid URL")
    private String originalUrl;

    // Optional: omit (or null) for a link that never expires
    @Min(value = 1, message = "Expiration must be at least 1 day")
    @Max(value = 3650, message = "Expiration cannot exceed 3650 days")
    private Integer expiresInDays;
}
