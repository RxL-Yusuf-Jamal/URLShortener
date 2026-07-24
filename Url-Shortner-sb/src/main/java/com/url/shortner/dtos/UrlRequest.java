package com.url.shortner.dtos;

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
}
