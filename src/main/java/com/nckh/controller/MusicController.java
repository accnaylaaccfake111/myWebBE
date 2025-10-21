package com.nckh.controller;

import com.nckh.dto.request.MusicGenerateRequest;
import com.nckh.dto.request.SunoCallBackRequest;
import com.nckh.dto.response.MusicGenerateResponse;
import com.nckh.service.MusicService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("/api/music")
@Tag(name = "Music", description = "AI-powered music generation APIs")
public class MusicController {
    private final MusicService musicService;

    @PostMapping("/generate")
    public ResponseEntity<MusicGenerateResponse> generateMusic(
            @RequestBody MusicGenerateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        request.setUserName(userDetails.getUsername());
        MusicGenerateResponse response = musicService.generateMusic(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{taskId}")
    public ResponseEntity<MusicGenerateResponse> getTaskStatus(@PathVariable String taskId) {
        MusicGenerateResponse response = musicService.getTaskStatus(taskId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleSunoSuccess(@RequestBody SunoCallBackRequest request) {
        log.info("Received Suno success callback");
        musicService.processCompletedGeneration(request);
        return ResponseEntity.ok().build();
    }
}
