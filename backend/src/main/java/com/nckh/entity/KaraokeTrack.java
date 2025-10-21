package com.nckh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "karaoke_tracks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KaraokeTrack extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id")
    private User creator;
    
    @Column(name = "audio_file_url", nullable = false, length = 500)
    private String audioFileUrl;
    
    @Column(name = "timing_data", columnDefinition = "TEXT")
    private String timingData; // JSON format for word-by-word timing
    
    @Column(name = "bpm")
    private Integer bpm;
    
    @Column(name = "key_signature", length = 20)
    private String keySignature; // C major, A minor, etc.
    
    @Column(name = "time_signature", length = 10)
    private String timeSignature; // 4/4, 3/4, etc.
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    @Column(name = "generation_method", length = 50)
    private String generationMethod; // AI, TEMPLATE, MANUAL
    
    @Column(name = "generation_time_ms")
    private Long generationTimeMs;
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(name = "play_count")
    private Integer playCount = 0;
    
    @Column(name = "average_score")
    private Double averageScore = 0.0;
    
    @Column(name = "total_performances")
    private Integer totalPerformances = 0;
    
    @Column(columnDefinition = "JSON")
    private String metadata; // Additional track information
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TrackStatus status = TrackStatus.ACTIVE;
    
    public enum TrackStatus {
        GENERATING,
        ACTIVE,
        FAILED,
        ARCHIVED
    }
}