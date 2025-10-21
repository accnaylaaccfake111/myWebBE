package com.nckh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nckh.dto.response.PerformanceScoreResponse;
import com.nckh.dto.response.VoiceAnalysResponse;
import com.nckh.entity.KaraokeTrack;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface VoiceAnalysisService {

    VoiceAnalysResponse analyst(MultipartFile voiceFile, String lyricId);

    VoiceAnalysResponse analyzeWithAI(String voice, String lyrics) throws JsonProcessingException;

}