package com.nckh.service;

import com.nckh.dto.request.KaraokeGenerateRequest;
import com.nckh.dto.response.KaraokeTrackResponse;
import com.nckh.dto.response.PerformanceScoreResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KaraokeService {
    
    KaraokeTrackResponse generateKaraokeTrack(KaraokeGenerateRequest request, String username);
    
    PerformanceScoreResponse scoreUserPerformance(
            MultipartFile voiceFile, 
            Long trackId, 
            String lyrics, 
            Double duration, 
            String username
    );
    
    KaraokeTrackResponse getKaraokeTrack(Long trackId);
    
    List<PerformanceScoreResponse> getUserPerformances(String username);
    
    List<PerformanceScoreResponse> getLeaderboard(Integer limit);
    
    void deletePerformance(Long performanceId, String username);
    
    List<KaraokeTrackResponse> getUserTracks(String username);
    
    List<KaraokeTrackResponse> getPublicTracks(Integer limit);
}