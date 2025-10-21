package com.nckh.controller;

import com.nckh.dto.response.ApiResponse;
import com.nckh.dto.response.Video3DResponse;
import com.nckh.service.Video3DService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("/api/video-3d")
@Tag(name = "Video 3D", description = "AI-powered video 3D processing")
public class Video3DController {
    private final Video3DService video3DService;

    @GetMapping
    @Operation
    public ResponseEntity<ApiResponse<List<Video3DResponse>>> getData(
            @AuthenticationPrincipal UserDetails userDetails
    ){
        List<Video3DResponse> responses = video3DService.getAll(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.<List<Video3DResponse>>builder()
                        .success(true)
                        .data(responses)
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation
    public ResponseEntity<ApiResponse<String>> deleteProject(
            @PathVariable("id") Long id
    ){
        video3DService.delete(id);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                        .data("Delete successful")
                        .success(true)
                        .message("Delete Video 3D project with ID::%s".formatted(id))
                .build());
    }
}
