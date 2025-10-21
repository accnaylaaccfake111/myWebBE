package com.nckh.service;

import com.nckh.dto.request.LyricsGenerateRequest;
import com.nckh.dto.response.LyricsResponse;

import java.util.List;

public interface LyricsService {
    
    LyricsResponse generateLyrics(LyricsGenerateRequest request, String username);
    
    List<LyricsResponse> getUserLyricsHistory(String username);
    
    List<String> getAvailableThemes();
    
    List<String> getAvailableMoods();
    
    void saveLyricsAsProject(Long lyricsId, String username);
    
    void rateLyrics(Long lyricsId, Integer rating, String username);
    
    LyricsResponse getLyricsById(Long id);
    
    void deleteLyrics(Long id, String username);
}