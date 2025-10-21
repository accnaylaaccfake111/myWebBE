package com.nckh.controller;

import com.nckh.dto.response.ApiResponse;
import com.nckh.dto.response.OutfitMergeDetailResponse;
import com.nckh.dto.response.OutfitMergeResponse;
import com.nckh.service.OutfitMergeService;
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
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("/api/outfit")
@Tag(name = "Music", description = "AI-powered music generation APIs")
public class OutfitMergeController {
    private final OutfitMergeService outfitMergeService;

    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<OutfitMergeResponse>> merge(
            @RequestParam("garmentImage") MultipartFile garmentImage,
            @RequestParam("modelImage") MultipartFile modelImage,
            @AuthenticationPrincipal UserDetails userDetails
            ){
        try{
            validateFilesImage(List.of(garmentImage, modelImage));
            OutfitMergeResponse response = outfitMergeService.merge(userDetails.getUsername(), garmentImage, modelImage);
            return ResponseEntity.ok(ApiResponse.<OutfitMergeResponse>builder()
                            .success(true)
                            .data(response)
                    .build());
        }catch (Exception ex){
            return ResponseEntity.ok(ApiResponse.<OutfitMergeResponse>builder()
                    .success(false)
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OutfitMergeDetailResponse>>> getData(
            @AuthenticationPrincipal UserDetails userDetails
    ){
        try {
            List<OutfitMergeDetailResponse> responses = outfitMergeService.getAlls(userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.<List<OutfitMergeDetailResponse>>builder()
                            .success(true)
                            .message("Get Outfit Merge Project successful")
                            .data(responses)
                    .build());
        }catch (Exception ex){
            return ResponseEntity.ok(ApiResponse.<List<OutfitMergeDetailResponse>>builder()
                            .success(false)
                            .message("Get Outfit Merge Project failled")
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OutfitMergeDetailResponse>> getById(
            @PathVariable("id") Long id
    ){
        try {
            OutfitMergeDetailResponse responses = outfitMergeService.getById(id);
            return ResponseEntity.ok(ApiResponse.<OutfitMergeDetailResponse>builder()
                    .success(true)
                    .message("Get Outfit Merge Project successful")
                    .data(responses)
                    .build());
        }catch (Exception ex){
            return ResponseEntity.ok(ApiResponse.<OutfitMergeDetailResponse>builder()
                    .success(false)
                    .message("Get Outfit Merge Project failled")
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteById(@PathVariable("id") Long id){
        outfitMergeService.delete(id);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                        .message("Delete successful")
                .build());
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
