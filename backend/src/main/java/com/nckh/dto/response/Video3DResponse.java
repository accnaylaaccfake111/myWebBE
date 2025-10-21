package com.nckh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video3DResponse {
    private Long id;
    private String title;
    private String videoUrl;
    private String resultUrl;
    private String type;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
}
