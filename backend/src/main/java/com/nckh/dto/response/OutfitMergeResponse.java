package com.nckh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitMergeResponse {
    private Long jobId;
    private String status;
    private String message;
    private Long processingTimeMs;
    private String outputUrl;
    private String errorMessage;
}
