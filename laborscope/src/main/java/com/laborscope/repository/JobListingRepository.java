package com.laborscope.repository;

// Imports for JPA and JobListing
import com.laborscope.model.JobListing;
import org.springframework.data.jpa.repository.JpaRepository;

// Built-in functionality imports
import java.time.LocalDateTime;
import java.util.List;

public interface JobListingRepository extends JpaRepository<JobListing, Long>{
    List<JobListing> findByCompany(String company);
    List<JobListing> findByJobTitle(String jobTitle);
    List<JobListing> findByJobType(String jobType);
    List<JobListing> findBySkills(String skill);
    List<JobListing> findByExperienceLevel(String experienceLevel);
    List<JobListing> findByLocation(String location);
    List<JobListing> findByPostedAtBefore(LocalDateTime date);
    List<JobListing> findBySourceSite(String sourceSite);
}
