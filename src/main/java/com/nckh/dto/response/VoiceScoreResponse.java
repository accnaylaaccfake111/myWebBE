package com.nckh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceScoreResponse {
    private Integer lyricMatch;
    private Integer pronunciation;
    private Integer intonation;
    private Integer fluency;
    private Integer overall;
}
