package com.nckh.controller;

import com.nckh.dto.request.KlangIOCallbackRequest;
import com.nckh.dto.request.SheetGanerateResquest;
import com.nckh.dto.request.TestSheetMusic;
import com.nckh.dto.response.ApiResponse;
import com.nckh.dto.response.SheetGanerateResponse;
import com.nckh.dto.response.SheetMusicDetailResponse;
import com.nckh.service.SheetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/sheets")
@Tag(name = "Sheet", description = "AI powered sheet music generation APIs and merge lyrics with sheet music")
public class SheetController {
    private final SheetService sheetService;

    @PostMapping("/test-merge")
    public String testMerge(@RequestBody TestSheetMusic request) {

        try {
            return sheetService.addLyricsToMusicXML(request.getSheetMusic(), request.getLyrics());
        } catch (Exception e) {
            return "FAILED";
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SheetGanerateResponse>> generateSheetMusic(
            @RequestParam("musicId") Long musicId,
            @AuthenticationPrincipal UserDetails userDetails) {
        SheetGanerateResquest resquest = SheetGanerateResquest.builder()
                .sheetMusicId(musicId)
                .userName(userDetails.getUsername())
                .build();
        SheetGanerateResponse response = sheetService.generateSheet(resquest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SheetMusicDetailResponse>>> getSumaryInfo(
            @AuthenticationPrincipal UserDetails userDetails
    ){
        List<SheetMusicDetailResponse> responses = sheetService.getSumaryInfo(userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.<List<SheetMusicDetailResponse>>builder()
                        .message("Get Sheet Music Sumary Information successful")
                        .data(responses)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SheetMusicDetailResponse>> getDetail(
            @PathVariable("id") Long id
    ){
        SheetMusicDetailResponse response = sheetService.getDetail(id);
        return ResponseEntity.ok(ApiResponse.<SheetMusicDetailResponse>builder()
                        .message("Get Detail Sheet Music with ID::%s successful".formatted(id))
                        .data(response)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable("id") Long id
    ){
        if(sheetService.delete(id)){
            return ResponseEntity.ok(ApiResponse.<String>builder()
                            .message("Delete Sheet Music With ID::%s successful")
                            .data("Success")
                    .build());
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.<String>builder()
                        .success(false)
                        .message("Delete Sheet Music With ID::%s failed")
                        .data("Failed")
                        .build());
    }

    @PostMapping("/callback")
    public void handleCallback(@RequestBody KlangIOCallbackRequest request) {
        log.info("Received callback data: {}", request.getId());

        sheetService.handleKlangIOCallback(request);
    }

    @PostMapping("/test-time-process")
    public void sendTest(@RequestParam("file") MultipartFile file){
        sheetService.testTimeProcess(file);
    }
}
