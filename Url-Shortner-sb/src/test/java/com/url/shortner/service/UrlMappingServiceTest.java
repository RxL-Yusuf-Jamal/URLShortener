package com.url.shortner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.url.shortner.dtos.ClickEventDTO;
import com.url.shortner.dtos.UrlMappingDTO;
import com.url.shortner.models.ClickEvent;
import com.url.shortner.models.UrlMapping;
import com.url.shortner.models.User;
import com.url.shortner.repository.ClickEventRepository;
import com.url.shortner.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlMappingServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private UrlMappingService urlMappingService;

    private User owner;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        // lenient: only tests that exercise getOriginalUrl's cache path actually invoke opsForValue()
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Real instance (not mocked) - it's simple, in-memory, and each test creates a fresh one with
        // full bucket capacity, so no test exercises more calls than the limit allows.
        urlMappingService = new UrlMappingService(
                urlMappingRepository, clickEventRepository, redisTemplate, objectMapper, new RateLimiterService());
        owner = new User();
        owner.setId(1L);
        owner.setUsername("alice");
    }

    @Test
    void createShortUrl_derivesShortUrlFromGeneratedIdAndReturnsDto() {
        // Simulates the IDENTITY column assigning an id on the first insert - no existence
        // check is needed since the short code is deterministically derived from that id.
        when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping mapping = invocation.getArgument(0);
            if (mapping.getId() == null) {
                mapping.setId(125L);
            }
            return mapping;
        });

        UrlMappingDTO dto = urlMappingService.createShortUrl("https://example.com", null, owner);

        assertThat(dto.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(dto.getShortUrl()).isEqualTo("21");
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getClickCount()).isZero();
        assertThat(dto.getExpiresAt()).isNull();
        verify(urlMappingRepository, times(2)).save(any(UrlMapping.class));
    }

    @Test
    void createShortUrl_withExpiresInDays_computesExpiresAt() {
        when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping mapping = invocation.getArgument(0);
            if (mapping.getId() == null) {
                mapping.setId(1L);
            }
            return mapping;
        });

        LocalDateTime before = LocalDateTime.now();
        UrlMappingDTO dto = urlMappingService.createShortUrl("https://example.com", 7, owner);
        LocalDateTime after = LocalDateTime.now();

        assertThat(dto.getExpiresAt()).isNotNull();
        assertThat(dto.getExpiresAt()).isAfterOrEqualTo(before.plusDays(7));
        assertThat(dto.getExpiresAt()).isBeforeOrEqualTo(after.plusDays(7));
    }

    @Test
    void getUrlsByUser_mapsEntitiesToDtos() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 5, null);
        when(urlMappingRepository.findByUser(owner)).thenReturn(List.of(mapping));

        List<UrlMappingDTO> result = urlMappingService.getUrlsByUser(owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShortUrl()).isEqualTo("abc12345");
        assertThat(result.get(0).getClickCount()).isEqualTo(5);
    }

    @Test
    void getClickEventsByDate_groupsClicksByDay() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 2, null);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 3, 0, 0);

        ClickEvent day1First = clickEvent(mapping, LocalDateTime.of(2026, 1, 1, 10, 0));
        ClickEvent day1Second = clickEvent(mapping, LocalDateTime.of(2026, 1, 1, 14, 0));
        ClickEvent day2 = clickEvent(mapping, LocalDateTime.of(2026, 1, 2, 9, 0));

        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);
        when(clickEventRepository.findByUrlMappingAndClickDateBetween(mapping, start, end))
                .thenReturn(List.of(day1First, day1Second, day2));

        List<ClickEventDTO> result = urlMappingService.getClickEventsByDate("abc12345", start, end);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(dto -> {
            assertThat(dto.getClickDate()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(dto.getCount()).isEqualTo(2L);
        });
        assertThat(result).anySatisfy(dto -> {
            assertThat(dto.getClickDate()).isEqualTo(LocalDate.of(2026, 1, 2));
            assertThat(dto.getCount()).isEqualTo(1L);
        });
    }

    @Test
    void getClickEventsByDate_returnsNullWhenShortUrlNotFound() {
        when(urlMappingRepository.findByShortUrl("missing")).thenReturn(null);

        List<ClickEventDTO> result = urlMappingService.getClickEventsByDate("missing",
                LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(result).isNull();
    }

    @Test
    void getOriginalUrl_onCacheMiss_fetchesFromDbAndPopulatesCache() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 4, null);
        when(valueOperations.get("urlmapping:abc12345")).thenReturn(null);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        UrlMapping result = urlMappingService.getOriginalUrl("abc12345");

        assertThat(result).isEqualTo(mapping);
        verify(urlMappingRepository).incrementClickCount("abc12345");
        verify(urlMappingRepository, never()).save(any());
        verify(valueOperations).set(eq("urlmapping:abc12345"), anyString(), any());

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(captor.capture());
        assertThat(captor.getValue().getUrlMapping()).isEqualTo(mapping);
    }

    @Test
    void getOriginalUrl_onCacheHit_skipsDatabaseReadButStillRecordsClick() {
        when(valueOperations.get("urlmapping:abc12345"))
                .thenReturn("{\"id\":1,\"originalUrl\":\"https://example.com\",\"expiresAt\":null}");

        UrlMapping result = urlMappingService.getOriginalUrl("abc12345");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOriginalUrl()).isEqualTo("https://example.com");
        verify(urlMappingRepository, never()).findByShortUrl(any());
        verify(urlMappingRepository).incrementClickCount("abc12345");
        verify(clickEventRepository).save(any(ClickEvent.class));
    }

    @Test
    void getOriginalUrl_whenDbEntryExpired_returnsNullWithoutRecordingOrCaching() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 0,
                LocalDateTime.now().minusDays(1));
        when(valueOperations.get("urlmapping:abc12345")).thenReturn(null);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        UrlMapping result = urlMappingService.getOriginalUrl("abc12345");

        assertThat(result).isNull();
        verify(urlMappingRepository, never()).incrementClickCount(any());
        verify(clickEventRepository, never()).save(any());
        verify(valueOperations, never()).set(anyString(), anyString(), any());
    }

    @Test
    void getOriginalUrl_whenCachedEntryExpired_returnsNullAndEvictsCache() {
        String expiredJson = "{\"id\":1,\"originalUrl\":\"https://example.com\",\"expiresAt\":\""
                + LocalDateTime.now().minusDays(1) + "\"}";
        when(valueOperations.get("urlmapping:abc12345")).thenReturn(expiredJson);

        UrlMapping result = urlMappingService.getOriginalUrl("abc12345");

        assertThat(result).isNull();
        verify(urlMappingRepository, never()).findByShortUrl(any());
        verify(urlMappingRepository, never()).incrementClickCount(any());
        verify(clickEventRepository, never()).save(any());
        verify(redisTemplate).delete("urlmapping:abc12345");
    }

    @Test
    void getOriginalUrl_returnsNullAndSkipsWritesWhenNotFound() {
        when(valueOperations.get("urlmapping:missing")).thenReturn(null);
        when(urlMappingRepository.findByShortUrl("missing")).thenReturn(null);

        UrlMapping result = urlMappingService.getOriginalUrl("missing");

        assertThat(result).isNull();
        verify(urlMappingRepository, never()).incrementClickCount(any());
        verify(urlMappingRepository, never()).save(any());
        verify(clickEventRepository, never()).save(any());
    }

    @Test
    void deleteUrlMapping_deletesClickEventsAndMappingForOwnerAndEvictsCache() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 0, null);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        urlMappingService.deleteUrlMapping("abc12345", owner);

        verify(clickEventRepository).deleteByUrlMapping(mapping);
        verify(urlMappingRepository).delete(mapping);
        verify(redisTemplate).delete("urlmapping:abc12345");
    }

    @Test
    void deleteUrlMapping_throwsWhenShortUrlNotFound() {
        when(urlMappingRepository.findByShortUrl("missing")).thenReturn(null);

        assertThatThrownBy(() -> urlMappingService.deleteUrlMapping("missing", owner))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteUrlMapping_throwsWhenCallerIsNotOwner() {
        User otherUser = new User();
        otherUser.setId(2L);
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 0, null);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        assertThatThrownBy(() -> urlMappingService.deleteUrlMapping("abc12345", otherUser))
                .isInstanceOf(SecurityException.class);

        verify(urlMappingRepository, never()).delete(any());
    }

    @Test
    void updateOriginalUrl_updatesAndReturnsDtoForOwnerAndEvictsCache() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://old.com", owner, 0, null);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        UrlMappingDTO dto = urlMappingService.updateOriginalUrl("abc12345", "https://new.com", owner);

        assertThat(dto.getOriginalUrl()).isEqualTo("https://new.com");
        verify(urlMappingRepository).save(mapping);
        verify(redisTemplate).delete("urlmapping:abc12345");
    }

    @Test
    void updateOriginalUrl_throwsWhenCallerIsNotOwner() {
        User otherUser = new User();
        otherUser.setId(2L);
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://old.com", owner, 0, null);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        assertThatThrownBy(() -> urlMappingService.updateOriginalUrl("abc12345", "https://new.com", otherUser))
                .isInstanceOf(SecurityException.class);

        verify(urlMappingRepository, never()).save(any());
    }

    private UrlMapping urlMapping(Long id, String shortUrl, String originalUrl, User user, int clickCount,
                                   LocalDateTime expiresAt) {
        UrlMapping mapping = new UrlMapping();
        mapping.setId(id);
        mapping.setShortUrl(shortUrl);
        mapping.setOriginalUrl(originalUrl);
        mapping.setUser(user);
        mapping.setClickCount(clickCount);
        mapping.setCreatedDate(LocalDateTime.now());
        mapping.setExpiresAt(expiresAt);
        return mapping;
    }

    private ClickEvent clickEvent(UrlMapping mapping, LocalDateTime clickDate) {
        ClickEvent event = new ClickEvent();
        event.setUrlMapping(mapping);
        event.setClickDate(clickDate);
        return event;
    }
}
