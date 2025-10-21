package com.nckh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicGenerateRequest {
    private Long lyricId;
    private String userName;
    private String theme;
    private String mood;
    private int duration;
}
