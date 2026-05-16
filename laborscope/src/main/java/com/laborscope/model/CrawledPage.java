package com.laborscope.model;

// JPA imports
import java.time.LocalDateTime;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

// This entity is a part of a entity-repository model for cralwed pages
@Entity
public class CrawledPage {
    // Generate primary key for eventual PostgreSQL usage
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // PostgreSQL fields
    private Long id;
    @Column(unique = true, length = 2048)
    private String url;
    @Column(length = 500)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String urlContent;
    private LocalDateTime crawledAt;
    private int depth;

    // Default constructor
    protected CrawledPage() {}
    // Initializer for class
    public CrawledPage(String url, String title, String urlContent, LocalDateTime crawledAt, int depth)
    {
        this.url = url;
        this.title = title;
        this.urlContent = urlContent;
        this.crawledAt = crawledAt;
        this.depth = depth;
    }
    // Simple getters
    public Long getId()
    {
        return id;
    }

    public String getUrl()
    {
        return url;
    }

    public String getTitle()
    {
        return title;
    }

    public String getUrlContent()
    {
        return urlContent;
    }

    public LocalDateTime getCrawledAt()
    {
        return crawledAt;
    }

    public int getDepth()
    {
        return depth;
    }
    // Override toString for easier retrieval
    @Override
    public String toString()
    {
        return String.format("CrawledPage[id=%d, url='%s', title='%s', urlContent='%s', crawledAt='%s', depth=%d]",
            id, url, title, urlContent, crawledAt, depth
        );
    }
}
