package com.url.shortner;

import com.url.shortner.dtos.UrlMappingDTO;
import com.url.shortner.models.ClickEvent;
import com.url.shortner.models.UrlMapping;
import com.url.shortner.repository.ClickEventRepository;
import com.url.shortner.repository.UrlMappingRepository;
import com.url.shortner.security.jwt.JwtAuthenticationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedirectFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Test
    void createShortUrlThenRedirectIncrementsClickCountAndRecordsClickEvent() {
        String token = registerAndLogin("redirectflowuser");
        UrlMappingDTO created = createShortUrl(token, "https://example.com/very/long/path");
        String shortUrl = created.getShortUrl();
        // Base62 codes are variable-length (derived from the row's id), so assert the alphabet instead of a fixed size.
        assertThat(shortUrl).isNotBlank().matches("[0-9a-zA-Z]+");

        ResponseEntity<Void> redirectResponse = followRedirect(shortUrl);

        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(redirectResponse.getHeaders().getLocation()).hasToString("https://example.com/very/long/path");

        UrlMapping persisted = urlMappingRepository.findByShortUrl(shortUrl);
        assertThat(persisted.getClickCount()).isEqualTo(1);

        List<ClickEvent> clickEvents = clickEventRepository.findByUrlMappingAndClickDateBetween(
                persisted, persisted.getCreatedDate().minusMinutes(1), persisted.getCreatedDate().plusMinutes(1));
        assertThat(clickEvents).hasSize(1);
    }

    @Test
    void updateAfterRedirect_evictsCacheSoNextRedirectServesNewUrl() {
        String token = registerAndLogin("cacheinvalidationuser");
        UrlMappingDTO created = createShortUrl(token, "https://old.example.com");
        String shortUrl = created.getShortUrl();

        // First redirect populates the Redis cache with the old destination.
        ResponseEntity<Void> firstRedirect = followRedirect(shortUrl);
        assertThat(firstRedirect.getHeaders().getLocation()).hasToString("https://old.example.com");

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        HttpEntity<Map<String, String>> updateRequest =
                new HttpEntity<>(Map.of("originalUrl", "https://new.example.com"), authHeaders);
        ResponseEntity<UrlMappingDTO> updateResponse = restTemplate.exchange(
                "/api/urls/" + shortUrl, HttpMethod.PUT, updateRequest, UrlMappingDTO.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // If the cache entry weren't evicted on update, this would still return the stale old URL.
        ResponseEntity<Void> secondRedirect = followRedirect(shortUrl);
        assertThat(secondRedirect.getHeaders().getLocation()).hasToString("https://new.example.com");
    }

    @Test
    void redirectToExpiredLink_returns404() {
        String token = registerAndLogin("expiredlinkuser");
        UrlMappingDTO created = createShortUrl(token, "https://example.com/expired");
        String shortUrl = created.getShortUrl();

        // Bypass the API's @Min(1) validation to simulate a link whose duration has already elapsed.
        UrlMapping mapping = urlMappingRepository.findByShortUrl(shortUrl);
        mapping.setExpiresAt(LocalDateTime.now().minusDays(1));
        urlMappingRepository.save(mapping);

        ResponseEntity<Void> redirectResponse = followRedirect(shortUrl);

        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String registerAndLogin(String username) {
        Map<String, Object> registerRequest = Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", "password123",
                "role", List.of("ROLE_USER")
        );
        ResponseEntity<String> registerResponse =
                restTemplate.postForEntity("/api/auth/public/register", registerRequest, String.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> loginRequest = Map.of("username", username, "password", "password123");
        ResponseEntity<JwtAuthenticationResponse> loginResponse =
                restTemplate.postForEntity("/api/auth/public/login", loginRequest, JwtAuthenticationResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().getToken();
        assertThat(token).isNotBlank();
        return token;
    }

    private UrlMappingDTO createShortUrl(String token, String originalUrl) {
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        HttpEntity<Map<String, String>> createRequest = new HttpEntity<>(Map.of("originalUrl", originalUrl), authHeaders);
        ResponseEntity<UrlMappingDTO> createResponse =
                restTemplate.postForEntity("/api/urls/shorten", createRequest, UrlMappingDTO.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return createResponse.getBody();
    }

    private ResponseEntity<Void> followRedirect(String shortUrl) {
        // TestRestTemplate auto-follows redirects, which would send this request out to a real external
        // host; use a plain RestTemplate configured to stop at the 302 instead.
        return nonRedirectFollowingRestTemplate().getForEntity("http://localhost:" + port + "/" + shortUrl, Void.class);
    }

    private RestTemplate nonRedirectFollowingRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        // Unlike TestRestTemplate, plain RestTemplate throws on 4xx/5xx by default; we want the actual
        // status back (302, 404, ...) as a normal ResponseEntity instead.
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return restTemplate;
    }
}
