package com.nckh.service;


import com.nckh.dto.request.FaceSwapMultiRequest;
import com.nckh.dto.request.FaceSwapSingleRequest;
import com.nckh.dto.response.FaceSwapDetailResponse;
import com.nckh.dto.response.FaceSwapResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FaceSwapService {
    FaceSwapResponse processSingleFaceSwap(FaceSwapSingleRequest request);
    FaceSwapResponse processMultiFaceSwap(FaceSwapMultiRequest request);
    List<String> detectVideo(MultipartFile video);
    FaceSwapResponse getProcessingStatus(Long projectId);
    void cancelProcessing(Long projectId);
    List<FaceSwapDetailResponse> getByUsers(String username);
    void deleteFaceSwap(Long id);
}
