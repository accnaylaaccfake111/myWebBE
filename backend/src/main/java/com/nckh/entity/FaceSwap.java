package com.nckh.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceSwap extends BaseEntity{
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FaceSwapStatus status = FaceSwapStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ai_model_version", length = 50)
    private String aiModelVersion;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "face_swap_faces",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "face_image_id")
    )
    private List<MediaFile> faceImages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_video_id")
    private MediaFile targetVideo;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "result_video_id")
    private MediaFile resultVideo;

    @Column(name = "swap_type")
    private String swapType;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message")
    private String errorMessage;

    public enum FaceSwapStatus {
        DRAFT,
        PROCESSING,
        COMPLETED,
        FAILED,
        PUBLISHED,
        ARCHIVED,
        DELETED
    }
}
