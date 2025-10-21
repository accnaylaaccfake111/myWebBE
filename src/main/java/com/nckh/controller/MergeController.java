package com.nckh.controller;

import com.nckh.dto.response.ApiResponse;
import com.nckh.service.MergeService;
import com.nckh.service.impl.GoogleSheetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/merge")
@Tag(name = "Lyrics Composition", description = "AI-powered lyrics generation APIs")
public class MergeController {
    private final MergeService mergeService;
    private final GoogleSheetService googleSheetService;

    @PostMapping
    public ResponseEntity<ApiResponse<byte[]>> merge(
            @RequestPart("video") MultipartFile video,
            @RequestPart("music") MultipartFile music,
            @RequestPart("lyric") MultipartFile lyric
            ){
        return ResponseEntity.ok(ApiResponse.<byte[]>builder()
                        .success(true)
                        .data(mergeService.merge(video, music, lyric))
                        .message("SUCCESS")
                .build());
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateSheet(@RequestBody UpdateRequest request) {
        try {
            if (request.getValues() == null || request.getValues().size() != 5) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cần chính xác 5 giá trị cho A2->E2"));
            }

            int updatedRows = googleSheetService.updateRange(request.getValues());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("updatedRows", updatedRows);
            response.put("message", "Cập nhật thành công");

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi khi cập nhật: " + e.getMessage()));
        }
    }

    @Data
    public static class UpdateRequest {
        private List<Object> values;
    }
}
