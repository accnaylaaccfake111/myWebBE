package com.nckh.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outfit_merge")
public class OutfitMerge extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="garment_image_id")
    private MediaFile garmentImage;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="model_image_id")
    private MediaFile modelImage;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="result_image_id")
    private MediaFile resultImage;
    @Column(name = "time_proccessing")
    private Long timeProcessing;
    @Column(name = "model_ai")
    private String modelAi;
    @Column(name = "error_message")
    private String errorMessage;
    @Enumerated(EnumType.STRING)
    private OutfiMergeStatus status;

    public enum OutfiMergeStatus{
        DRAFT,
        PROCCESSING,
        COMPLETED,
        FAILED
    }
}
