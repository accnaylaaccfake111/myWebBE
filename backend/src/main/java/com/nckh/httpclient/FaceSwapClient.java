package com.nckh.httpclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.nckh.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(
        name = "FaceSwapService",
        url = "${ai-service.faceswap.url}",
        configuration = FeignConfig.class
)
public interface FaceSwapClient {
    @PostMapping(
            value = "/detect-faces-video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<JsonNode> detectFacesInVideo(@RequestPart("video") MultipartFile video);

    @PostMapping(
            value = "/swap-single-face",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    ResponseEntity<byte[]> swapSingleFace(
            @RequestPart("image") MultipartFile image,
            @RequestPart("video") MultipartFile video
    );

    @PostMapping(
            value = "/swap-multi-faces",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    ResponseEntity<byte[]> swapMultiFaces(
            @RequestPart("video") MultipartFile video,
            @RequestPart("src_faces") List<MultipartFile> srcFaces,
            @RequestPart("dst_faces") List<MultipartFile> dstFaces
    );

    @PostMapping(
            value = "/merge-video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    ResponseEntity<byte[]> merge(
            @RequestPart("video") MultipartFile video,
            @RequestPart("music") MultipartFile music,
            @RequestPart("lyrics") MultipartFile lyrics
    );
}
