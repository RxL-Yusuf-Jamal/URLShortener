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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
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
        Map<String, Object> registerRequest = Map.of(
                "username", "redirectflowuser",
                "email", "redirectflow@example.com",
                "password", "password123",
                "role", List.of("ROLE_USER")
        );
        ResponseEntity<String> registerResponse =
                restTemplate.postForEntity("/api/auth/public/register", registerRequest, String.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> loginRequest = Map.of(
                "username", "redirectflowuser",
                "password", "password123"
        );
        ResponseEntity<JwtAuthenticationResponse> loginResponse =
                restTemplate.postForEntity("/api/auth/public/login", loginRequest, JwtAuthenticationResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().getToken();
        assertThat(token).isNotBlank();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        HttpEntity<Map<String, String>> createRequest =
                new HttpEntity<>(Map.of("originalUrl", "https://example.com/very/long/path"), authHeaders);

        ResponseEntity<UrlMappingDTO> createResponse =
                restTemplate.postForEntity("/api/urls/shorten", createRequest, UrlMappingDTO.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String shortUrl = createResponse.getBody().getShortUrl();
        assertThat(shortUrl).hasSize(8);

        // TestRestTemplate auto-follows redirects, which would send this request out to the real
        // example.com; use a plain RestTemplate configured to stop at the 302 instead.
        ResponseEntity<Void> redirectResponse = nonRedirectFollowingRestTemplate()
                .getForEntity("http://localhost:" + port + "/" + shortUrl, Void.class);

        assertThat(redirectResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(redirectResponse.getHeaders().getLocation()).hasToString("https://example.com/very/long/path");

        UrlMapping persisted = urlMappingRepository.findByShortUrl(shortUrl);
        assertThat(persisted.getClickCount()).isEqualTo(1);

        List<ClickEvent> clickEvents = clickEventRepository.findByUrlMappingAndClickDateBetween(
                persisted, persisted.getCreatedDate().minusMinutes(1), persisted.getCreatedDate().plusMinutes(1));
        assertThat(clickEvents).hasSize(1);
    }

    private RestTemplate nonRedirectFollowingRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        return new RestTemplate(requestFactory);
    }
}
