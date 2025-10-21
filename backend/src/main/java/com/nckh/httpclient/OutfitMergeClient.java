package com.nckh.httpclient;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(
        name = "OutFitMergeService",
        url = "${ai-service.outfit-merge.url}"
)
public interface OutfitMergeClient {
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<JsonNode> createTask(
            @RequestHeader("X-API-KEY") String apiKey,
            @RequestPart("cloth_image") MultipartFile clothImage,
            @RequestPart("model_image") MultipartFile modelImage,
            @RequestPart("cloth_type") String type
            );

    @GetMapping("/{id}")
    ResponseEntity<JsonNode> checkStatusTask(
            @RequestHeader("X-API-KEY") String apiKey,
            @PathVariable("id") String id);
}
