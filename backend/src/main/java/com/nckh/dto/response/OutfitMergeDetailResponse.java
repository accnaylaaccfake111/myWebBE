package com.nckh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitMergeDetailResponse {
    private Long id;
    private String garmentImage;
    private String modelImage;
    private String resultImage;
    private Long timeProcessing;
    private String modelAi;
    private String errorMessage;
    private String status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
}
