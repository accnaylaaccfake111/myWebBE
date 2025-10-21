package com.nckh.dto.response;

import com.nckh.entity.MediaFile;
import com.nckh.entity.SheetMusic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetMusicDetailResponse {
    private Long id;
    private Long lyricId;
    private String title;
    private SheetMusic.SheetMusicStatus status;
    private Long processingTimeMs;
    private String audioFile;
    private LocalDateTime completedAt;
    private String errorMessage;
    private String sheetMusicXML;
    private String musicModelAI;
    private String sheetMidelAI;
    private String lyricModelAi;
    private String lyrics;
    private LocalDateTime createdAt;
}
