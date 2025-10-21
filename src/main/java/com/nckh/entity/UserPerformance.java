package com.nckh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_performances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPerformance extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "karaoke_track_id", nullable = false)
    private KaraokeTrack karaokeTrack;
    
    @Column(name = "voice_recording_url", nullable = false, length = 500)
    private String voiceRecordingUrl;
    
    @Column(name = "overall_score", nullable = false)
    private Double overallScore;
    
    @Column(name = "pitch_accuracy")
    private Double pitchAccuracy;
    
    @Column(name = "timing_accuracy")
    private Double timingAccuracy;
    
    @Column(name = "rhythm_score")
    private Double rhythmScore;
    
    @Column(name = "pronunciation_score")
    private Double pronunciationScore;
    
    @Column(name = "completion_percentage")
    private Double completionPercentage;
    
    @Column(name = "performance_duration_seconds")
    private Double performanceDurationSeconds;
    
    @Column(columnDefinition = "JSON")
    private String detailedFeedback; // JSON with word-by-word analysis
    
    @Column(columnDefinition = "JSON")
    private String scoreBreakdown; // Detailed scoring metrics
    
    @Column(name = "analysis_method", length = 50)
    private String analysisMethod; // AI_VOICE_ANALYSIS, MANUAL, etc.
    
    @Column(name = "analysis_time_ms")
    private Long analysisTimeMs;
    
    @Column(name = "is_public_score")
    private Boolean isPublicScore = false;
    
    @Column(name = "difficulty_level", length = 20)
    private String difficultyLevel;
    
    @Column(name = "rank_position")
    private Integer rankPosition;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "performance_status")
    private PerformanceStatus status = PerformanceStatus.COMPLETED;
    
    public enum PerformanceStatus {
        RECORDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}