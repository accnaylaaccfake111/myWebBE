package com.nckh.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lyrics_compositions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LyricsComposition extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String title;
    
    @Column(nullable = false)
    private String theme;
    
    private String customTheme;
    
    private String mood;
    
    private String style;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String lyrics;
    
    @Column(columnDefinition = "TEXT")
    private String formattedLyrics;
    
    private Integer lineCount;
    
    private Integer wordCount;
    
    private Double rhymeScore;
    
    @Column(length = 50)
    private String generationMethod;
    
    private Long generationTimeMs;
    
    private Integer rating;
    
    @Column(columnDefinition = "JSON")
    private String metadata;
    
    @Column(name = "is_saved_as_project")
    private Boolean isSavedAsProject = false;
    
    @Column(name = "template_id")
    private Long templateId;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "lyricsComposition", cascade = CascadeType.ALL)
    @JsonManagedReference
    private SheetMusic sheetMusic;
}