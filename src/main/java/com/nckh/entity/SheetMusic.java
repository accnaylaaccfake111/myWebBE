package com.nckh.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetMusic extends BaseEntity{
    private String taskId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SheetMusicStatus status = SheetMusicStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @OneToOne
    private MediaFile audioFile;

    @Column(name = "duration")
    private int duration;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sheet_music", columnDefinition = "TEXT")
    private String sheetMusicXML;

    private String musicModelAI;

    private String sheetMidelAI;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lyric_id")
    @JsonBackReference
    private LyricsComposition lyricsComposition;

    public enum SheetMusicStatus {
        DRAFT,
        MUSIC_GENERATE_PROCESSING,
        SHEET_GENERATE_PROCESSING,
        MUSIC_COMPLETED,
        SHEET_COMPLETED,
        MUSIC_FAILED,
        SHEET_FAILED,
        DELETED
    }
}
