package com.nckh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lyrics_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LyricsTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String theme;
    
    private String mood;
    
    @Column(nullable = false)
    private String style; // lục bát, song thất, tự do
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String template;
    
    @Column(columnDefinition = "TEXT")
    private String example;
    
    @Column(name = "rhyme_pattern")
    private String rhymePattern; // 6-8-6-8 for lục bát
    
    private Integer lineCount;
    
    private Integer popularity = 0;
    
    private Integer usageCount = 0;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(columnDefinition = "JSON")
    private String placeholders; // JSON array of placeholder variables
    
    private String author;
    
    private String source; // traditional, modern, ai-generated
}