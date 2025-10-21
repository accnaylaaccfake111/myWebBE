package com.nckh.service.impl;

import com.nckh.httpclient.FaceSwapClient;
import com.nckh.service.MergeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MergeServiceImpl implements MergeService {
    private final FaceSwapClient faceSwapClient;

    @Override
    public byte[] merge(MultipartFile videoFile, MultipartFile lyricFile, MultipartFile musicFile) {
        try {
            ResponseEntity<byte[]> response = faceSwapClient.merge(videoFile, musicFile, lyricFile);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RestClientException("FastAPI returned error: " + response.getStatusCode());
            }else{
                return response.getBody();
            }
        } catch (RestClientException e) {
            log.error("Error calling FastAPI", e);
            throw new RuntimeException("Failed to detect faces in video: " + e.getMessage(), e);
        }
    }
}
