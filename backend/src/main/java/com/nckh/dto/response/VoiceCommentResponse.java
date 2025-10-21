package com.nckh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCommentResponse {
    private String lyricMatch;
    private String pronunciation;
    private String intonation;
    private String fluency;
    private String overall;
}
