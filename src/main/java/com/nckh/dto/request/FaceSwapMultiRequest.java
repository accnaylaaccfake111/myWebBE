package com.nckh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceSwapMultiRequest {
    private List<MultipartFile> srcImages;
    private List<MultipartFile> dstImages;
    private MultipartFile targetVideo;
    private String userName;
    private String title;
}
