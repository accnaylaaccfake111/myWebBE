package com.nckh.dto.response;

import com.nckh.entity.UserPerformance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceScoreResponse {
    private Long id;
    private Long userId;
    private String username;
    private Long karaokeTrackId;
    private String songTitle;
    private String voiceRecordingUrl;
    private Double overallScore;
    private Double pitchAccuracy;
    private Double timingAccuracy;
    private Double rhythmScore;
    private Double pronunciationScore;
    private Double completionPercentage;
    private Double performanceDurationSeconds;
    private String detailedFeedback;
    private String scoreBreakdown;
    private String analysisMethod;
    private Long analysisTimeMs;
    private Boolean isPublicScore;
    private String difficultyLevel;
    private Integer rankPosition;
    private UserPerformance.PerformanceStatus status;
    private LocalDateTime performedAt;
    private String grade; // A+, A, B+, B, C+, C, D
    private String achievements; // JSON array of achievement badges
}