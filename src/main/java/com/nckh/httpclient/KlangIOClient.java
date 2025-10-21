package com.nckh.httpclient;

import com.nckh.config.FeignConfig;
import com.nckh.dto.response.KlangIOResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(
        name = "klangIOClient",
        url = "${ai-service.klangio.url}",
        configuration = FeignConfig.class)
public interface KlangIOClient {
    @PostMapping(value = "/transcription", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<KlangIOResponse> transcribe(
            @RequestHeader("kl-api-key") String apiKey,
            @RequestParam("model") String model,
            @RequestParam("title") String title,
            @RequestParam("composer") String composer,
            @RequestParam("webhook_url") String webhookUrl,
            @RequestPart("file") MultipartFile file,
            @RequestPart("outputs") String outputs
    );

    @GetMapping(value = "/job/{id}/xml", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> downloadMusicXml(@PathVariable String id, @RequestHeader("kl-api-key") String apiKey);
}
