package com.laborscope.repository;

// Imports for JPA and CrawledPage
import com.laborscope.model.CrawledPage;
import org.springframework.data.jpa.repository.JpaRepository;

// Built-in functionality imports
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// This repository is a part of the entity-repository model for CrawledPage
public interface CrawledPageRepository extends JpaRepository<CrawledPage, Long>{
    // Check if a URL has already been processed to avoid duplicates
    boolean existsByUrl(String url);
    // Search for a page by URL for content or timestamp update
    Optional<CrawledPage> findByUrl(String url);
    // Find pages containing specific a specific key word (ignoring case)
    List<CrawledPage> findByTitleContains(String keyword);
    // Find pages crawled before a given date
    List<CrawledPage> findByCrawledAtBefore(LocalDateTime date);
    // Find pages discovered at a specific depth
    List<CrawledPage> findByDepth(int depth);
}
