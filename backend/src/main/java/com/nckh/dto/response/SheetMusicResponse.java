package com.nckh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetMusicResponse {
    Long id;
    String title;
    Integer duration;
    String status;
    String theme;
    String mood;
    LocalDateTime createdAt;
}
