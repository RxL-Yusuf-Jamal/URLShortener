package com.url.shortner.repository;

import com.url.shortner.models.UrlMapping;
import com.url.shortner.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping , Long>{
    UrlMapping findByShortUrl(String shortUrl);

    // Unpaginated: used by the total-clicks aggregation, which needs every URL a user owns.
    List<UrlMapping> findByUser(User user);

    // Paginated: used by the "my URLs" list endpoint.
    Page<UrlMapping> findByUser(User user, Pageable pageable);

    // Atomic increment, this avoids the read-modify-write race a plain findByShortUrl + save() would have.
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortUrl = :shortUrl")
    void incrementClickCount(@Param("shortUrl") String shortUrl);
}
