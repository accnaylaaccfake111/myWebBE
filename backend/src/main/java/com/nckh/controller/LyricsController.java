package com.nckh.controller;

import com.nckh.dto.request.LyricsGenerateRequest;
import com.nckh.dto.response.ApiResponse;
import com.nckh.dto.response.LyricsResponse;
import com.nckh.service.LyricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lyrics")
@Tag(name = "Lyrics Composition", description = "AI-powered lyrics generation APIs")
public class LyricsController {
    
    private final LyricsService lyricsService;
    
    @PostMapping("/generate")
    @Operation(summary = "Generate lyrics", description = "Generate lyrics based on theme and mood")
    public ResponseEntity<ApiResponse<LyricsResponse>> generateLyrics(
            @Valid @RequestBody LyricsGenerateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Generating lyrics for theme: {}, mood: {}", request.getTheme(), request.getMood());
        
        String username = userDetails != null ? userDetails.getUsername() : "anonymous";
        LyricsResponse response = lyricsService.generateLyrics(request, username);
        
        return ResponseEntity.ok(ApiResponse.<LyricsResponse>builder()
                .success(true)
                .message("Lyrics generated successfully")
                .data(response)
                .build());
    }
    
    @GetMapping("/history")
    @Operation(summary = "Get lyrics history", description = "Get user's lyrics generation history")
    public ResponseEntity<ApiResponse<List<LyricsResponse>>> getLyricsHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.ok(ApiResponse.<List<LyricsResponse>>builder()
                    .success(false)
                    .message("Login required to view history")
                    .build());
        }
        
        List<LyricsResponse> history = lyricsService.getUserLyricsHistory(userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.<List<LyricsResponse>>builder()
                .success(true)
                .message("History retrieved successfully")
                .data(history)
                .build());
    }
    
    @GetMapping("/themes")
    @Operation(summary = "Get available themes", description = "Get list of available themes for lyrics generation")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableThemes() {
        
        List<String> themes = lyricsService.getAvailableThemes();
        
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .success(true)
                .message("Themes retrieved successfully")
                .data(themes)
                .build());
    }
    
    @GetMapping("/moods")
    @Operation(summary = "Get available moods", description = "Get list of available moods for lyrics generation")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableMoods() {
        
        List<String> moods = lyricsService.getAvailableMoods();
        
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .success(true)
                .message("Moods retrieved successfully")
                .data(moods)
                .build());
    }
    
    @PostMapping("/{id}/save")
    @Operation(summary = "Save lyrics to project", description = "Save generated lyrics as a project")
    public ResponseEntity<ApiResponse<Void>> saveLyricsProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        if (userDetails == null) {
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Login required to save project")
                    .build());
        }
        
        lyricsService.saveLyricsAsProject(id, userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Lyrics saved as project successfully")
                .build());
    }
    
    @PostMapping("/{id}/rate")
    @Operation(summary = "Rate generated lyrics", description = "Provide feedback on generated lyrics")
    public ResponseEntity<ApiResponse<Void>> rateLyrics(
            @PathVariable Long id,
            @RequestParam Integer rating,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String username = userDetails != null ? userDetails.getUsername() : "anonymous";
        lyricsService.rateLyrics(id, rating, username);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Thank you for your feedback")
                .build());
    }
}