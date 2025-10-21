package com.nckh.controller;

import com.nckh.dto.request.FaceSwapMultiRequest;
import com.nckh.dto.request.FaceSwapSingleRequest;
import com.nckh.dto.response.ApiResponse;
import com.nckh.dto.response.FaceSwapDetailResponse;
import com.nckh.dto.response.FaceSwapResponse;
import com.nckh.service.impl.FaceSwapServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("/api/face-swap")
@Tag(name = "face-swap", description = "Swap face from image to video with SimSwap AI")
public class FaceSwapController {
    private final FaceSwapServiceImpl faceSwapService;

    @PostMapping(value = "/process-single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Process Single Face Swap", description = "Api for single face swap")
    public ResponseEntity<ApiResponse<FaceSwapResponse>> processSingleFaceSwap(
            @RequestParam("sourceImage") MultipartFile sourceImage,
            @RequestParam("targetVideo") MultipartFile targetVideo,
            @RequestParam(value = "title", required = false) String title,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received face swap request for user: {}", userDetails.getUsername());
        validateFilesImage(List.of(sourceImage));
        validateFileVideo(targetVideo);

        FaceSwapSingleRequest request = FaceSwapSingleRequest.builder()
                .sourceImage(sourceImage)
                .targetVideo(targetVideo)
                .userName(userDetails.getUsername())
                .title(title)
                .build();

        FaceSwapResponse response = faceSwapService.processSingleFaceSwap(request);
        return ResponseEntity.ok(ApiResponse.<FaceSwapResponse>builder()
                .success(true)
                .message("Image and video is processing")
                .data(response)
                .build());
    }

    @PostMapping(value = "/process-multi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Process Multi Face Swap", description = "Api for multi face swap")
    public ResponseEntity<ApiResponse<FaceSwapResponse>> processMultiFaceSwap(
            @RequestParam("srcImage") List<MultipartFile> srcImage,
            @RequestParam("dstImage") List<MultipartFile> dstImage,
            @RequestParam("targetVideo") MultipartFile targetVideo,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Received face swap request for user: {}", userDetails.getUsername());
        validateFileVideo(targetVideo);
        validateFilesImage(srcImage);
        validateFilesImage(dstImage);

        FaceSwapMultiRequest request = FaceSwapMultiRequest.builder()
                .srcImages(srcImage)
                .dstImages(dstImage)
                .targetVideo(targetVideo)
                .userName(userDetails.getUsername())
                .build();
        FaceSwapResponse response = faceSwapService.processMultiFaceSwap(request);
        return ResponseEntity.ok(ApiResponse.<FaceSwapResponse>builder()
                .success(true)
                .message("Image and video is processing")
                .data(response)
                .build());
    }

    @PostMapping(value = "/detect-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Detect Video", description = "Detect faces in video")
    public ResponseEntity<List<String>> detectVideo(
            @RequestParam("video") MultipartFile video) {
        List<String> response = faceSwapService.detectVideo(video);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{projectId}")
    @Operation(summary = "View Status", description = "Check status of face swap process")
    public ResponseEntity<FaceSwapResponse> getProcessingStatus(@PathVariable Long projectId) {
        FaceSwapResponse response = faceSwapService.getProcessingStatus(projectId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel/{projectId}")
    @Operation(summary = "Cancel Process", description = "Cancel current process")
    public ResponseEntity<Void> cancelProcessing(@PathVariable Long projectId) {
        faceSwapService.cancelProcessing(projectId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "Get My FacSwap", description = "Get face swap project by user id")
    public ResponseEntity<ApiResponse<List<FaceSwapDetailResponse>>> myFaceSwapProject(
            @AuthenticationPrincipal UserDetails userDetails
    ){
        List<FaceSwapDetailResponse> responses = faceSwapService.getByUsers(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.<List<FaceSwapDetailResponse>>builder()
                        .message("Get face swap project by user successful")
                        .data(responses)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Get My FacSwap", description = "Get face swap project by user id")
    public ResponseEntity<ApiResponse<String>> deleteFaceSwapProject(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ){
        faceSwapService.deleteFaceSwap(id);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                        .success(true)
                        .data("Delete Face Swap Project success")
                .build());
    }

    private void validateFileVideo(MultipartFile targetVideo) {

        if (targetVideo.isEmpty()) {
            throw new RuntimeException("Target video is required");
        }

        String videoContentType = targetVideo.getContentType();
        if (videoContentType == null || !videoContentType.startsWith("video/")) {
            throw new RuntimeException("Target file must be a video");
        }

        long maxSize = 100 * 1024 * 1024; // 100MB
        if (targetVideo.getSize() > maxSize) {
            throw new RuntimeException("File size must be less than 100MB");
        }
    }

    private void validateFilesImage(List<MultipartFile> sourceImage){
        if (sourceImage.isEmpty()) {
            throw new RuntimeException("Source image is required");
        }

        for (MultipartFile image : sourceImage) {
            String imageContentType = image.getContentType();
            if (imageContentType == null || !imageContentType.startsWith("image/")) {
                throw new RuntimeException("Source file must be an image");
            }

            long maxSize = 5 * 1024 * 1024; // 5MB
            if (image.getSize() > maxSize) {
                throw new RuntimeException("File size must be less than 5MB");
            }
        }
    }
}
