package com.nckh.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "music_gen_task")
public class MusicGenTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String taskId;  // Internal task ID

    private Long lyricId;

    @Column(unique = true)
    private String externalTaskId;  // Task ID từ Suno

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_music_id")
    private SheetMusic sheetMusic;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    private MusicProvider provider;

    @Column(length = 1024)
    private String prompt;
    private Integer duration;
    private String audioUrl;
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public enum TaskStatus {
        PENDING,
        PROCESSING,
        DOWNLOADING,
        COMPLETED,
        FAILED
    }

    public enum MusicProvider {
        SUNO,
        MUSICGEN
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
