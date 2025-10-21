package com.nckh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LyricsResponse {
    private Long id;
    
    private String theme;
    
    private String mood;
    
    private String style;
    
    private String lyrics;
    
    private String formattedLyrics; // with line breaks and verses
    
    private Integer lineCount;
    
    private Integer wordCount;
    
    private Double rhymeScore; // 0-100
    
    private String generationMethod; // AI, TEMPLATE, HYBRID
    
    private Long generationTimeMs;
    
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    private Integer rating;
    
    private Map<String, Object> metadata;
    
    // For displaying in UI
    private String[] lyricsLines;
    
    private String[][] lyricsVerses; // grouped by verses
}