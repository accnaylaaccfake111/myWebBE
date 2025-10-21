package com.nckh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicGenerateResponse {
    private Long sheetMusicId;
    private String taskId;
    private String status;
    private String message;
    private String sheetMusic;
    private Long processingTimeMs;
    private String outputUrl;
    private String errorMessage;
}
