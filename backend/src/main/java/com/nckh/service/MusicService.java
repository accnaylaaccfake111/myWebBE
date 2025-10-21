package com.nckh.service;

import com.nckh.dto.request.MusicGenerateRequest;
import com.nckh.dto.request.SunoCallBackRequest;
import com.nckh.dto.response.MusicGenerateResponse;

public interface MusicService {
    MusicGenerateResponse generateMusic(MusicGenerateRequest request);

    void processCompletedGeneration(SunoCallBackRequest callbackData);

    void processFailedGeneration(SunoCallBackRequest request);

    MusicGenerateResponse getTaskStatus(String taskId);
}
