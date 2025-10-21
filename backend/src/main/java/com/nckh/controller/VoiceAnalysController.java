package com.nckh.controller;

import com.nckh.dto.response.ApiResponse;
import com.nckh.dto.response.VoiceAnalysResponse;
import com.nckh.service.VoiceAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("/api/voice")
@Tag(name = "Voice Analys", description = "Voice Analys processing")
public class VoiceAnalysController {
    private final VoiceAnalysisService voiceAnalysisService;

    @PostMapping("/analyst")
    @Operation(summary = "Voice analyst", description = "Voice analyst processing")
    public ResponseEntity<ApiResponse<VoiceAnalysResponse>> analyst(
            @RequestPart("record") MultipartFile record,
            @RequestPart("lyric") String lyric
            ){
        VoiceAnalysResponse response = voiceAnalysisService.analyst(record, lyric);
        return ResponseEntity.ok(ApiResponse.<VoiceAnalysResponse>builder()
                        .success(true)
                        .data(response)
                .build());
    }

}
