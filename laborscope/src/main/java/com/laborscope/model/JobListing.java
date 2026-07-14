package com.laborscope.model;

// Java Util imports
import java.util.List;
import java.time.LocalDateTime;

// JPA imports
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// Lombok imports
import lombok.Getter;
import lombok.Setter;

// This entity is a part of a entity-repository model for job listings
@Entity
@Getter
@Setter
public class JobListing {
    // Generate primary key for eventual PostgreSQL usage
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // PostgreSQL fields
    private Long id;
    private Long pageId;
    private String company;
    private String jobTitle;
    private String jobType;
    @ElementCollection
    private List<String> skills;
    private String experienceLevel;
    @Column(columnDefinition = "TEXT")
    private String jobDescription;
    private String payRange;
    private String location;
    private LocalDateTime postedAt;
    @Column(length = 2048)
    private String sourceSite;
    // Default constructor
    protected JobListing() {}
    // Initializer for class
    public JobListing (String company, String jobTitle, String jobType, List<String> skills,
        String experienceLevel, String jobDescription, String payRange, String location,
        LocalDateTime postedAt, String sourceSite)
    {
        this.company = company;
        this.jobTitle = jobTitle;
        this.jobType = jobType;
        this.skills = skills;
        this.experienceLevel = experienceLevel;
        this.jobDescription = jobDescription;
        this.payRange = payRange;
        this.location = location;
        this.postedAt = postedAt;
        this.sourceSite = sourceSite;
    }

}
