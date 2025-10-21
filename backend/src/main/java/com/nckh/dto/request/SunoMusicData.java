package com.nckh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SunoMusicData {
    private String id;
    private String audio_url;
    private String source_audio_url;
    private String stream_audio_url;
    private String source_stream_audio_url;
    private String image_url;
    private String source_image_url;
    private String prompt;
    private String model_name;
    private String title;
    private String tags;
    private String createTime;
    private String duration;
}
