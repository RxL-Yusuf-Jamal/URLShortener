package com.url.shortner.service;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private UrlMappingService urlMappingService;

    private User owner;

    @BeforeEach
    void setUp() {
        urlMappingService = new UrlMappingService(urlMappingRepository, clickEventRepository);
        owner = new User();
        owner.setId(1L);
        owner.setUsername("alice");
    }

    @Test
    void createShortUrl_savesMappingAndReturnsDto() {
        when(urlMappingRepository.existsByShortUrl(anyString())).thenReturn(false);
        when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UrlMappingDTO dto = urlMappingService.createShortUrl("https://example.com", owner);

        assertThat(dto.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(dto.getShortUrl()).hasSize(8);
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getClickCount()).isZero();
        verify(urlMappingRepository).save(any(UrlMapping.class));
    }

    @Test
    void createShortUrl_retriesUntilShortUrlIsUnique() {
        when(urlMappingRepository.existsByShortUrl(anyString())).thenReturn(true, true, false);
        when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        urlMappingService.createShortUrl("https://example.com", owner);

        verify(urlMappingRepository, times(3)).existsByShortUrl(anyString());
    }

    @Test
    void getUrlsByUser_mapsEntitiesToDtos() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 5);
        when(urlMappingRepository.findByUser(owner)).thenReturn(List.of(mapping));

        List<UrlMappingDTO> result = urlMappingService.getUrlsByUser(owner);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShortUrl()).isEqualTo("abc12345");
        assertThat(result.get(0).getClickCount()).isEqualTo(5);
    }

    @Test
    void getClickEventsByDate_groupsClicksByDay() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 2);
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
    void getOriginalUrl_incrementsClickCountAndRecordsClickEvent() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 4);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        UrlMapping result = urlMappingService.getOriginalUrl("abc12345");

        assertThat(result.getClickCount()).isEqualTo(5);
        verify(urlMappingRepository).save(mapping);

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(captor.capture());
        assertThat(captor.getValue().getUrlMapping()).isEqualTo(mapping);
    }

    @Test
    void getOriginalUrl_returnsNullAndSkipsWritesWhenNotFound() {
        when(urlMappingRepository.findByShortUrl("missing")).thenReturn(null);

        UrlMapping result = urlMappingService.getOriginalUrl("missing");

        assertThat(result).isNull();
        verify(urlMappingRepository, never()).save(any());
        verify(clickEventRepository, never()).save(any());
    }

    @Test
    void deleteUrlMapping_deletesClickEventsAndMappingForOwner() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 0);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        urlMappingService.deleteUrlMapping("abc12345", owner);

        verify(clickEventRepository).deleteByUrlMapping(mapping);
        verify(urlMappingRepository).delete(mapping);
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
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://example.com", owner, 0);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        assertThatThrownBy(() -> urlMappingService.deleteUrlMapping("abc12345", otherUser))
                .isInstanceOf(SecurityException.class);

        verify(urlMappingRepository, never()).delete(any());
    }

    @Test
    void updateOriginalUrl_updatesAndReturnsDtoForOwner() {
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://old.com", owner, 0);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        UrlMappingDTO dto = urlMappingService.updateOriginalUrl("abc12345", "https://new.com", owner);

        assertThat(dto.getOriginalUrl()).isEqualTo("https://new.com");
        verify(urlMappingRepository).save(mapping);
    }

    @Test
    void updateOriginalUrl_throwsWhenCallerIsNotOwner() {
        User otherUser = new User();
        otherUser.setId(2L);
        UrlMapping mapping = urlMapping(1L, "abc12345", "https://old.com", owner, 0);
        when(urlMappingRepository.findByShortUrl("abc12345")).thenReturn(mapping);

        assertThatThrownBy(() -> urlMappingService.updateOriginalUrl("abc12345", "https://new.com", otherUser))
                .isInstanceOf(SecurityException.class);

        verify(urlMappingRepository, never()).save(any());
    }

    private UrlMapping urlMapping(Long id, String shortUrl, String originalUrl, User user, int clickCount) {
        UrlMapping mapping = new UrlMapping();
        mapping.setId(id);
        mapping.setShortUrl(shortUrl);
        mapping.setOriginalUrl(originalUrl);
        mapping.setUser(user);
        mapping.setClickCount(clickCount);
        mapping.setCreatedDate(LocalDateTime.now());
        return mapping;
    }

    private ClickEvent clickEvent(UrlMapping mapping, LocalDateTime clickDate) {
        ClickEvent event = new ClickEvent();
        event.setUrlMapping(mapping);
        event.setClickDate(clickDate);
        return event;
    }
}
