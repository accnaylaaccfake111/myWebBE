package com.nckh.service;

import com.nckh.dto.request.LyricsGenerateRequest;

public interface IntegrateAIService {
    String generateLyrics(LyricsGenerateRequest request);
}
