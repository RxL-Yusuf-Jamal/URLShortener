package com.url.shortner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.shortner.cache.CachedUrlMapping;
import com.url.shortner.dtos.ClickEventDTO;
import com.url.shortner.dtos.PagedResponse;
import com.url.shortner.dtos.UrlMappingDTO;
import com.url.shortner.models.ClickEvent;
import com.url.shortner.models.UrlMapping;
import com.url.shortner.models.User;
import com.url.shortner.repository.ClickEventRepository;
import com.url.shortner.repository.UrlMappingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UrlMappingService {

    private static final Logger log = LoggerFactory.getLogger(UrlMappingService.class);

    private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String CACHE_KEY_PREFIX = "urlmapping:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final UrlMappingRepository urlMappingRepository;

    private final ClickEventRepository clickEventRepository;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final RateLimiterService rateLimiterService;

    // Create a short URL for the given original URL and user.
    // The id (and therefore the short code derived from it) is assigned by the database's ownn
    // identity column, so two concurrent creates can never collide, so no existence check or retry is needed.
    @Transactional
    public UrlMappingDTO createShortUrl(String originalUrl, Integer expiresInDays, User user) {
        rateLimiterService.checkShortenLimit(user.getUsername());

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedDate(LocalDateTime.now());
        if (expiresInDays != null) {
            urlMapping.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        }
        UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);

        savedUrlMapping.setShortUrl(encodeBase62(savedUrlMapping.getId()));
        savedUrlMapping = urlMappingRepository.save(savedUrlMapping);

        return convertToDto(savedUrlMapping);
    }

    private String encodeBase62(long id) {
        if (id == 0) {
            return String.valueOf(BASE62_ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        long value = id;
        while (value > 0) {
            sb.append(BASE62_ALPHABET.charAt((int) (value % 62)));
            value /= 62;
        }
        return sb.reverse().toString();
    }

    // Convert UrlMapping entity to UrlMappingDTO
    private UrlMappingDTO convertToDto(UrlMapping urlMapping){
        UrlMappingDTO urlMappingDTO = new UrlMappingDTO();
        urlMappingDTO.setId(urlMapping.getId());
        urlMappingDTO.setOriginalUrl(urlMapping.getOriginalUrl());
        urlMappingDTO.setShortUrl(urlMapping.getShortUrl());
        urlMappingDTO.setClickCount(urlMapping.getClickCount());
        urlMappingDTO.setCreatedDate(urlMapping.getCreatedDate());
        urlMappingDTO.setExpiresAt(urlMapping.getExpiresAt());
        urlMappingDTO.setUsername(urlMapping.getUser().getUsername());
        return urlMappingDTO;
    }

    // Get a page of URLs for a specific user, most recently created first
    public PagedResponse<UrlMappingDTO> getUrlsByUser(User user, Pageable pageable) {
        Page<UrlMapping> page = urlMappingRepository.findByUser(user, pageable);
        List<UrlMappingDTO> content = page.getContent().stream().map(this::convertToDto).toList();
        return new PagedResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    // Get click events for a specific short URL in a date range - only the owner may view them.
    public List<ClickEventDTO> getClickEventsByDate(String shortUrl, LocalDateTime start, LocalDateTime end, User user) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);

        if (urlMapping == null) {
            throw new NoSuchElementException("URL mapping not found");
        }

        if (urlMapping.getUser().getId() != user.getId()) {
            throw new SecurityException("You don't have permission to view analytics for this URL");
        }

        return clickEventRepository.findByUrlMappingAndClickDateBetween(urlMapping , start , end)
                .stream().collect(Collectors.groupingBy(click -> click.getClickDate().toLocalDate(),Collectors.counting()))
                .entrySet().stream()
                .map(entry ->
                {
                    ClickEventDTO clickEventDTO = new ClickEventDTO();
                    clickEventDTO.setClickDate(entry.getKey());
                    clickEventDTO.setCount(entry.getValue());
                    return clickEventDTO;
                })
                .collect(Collectors.toList());
    }

    // Get total clicks for a user in a date range
    public Map<LocalDate, Long> getTotalClicksByUserAndDate(User user, LocalDate start, LocalDate end) {
        List<UrlMapping> urlMappings = urlMappingRepository.findByUser(user);
        List<ClickEvent> clickEvents = clickEventRepository.findByUrlMappingInAndClickDateBetween(urlMappings , start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        return clickEvents.stream().collect(Collectors.groupingBy(click -> click.getClickDate().toLocalDate(), Collectors.counting()));
    }

    // Retrieve original URL and increment click count.
    // The returned urlMapping's in-memory clickCount is intentionally left at its pre-increment value:
    // nothing reads it from this return value today, and setting it here would let Hibernate's dirty
    // checking flush a plain assignment over the atomic increment below, silently reintroducing the race.
    // Cache-aside: a hit skips the DB read of the mapping itself, but the click count increment and
    // click event recording below still run on every call, hit or miss - caching must never affect analytics.
    @Transactional
    public UrlMapping getOriginalUrl(String shortUrl) {
        UrlMapping urlMapping;
        CachedUrlMapping cached = getFromCache(shortUrl);

        if (cached != null) {
            if (isExpired(cached.getExpiresAt())) {
                evictFromCache(shortUrl);
                return null;
            }
            urlMapping = new UrlMapping();
            urlMapping.setId(cached.getId());
            urlMapping.setOriginalUrl(cached.getOriginalUrl());
        } else {
            urlMapping = urlMappingRepository.findByShortUrl(shortUrl);
            if (urlMapping == null) {
                return null;
            }
            if (isExpired(urlMapping.getExpiresAt())) {
                return null;
            }
            putInCache(shortUrl, urlMapping);
        }

        urlMappingRepository.incrementClickCount(shortUrl);

        // record Click event

        ClickEvent clickEvent =  new ClickEvent();
        clickEvent.setClickDate(LocalDateTime.now());
        clickEvent.setUrlMapping(urlMapping);
        clickEventRepository.save(clickEvent);

        return urlMapping;
    }

    private boolean isExpired(LocalDateTime expiresAt) {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    // Cache reads/writes are best-effort: if Redis is slow, misconfigured, or down, we fall back
    // to the database rather than let a caching optimization take the whole redirect path down with it.
    private CachedUrlMapping getFromCache(String shortUrl) {
        try {
            String json = redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + shortUrl);
            return json != null ? objectMapper.readValue(json, CachedUrlMapping.class) : null;
        } catch (Exception e) {
            log.warn("Redis cache read failed for shortUrl={}, falling back to DB", shortUrl, e);
            return null;
        }
    }

    private void putInCache(String shortUrl, UrlMapping urlMapping) {
        try {
            CachedUrlMapping cached = new CachedUrlMapping(
                    urlMapping.getId(), urlMapping.getOriginalUrl(), urlMapping.getExpiresAt());
            redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + shortUrl, objectMapper.writeValueAsString(cached), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis cache write failed for shortUrl={}", shortUrl, e);
        }
    }

    private void evictFromCache(String shortUrl) {
        try {
            redisTemplate.delete(CACHE_KEY_PREFIX + shortUrl);
        } catch (Exception e) {
            log.warn("Redis cache evict failed for shortUrl={}", shortUrl, e);
        }
    }

    // Delete URL mapping along with its click events
    @Transactional  // Ensure all deletions happen in a single transaction
    public void deleteUrlMapping(String shortUrl, User user) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);

        if (urlMapping == null) {
            throw new NoSuchElementException("URL mapping not found");
        }

        if (urlMapping.getUser().getId() != user.getId()) {
            throw new SecurityException("You don't have permission to delete this URL");
        }

        // Delete all click events related to this URL
        clickEventRepository.deleteByUrlMapping(urlMapping);

        // Delete the URL mapping itself
        urlMappingRepository.delete(urlMapping);

        // Otherwise a cached entry could keep serving redirects for a link that was just deleted.
        evictFromCache(shortUrl);
    }

    // Update the original URL (and optionally the expiration) for a given short URL.
    // A null expiresInDays leaves the existing expiration untouched, not cleared.
    public UrlMappingDTO updateOriginalUrl(String shortUrl, String newOriginalUrl, Integer expiresInDays, User user) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl);

        if (urlMapping == null) {
            throw new NoSuchElementException("URL mapping not found");
        }

        if (urlMapping.getUser().getId() != user.getId()) {
            throw new SecurityException("You don't have permission to update this URL");
        }

        urlMapping.setOriginalUrl(newOriginalUrl);
        if (expiresInDays != null) {
            urlMapping.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        }
        urlMappingRepository.save(urlMapping);

        // Otherwise a cached entry would keep serving the old originalUrl/expiresAt until the cache TTL expires.
        evictFromCache(shortUrl);

        return convertToDto(urlMapping);
    }
}
