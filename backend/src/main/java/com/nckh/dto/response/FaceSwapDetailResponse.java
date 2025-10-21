package com.nckh.dto.response;

import com.nckh.entity.FaceSwap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceSwapDetailResponse {
    private Long id;
    private String title;
    private FaceSwap.FaceSwapStatus status;
    private String aiModelVersion;
    private Long processingTimeMs;
    private List<String> facesUrl;
    private String targetUrl;
    private String resultUrl;
    private String swapType;
    private LocalDateTime completedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
