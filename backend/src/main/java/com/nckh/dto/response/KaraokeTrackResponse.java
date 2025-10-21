package com.nckh.dto.response;

import com.nckh.entity.KaraokeTrack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KaraokeTrackResponse {
    
    private Long id;
    private Long sheetMusicId;
    private String lyricsTitle;
    private String lyricsContent;
    private String audioFaileUrl;
    private String timingData;
    private Integer bpm;
    private String keySignature;
    private String timeSignature;
    private Integer durationSeconds;
    private String generationMethod;
    private Long generationTimeMs;
    private Boolean isPublic;
    private Integer playCount;
    private Double averageScore;
    private Integer totalPerformances;
    private String metadata;
    private KaraokeTrack.TrackStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}