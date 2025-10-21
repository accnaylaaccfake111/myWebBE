package com.nckh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceAnalysResponse {
    private VoiceScoreResponse scores;
    private VoiceCommentResponse comments;
}
