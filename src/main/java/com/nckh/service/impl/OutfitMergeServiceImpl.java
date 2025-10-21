package com.nckh.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.nckh.dto.response.FileUploadResponse;
import com.nckh.dto.response.OutfitMergeDetailResponse;
import com.nckh.dto.response.OutfitMergeResponse;
import com.nckh.entity.MediaFile;
import com.nckh.entity.OutfitMerge;
import com.nckh.entity.User;
import com.nckh.exception.ResourceNotFoundException;
import com.nckh.httpclient.OutfitMergeClient;
import com.nckh.repository.MediaFileRepository;
import com.nckh.repository.OutfitMergeRepository;
import com.nckh.repository.UserRepository;
import com.nckh.service.CloudinaryService;
import com.nckh.service.OutfitMergeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitMergeServiceImpl implements OutfitMergeService {
    private final OutfitMergeClient outfitMergeClient;
    private final OutfitMergeRepository outfitMergeRepository;
    private final UserRepository userRepository;
    private final MediaFileRepository mediaFileRepository;
    private final CloudinaryService cloudinaryService;

    @Value("${ai-service.outfit-merge.api-key}")
    private String apiKey;

    private static final int MAX_TRIES = 20;
    private static final int DELAY_MS = 3000;
    private static final int DOWNLOAD_RETRY_COUNT = 3;
    private static final int DOWNLOAD_DELAY_MS = 2000;

    @Override
    @Transactional
    public OutfitMergeResponse merge(String username, MultipartFile garmentImage, MultipartFile modelImage) {
        User user = getUserByUsername(username);
        OutfitMerge om = createOutfitMerge(user);

        try {
            ResponseEntity<JsonNode> response = outfitMergeClient.createTask(
                    apiKey, garmentImage, modelImage, "full");

            if (!isTaskCreatedSuccessfully(response)) {
                return buildFailedResponse("Failed to create task");
            }

            saveInputImages(om, user, garmentImage, modelImage);
            om.setStatus(OutfitMerge.OutfiMergeStatus.PROCCESSING);
            om = outfitMergeRepository.save(om);

            String taskId = response.getBody().get("task_id").asText();
            processTask(om, user, taskId);

            outfitMergeRepository.save(om);
            return buildSuccessResponse(om);

        } catch (Exception ex) {
            log.error("Merge outfit failed: {}", ex.getMessage());
            return buildFailedResponse(ex.getMessage());
        }
    }

    @Override
    @Transactional
    public OutfitMergeDetailResponse getById(Long id) {
        OutfitMerge om = outfitMergeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Outfit Merge Project not found"));

        return mapToDetailResponse(om);
    }

    @Override
    @Transactional
    public List<OutfitMergeDetailResponse> getAlls(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return outfitMergeRepository.findAllByUser(user).stream()
                .map(this::mapToDetailResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        OutfitMerge om = outfitMergeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Outfit Merge not found: " + id));

        List<String> publicIdsToDelete = Stream.of(
                        om.getGarmentImage(),
                        om.getModelImage(),
                        om.getResultImage()
                )
                .filter(Objects::nonNull)
                .map(MediaFile::getPublicId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        outfitMergeRepository.delete(om);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        deleteCloudinaryFiles(publicIdsToDelete, id);
                    }
                }
        );
    }

    private void deleteCloudinaryFiles(List<String> publicIds, Long outfitMergeId) {
        publicIds.forEach(publicId -> {
            try {
                cloudinaryService.deleteFile(publicId, "image");
                log.debug("Deleted Cloudinary file: {}", publicId);
            } catch (Exception e) {
                log.error("Failed to delete Cloudinary file [{}] for outfitMerge id={}",
                        publicId, outfitMergeId, e);
            }
        });
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean isTaskCreatedSuccessfully(ResponseEntity<JsonNode> response) {
        return response.getStatusCode().is2xxSuccessful()
                && response.getBody() != null
                && "CREATED".equals(response.getBody().get("status").asText());
    }

    private void saveInputImages(OutfitMerge om, User user,
                                 MultipartFile garmentImage, MultipartFile modelImage) throws IOException {
        MediaFile saveGarmentImage = saveMediafile(user, garmentImage.getBytes(),
                "outfit_garment_image_" + om.getId() + ".png");
        MediaFile saveModelImage = saveMediafile(user, modelImage.getBytes(),
                "outfit_model_image_" + om.getId() + ".png");

        om.setGarmentImage(saveGarmentImage);
        om.setModelImage(saveModelImage);
    }

    private void processTask(OutfitMerge om, User user, String taskId) {
        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                ResponseEntity<JsonNode> resp = outfitMergeClient.checkStatusTask(apiKey, taskId);
                JsonNode body = resp.getBody();

                if (body == null) {
                    continue;
                }

                String status = body.get("status").asText();

                if ("COMPLETED".equals(status)) {
                    handleCompletedTask(om, user, body);
                    break;
                } else if ("FAILED".equals(status)) {
                    handleFailedTask(om, body);
                    break;
                }

                waitBeforeRetry();

            } catch (Exception e) {
                log.error("Error checking task status: {}", e.getMessage());
                if (i == MAX_TRIES - 1) {
                    handleTaskTimeout(om);
                }
            }
        }
    }

    private void handleCompletedTask(OutfitMerge om, User user, JsonNode body) {
        try {
            String downloadUrl = body.get("download_signed_url").asText();
            byte[] resultBytes = downloadResultWithRetry(downloadUrl);

            if (resultBytes != null) {
                MediaFile saveResultImage = saveMediafile(user, resultBytes,
                        "outfit_result_image_" + om.getId() + ".png");
                om.setStatus(OutfitMerge.OutfiMergeStatus.COMPLETED);
                om.setResultImage(saveResultImage);
            } else {
                om.setStatus(OutfitMerge.OutfiMergeStatus.FAILED);
                om.setErrorMessage("Failed to download result image after multiple attempts");
            }

        } catch (Exception e) {
            log.error("Failed to handle completed task: {}", e.getMessage());
            om.setStatus(OutfitMerge.OutfiMergeStatus.FAILED);
            om.setErrorMessage("Download failed: " + e.getMessage());
        } finally {
            om.setTimeProcessing(Duration.between(om.getCreatedAt(), LocalDateTime.now()).toMillis());
        }
    }

    private byte[] downloadResultWithRetry(String downloadUrl) {
        RestTemplate simpleRestTemplate = new RestTemplate();
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        simpleRestTemplate.setUriTemplateHandler(uriBuilderFactory);

        for (int retry = 0; retry < DOWNLOAD_RETRY_COUNT; retry++) {
            try {
                Thread.sleep(DOWNLOAD_DELAY_MS * (retry + 1));

                log.info("Downloading result from: {} (attempt {})", downloadUrl, retry + 1);

                // Dùng URI thay vì String để tránh encoding
                URI uri = URI.create(downloadUrl);
                ResponseEntity<byte[]> fileDown = simpleRestTemplate.getForEntity(uri, byte[].class);

                if (fileDown.getStatusCode().is2xxSuccessful() && fileDown.getBody() != null) {
                    log.info("Successfully downloaded result image ({} bytes)", fileDown.getBody().length);
                    return fileDown.getBody();
                }

            } catch (Exception e) {
                log.warn("Download attempt {} failed: {}", retry + 1, e.getMessage());
                if (retry == DOWNLOAD_RETRY_COUNT - 1) {
                    log.error("Failed to download after {} attempts", DOWNLOAD_RETRY_COUNT);
                }
            }
        }
        return null;
    }

    private void handleFailedTask(OutfitMerge om, JsonNode body) {
        om.setStatus(OutfitMerge.OutfiMergeStatus.FAILED);
        om.setErrorMessage(body.get("error") != null ? body.get("error").asText() : "Unknown error");
        om.setTimeProcessing(Duration.between(om.getCreatedAt(), LocalDateTime.now()).toMillis());
    }

    private void handleTaskTimeout(OutfitMerge om) {
        om.setStatus(OutfitMerge.OutfiMergeStatus.FAILED);
        om.setErrorMessage("Task timeout after " + MAX_TRIES + " attempts");
        om.setTimeProcessing(Duration.between(om.getCreatedAt(), LocalDateTime.now()).toMillis());
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while waiting for task", e);
        }
    }

    private OutfitMerge createOutfitMerge(User user) {
        OutfitMerge om = OutfitMerge.builder()
                .modelAi("FitRoom")
                .status(OutfitMerge.OutfiMergeStatus.DRAFT)
                .user(user)
                .build();

        return outfitMergeRepository.save(om);
    }

    private MediaFile saveMediafile(User user, byte[] file, String fileName) {
        try {
            FileUploadResponse response = cloudinaryService.uploadImage(file);

            MediaFile mediaFile = new MediaFile();
            mediaFile.setFileName(fileName);
            mediaFile.setPublicId(response.getPublicId());
            mediaFile.setOriginalName(fileName);
            mediaFile.setFilePath(response.getSecureUrl());
            mediaFile.setFileUrl(response.getSecureUrl());
            mediaFile.setFileType(MediaFile.FileType.IMAGE);
            mediaFile.setMimeType("image/png");
            mediaFile.setFileSize((long) file.length);
            mediaFile.setUser(user);
            mediaFile.setProcessed(true);
            mediaFile.setStorageType(MediaFile.StorageType.CLOUDINARY);

            return mediaFileRepository.save(mediaFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save media file: " + e.getMessage(), e);
        }
    }

    private OutfitMergeResponse buildSuccessResponse(OutfitMerge om) {
        return OutfitMergeResponse.builder()
                .jobId(om.getId())
                .status(om.getStatus().name())
                .outputUrl(om.getResultImage() != null ? om.getResultImage().getFileUrl() : null)
                .errorMessage(om.getErrorMessage())
                .build();
    }

    private OutfitMergeResponse buildFailedResponse(String errorMessage) {
        return OutfitMergeResponse.builder()
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }

    @Transactional
    protected OutfitMergeDetailResponse mapToDetailResponse(OutfitMerge om) {
        return OutfitMergeDetailResponse.builder()
                .id(om.getId())
                .modelAi(om.getModelAi())
                .garmentImage(Optional.ofNullable(om.getGarmentImage())
                        .map(MediaFile::getFileUrl)
                        .orElse(null))
                .modelImage(Optional.ofNullable(om.getModelImage())
                        .map(MediaFile::getFileUrl)
                        .orElse(null))
                .resultImage(Optional.ofNullable(om.getResultImage())
                        .map(MediaFile::getFileUrl)
                        .orElse(null))
                .timeProcessing(om.getTimeProcessing())
                .errorMessage(om.getErrorMessage())
                .updateAt(om.getUpdatedAt())
                .status(om.getStatus().name())
                .createAt(om.getCreatedAt())
                .build();
    }
}