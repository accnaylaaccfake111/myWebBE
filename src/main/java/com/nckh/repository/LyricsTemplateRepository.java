package com.nckh.repository;

import com.nckh.entity.LyricsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LyricsTemplateRepository extends JpaRepository<LyricsTemplate, Long> {
    
    List<LyricsTemplate> findByThemeAndIsActiveTrue(String theme);
    
    List<LyricsTemplate> findByThemeAndMoodAndIsActiveTrue(String theme, String mood);
    
    List<LyricsTemplate> findByStyleAndIsActiveTrue(String style);
    
    @Query("SELECT DISTINCT t.theme FROM LyricsTemplate t WHERE t.isActive = true")
    List<String> findDistinctThemes();
    
    @Query("SELECT DISTINCT t.mood FROM LyricsTemplate t WHERE t.mood IS NOT NULL AND t.isActive = true")
    List<String> findDistinctMoods();
    
    @Query("SELECT t FROM LyricsTemplate t WHERE t.theme = :theme AND t.isActive = true ORDER BY RAND() LIMIT 1")
    Optional<LyricsTemplate> findRandomByTheme(@Param("theme") String theme);
    
    @Query("SELECT t FROM LyricsTemplate t WHERE t.theme = :theme AND t.mood = :mood AND t.isActive = true ORDER BY RAND() LIMIT 1")
    Optional<LyricsTemplate> findRandomByThemeAndMood(@Param("theme") String theme, @Param("mood") String mood);
    
    @Modifying
    @Query("UPDATE LyricsTemplate t SET t.usageCount = t.usageCount + 1 WHERE t.id = :id")
    void incrementUsageCount(@Param("id") Long id);
    
    List<LyricsTemplate> findTop10ByIsActiveTrueOrderByPopularityDesc();
    
    List<LyricsTemplate> findBySourceAndIsActiveTrue(String source);
}