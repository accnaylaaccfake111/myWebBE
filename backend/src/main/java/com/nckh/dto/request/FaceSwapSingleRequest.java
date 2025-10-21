package com.nckh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceSwapSingleRequest {
    private MultipartFile sourceImage;
    private MultipartFile targetVideo;
    private String userName;
    private String title;
}
