package com.nckh.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.nckh.dto.request.FaceSwapMultiRequest;
import com.nckh.dto.request.FaceSwapSingleRequest;
import com.nckh.dto.response.FaceSwapDetailResponse;
import com.nckh.dto.response.FaceSwapResponse;
import com.nckh.dto.response.FileUploadResponse;
import com.nckh.entity.FaceSwap;
import com.nckh.entity.MediaFile;
import com.nckh.entity.User;
import com.nckh.exception.ResourceNotFoundException;
import com.nckh.httpclient.FaceSwapClient;
import com.nckh.repository.FaceSwapRepository;
import com.nckh.repository.MediaFileRepository;
import com.nckh.repository.UserRepository;
import com.nckh.service.CloudinaryService;
import com.nckh.service.FaceSwapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceSwapServiceImpl implements FaceSwapService {
    private static final String FACE_SWAP_MULTI = "MULTI";
    private static final String FACE_SWAP_SINGLE = "SINGLE";

    @Value("${ai-service.face-swap.url:http://localhost:5001}")
    private String fastApiBaseUrl;

    @Value("${ai-service.face-swap.timeout:1800}")
    private int timeoutSeconds;

    @Value("${minio.bucket.videos}")
    private String bucketNameVideo;

    private final CloudinaryService cloudinaryService;
    private final FaceSwapRepository faceSwapRepository;
    private final UserRepository userRepository;
    private final MediaFileRepository mediaFileRepository;
    private final RestTemplate restTemplate;
    private final FaceSwapClient faceSwapClient;
    private final Map<Long, CompletableFuture<FaceSwapResponse>> processingTasks = new ConcurrentHashMap<>();

    private final Executor taskExecutor = Executors.newFixedThreadPool(10);

    @Override
    public FaceSwapResponse processSingleFaceSwap(FaceSwapSingleRequest request) {
        return processFaceSwap(
                request.getUserName(),
                request.getTitle(),
                null,
                request.getSourceImage(),
                request.getTargetVideo(),
                FACE_SWAP_SINGLE
        );
    }

    @Override
    public FaceSwapResponse processMultiFaceSwap(FaceSwapMultiRequest request) {
        return processFaceSwap(
                request.getUserName(),
                request.getTitle(),
                request.getSrcImages(),
                request.getDstImages(),
                request.getTargetVideo(),
                FACE_SWAP_MULTI
        );
    }

    @Override
    public List<String> detectVideo(MultipartFile video) {
        try {
            ResponseEntity<JsonNode> response = faceSwapClient.detectFacesInVideo(video);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RestClientException("FastAPI returned error: " + response.getStatusCode());
            }else{
                JsonNode resultsNode = response.getBody().get("results");
                if (resultsNode.isArray()) {
                    List<String> results = new ArrayList<>();
                    log.info("Detect {}", results);
                    resultsNode.forEach(node -> results.add(node.asText()));
                    log.info("Detected {} faces in video", results.size());
                    return results;
                }
            }

            return List.of();
        } catch (RestClientException e) {
            log.error("Error calling FastAPI", e);
            throw new RuntimeException("Failed to detect faces in video: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FaceSwapResponse getProcessingStatus(Long jobId) {
        CompletableFuture<FaceSwapResponse> task = processingTasks.get(jobId);

        if (task == null) {
            FaceSwap faceSwap = faceSwapRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            return FaceSwapResponse.builder()
                    .jobId(jobId)
                    .status(faceSwap.getStatus().name())
                    .outputUrl(faceSwap.getResultVideo().getFileUrl())
                    .errorMessage(faceSwap.getErrorMessage())
                    .build();
        }

        if (task.isDone()) {
            try {
                FaceSwapResponse result = task.get();
                // Remove completed task
                processingTasks.remove(jobId);
                return result;
            } catch (Exception e) {
                log.error("Error getting task result for project: {}", jobId, e);
                return FaceSwapResponse.builder()
                        .jobId(jobId)
                        .status("FAILED")
                        .message("Error retrieving result: " + e.getMessage())
                        .build();
            }
        } else {
            return FaceSwapResponse.builder()
                    .jobId(jobId)
                    .status("PROCESSING")
                    .message("Face swap is still processing...")
                    .build();
        }
    }

    @Override
    public void cancelProcessing(Long projectId) {
        CompletableFuture<FaceSwapResponse> task = processingTasks.get(projectId);

        if (task != null && !task.isDone()) {
            task.cancel(true);
            processingTasks.remove(projectId);
            log.info("Cancelled face swap processing for project: {}", projectId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaceSwapDetailResponse> getByUsers(String username) {
        User user = findUser(username);
        return faceSwapRepository.findAllByUser(user).stream()
                .map(this::mapToDetailResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteFaceSwap(Long id) {
        FaceSwap om = faceSwapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Face Swap not found: " + id));

        List<String> publicIdsToDelete = om.getFaceImages().stream()
                        .map(MediaFile::getPublicId)
                                .toList();

        faceSwapRepository.delete(om);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            if(om.getResultVideo() != null){
                                cloudinaryService.deleteFile(om.getResultVideo().getPublicId(), "video");
                            }
                            if(om.getTargetVideo() != null){
                                cloudinaryService.deleteFile(om.getTargetVideo().getPublicId(), "video");
                            }
                        } catch (Exception e) {
                            log.error("Failed to delete Cloudinary file for outfitMerge id",  e);
                        }

                        deleteCloudinaryFiles(publicIdsToDelete, id);
                    }
                }
        );
    }

    private void deleteCloudinaryFiles(List<String> publicIds, Long faceSwapId) {
        publicIds.forEach(publicId -> {
            try {
                cloudinaryService.deleteFile(publicId, "image");
                log.debug("Deleted Cloudinary file: {}", publicId);
            } catch (Exception e) {
                log.error("Failed to delete Cloudinary file [{}] for outfitMerge id={}",
                        publicId, faceSwapId, e);
            }
        });
    }

    private FaceSwapResponse processFaceSwap(
            String username,
            String title,
            Object sourceFiles,
            Object dstFiles,
            MultipartFile targetVideo,
            String type) {

        try {
            User user = findUser(username);
            FaceSwap faceSwap = createProject(title, user);

            List<MediaFile> faceImages = new ArrayList<>();

            if (dstFiles instanceof MultipartFile src) {
                FileUploadResponse response = cloudinaryService.uploadImage(src.getBytes());
                MediaFile mediaFile = saveResultFile(
                        "source_image_" + faceSwap.getId(),
                        response.getSecureUrl(),
                        faceSwap,
                        src.getSize(),
                        response.getPublicId()
                );
                faceImages.add(mediaFile);

            } else if (dstFiles instanceof List<?> list) {
                int i = 1;
                for (Object item : list) {
                    if (item instanceof MultipartFile file) {
                        FileUploadResponse response = cloudinaryService.uploadImage(file.getBytes());
                        MediaFile mediaFile = saveResultFile(
                                "source_image_" + faceSwap.getId() + "_" + i,
                                response.getSecureUrl(),
                                faceSwap,
                                file.getSize(),
                                response.getPublicId()
                        );
                        faceImages.add(mediaFile);
                        i++;
                    }
                }
            }

            faceSwap.setFaceImages(faceImages);

            FileUploadResponse fileUploadResponse = cloudinaryService.uploadVideo(targetVideo.getBytes());
            MediaFile saveTargetVideo = saveResultFile("target_video_" + faceSwap.getId(), fileUploadResponse.getSecureUrl(), faceSwap, targetVideo.getSize(), fileUploadResponse.getPublicId());
            faceSwap.setTargetVideo(saveTargetVideo);

            log.info("Starting face swap processing for project: {}", faceSwap.getId());

            return executeAsyncProcessing(faceSwap, sourceFiles, dstFiles, targetVideo, type);
        } catch (Exception e) {
            log.error("Unexpected error starting face swap processing", e);
            throw new RuntimeException("Failed to process face swap: " + e.getMessage());
        }
    }

    private FaceSwap createProject(String title, User user) {
        FaceSwap faceSwap = FaceSwap.builder()
                .title(title != null ? title : "Face Swap" + LocalDateTime.now())
                .status(FaceSwap.FaceSwapStatus.DRAFT)
                .user(user)
                .aiModelVersion("SimSwap-v1.0")
                .build();

        return faceSwapRepository.save(faceSwap);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private FaceSwapResponse executeAsyncProcessing(
            FaceSwap faceSwap,
            Object sourceFiles,
            Object destinationFiles,
            MultipartFile targetVideo,
            String type) throws IOException {

        File videoFile = createTempFile(targetVideo, "video_");

        List<File> srcFiles = null;
        List<File> dstFiles = null;
        File singleImageFile = null;

        try {
            if (type.equals(FACE_SWAP_SINGLE)) {
                singleImageFile = createTempFile((MultipartFile) destinationFiles, "image_");
            } else {
                srcFiles = createTempFiles((List<MultipartFile>) sourceFiles, "SRC_");
                dstFiles = createTempFiles((List<MultipartFile>) destinationFiles, "DST_");
            }

            // Tạo final references cho lambda
            final File finalVideoFile = videoFile;
            final File finalSingleImageFile = singleImageFile;
            final List<File> finalSrcFiles = srcFiles;
            final List<File> finalDstFiles = dstFiles;

            CompletableFuture<FaceSwapResponse> future = CompletableFuture
                    .supplyAsync(() -> processWithSavedFiles(
                            faceSwap,
                            finalSingleImageFile,
                            finalSrcFiles,
                            finalDstFiles,
                            finalVideoFile,
                            type
                    ), taskExecutor)
                    .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        log.error("Face swap processing failed for project: {}", faceSwap.getId(), ex);
                        updateProjectStatus(faceSwap, FaceSwap.FaceSwapStatus.FAILED);

                        // Cleanup files on error
                        deleteQuietly(finalVideoFile);
                        deleteQuietly(finalSingleImageFile);
                        if (finalSrcFiles != null) finalSrcFiles.forEach(this::deleteQuietly);
                        if (finalDstFiles != null) finalDstFiles.forEach(this::deleteQuietly);

                        return buildResponse(faceSwap.getId(), faceSwap.getStatus().name(), "Face swap processing failed: " + ex.getMessage());

                    });

            processingTasks.put(faceSwap.getId(), future);
            return buildResponse(faceSwap.getId(), "PROCESSING", "Face swap is processing...");

        } catch (IOException e) {
            // Cleanup on error during file saving
            deleteQuietly(videoFile);
            deleteQuietly(singleImageFile);
            if (srcFiles != null) srcFiles.forEach(this::deleteQuietly);
            if (dstFiles != null) dstFiles.forEach(this::deleteQuietly);
            throw e;
        }
    }

    private FaceSwapResponse processWithSavedFiles(
            FaceSwap faceSwap,
            File singleImageFile,
            List<File> srcFiles,
            List<File> dstFiles,
            File videoFile,
            String type) {

        long startTime = System.currentTimeMillis();

        try {
            ResponseEntity<byte[]> resultData;
            if (type.equals(FACE_SWAP_SINGLE)) {
                faceSwap.setSwapType(FACE_SWAP_SINGLE);
                resultData = callFastApiService("/swap-single-face", singleImageFile, videoFile);
            } else {
                faceSwap.setSwapType(FACE_SWAP_MULTI);
                resultData = callFastApiServiceMulti("/swap-multi-faces", srcFiles, dstFiles, videoFile);
            }

            if(resultData.getStatusCode().is2xxSuccessful()){
                long processingTime = System.currentTimeMillis() - startTime;
                log.info("Process completed in {} ms", processingTime);

                byte[] data = resultData.getBody();

                // Upload and save result
                MediaFile mediaFile = uploadResult(data, faceSwap);

                // Update project
                updateProjectWithSuccess(faceSwap, mediaFile, processingTime);

                return FaceSwapResponse.builder()
                        .jobId(faceSwap.getId())
                        .status(FaceSwap.FaceSwapStatus.COMPLETED.name())
                        .outputUrl(mediaFile.getFileUrl())
                        .processingTimeMs(processingTime)
                        .message("Face swap completed successfully")
                        .build();
            }else{
                updateProjectStatus(faceSwap, FaceSwap.FaceSwapStatus.FAILED);
                return FaceSwapResponse.builder()
                        .jobId(faceSwap.getId())
                        .status(FaceSwap.FaceSwapStatus.FAILED.name())
                        .message("Face swap completed successfully")
                        .build();
            }
        } catch (Exception e) {
            log.error("Processing error for project: {}", faceSwap.getId(), e);
            faceSwap.setErrorMessage(e.getMessage());
            updateProjectStatus(faceSwap, FaceSwap.FaceSwapStatus.FAILED);
            throw new RuntimeException("Face swap processing failed", e);
        } finally {
            // Cleanup ALL files after processing
            deleteQuietly(videoFile);
            deleteQuietly(singleImageFile);
            if (srcFiles != null) srcFiles.forEach(this::deleteQuietly);
            if (dstFiles != null) dstFiles.forEach(this::deleteQuietly);
        }
    }

    private ResponseEntity<byte[]> callFastApiServiceMulti(String endpoint, List<File> srcFiles, List<File> dstFiles, File videoFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (File imageFile : srcFiles) {
            body.add("src_faces", new FileSystemResource(imageFile));
        }
        for (File imageFile : dstFiles) {
            body.add("dst_faces", new FileSystemResource(imageFile));
        }
        body.add("video", new FileSystemResource(videoFile));

        return executeApiCall(endpoint, body);
    }

    private ResponseEntity<byte[]> callFastApiService(String endpoint, File imageFile, File videoFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new FileSystemResource(imageFile));
        body.add("video", new FileSystemResource(videoFile));

        return executeApiCall(endpoint, body);
    }

    private ResponseEntity<byte[]> executeApiCall(String endpoint, MultiValueMap<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                fastApiBaseUrl + endpoint,
                HttpMethod.POST,
                requestEntity,
                byte[].class
        );
    }

    private MediaFile uploadResult(byte[] data, FaceSwap faceSwap) throws IOException {
        String fileName = "face_swap_result_" + faceSwap.getId() + ".mp4";

        /* Upload file to MinIO
        InputStream inputStream = new ByteArrayInputStream(data);
        String url = minioService.uploadFile(
                inputStream,
                data.length,
                fileName,
                bucketNameVideo,
                "video/mp4"
        );*/

        FileUploadResponse uploadResponse = cloudinaryService.uploadVideo(data);

        String url = uploadResponse.getSecureUrl();
        return saveResultFile(fileName, url, faceSwap, data.length, uploadResponse.getPublicId());
    }

    private File createTempFile(MultipartFile file, String prefix) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());
        File tempFile = File.createTempFile(prefix, extension);
        file.transferTo(tempFile);
        return tempFile;
    }

    private List<File> createTempFiles(List<MultipartFile> files, String prefix) throws IOException {
        return files.stream()
                .map(file -> {
                    try {
                        return createTempFile(file, prefix);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to create temp file", e);
                    }
                })
                .toList();
    }

    private String getFileExtension(String filename) {
        if (filename == null) return ".tmp";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".tmp";
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private MediaFile saveResultFile(String fileName, String fileUrl, FaceSwap faceSwap, long fileSize, String publicId) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileName(fileName);
        mediaFile.setPublicId(publicId);
        mediaFile.setOriginalName(fileName);
        mediaFile.setFilePath(fileUrl);
        mediaFile.setFileUrl(fileUrl);
        mediaFile.setFileType(MediaFile.FileType.VIDEO);
        mediaFile.setMimeType("video/mp4");
        mediaFile.setFileSize(fileSize);
        mediaFile.setUser(faceSwap.getUser());
        mediaFile.setProcessed(true);
        mediaFile.setBucketName(bucketNameVideo);
        mediaFile.setStorageType(MediaFile.StorageType.CLOUDINARY);

        return mediaFileRepository.save(mediaFile);
    }

    private void updateProjectWithSuccess(FaceSwap faceSwap, MediaFile mediaFile, long processingTime) {
        faceSwap.setStatus(FaceSwap.FaceSwapStatus.COMPLETED);
        faceSwap.setCompletedAt(LocalDateTime.now());
        faceSwap.setProcessingTimeMs(processingTime);
        faceSwap.setResultVideo(mediaFile);
        faceSwapRepository.save(faceSwap);
    }

    private void updateProjectStatus(FaceSwap faceSwap, FaceSwap.FaceSwapStatus status) {
        faceSwap.setStatus(status);
        faceSwapRepository.save(faceSwap);
    }

    private FaceSwapResponse buildResponse(Long jobId, String status, String message) {
        return FaceSwapResponse.builder()
                .jobId(jobId)
                .status(status)
                .message(message)
                .build();
    }

    @Transactional
    protected FaceSwapDetailResponse mapToDetailResponse(FaceSwap faceSwap){
        return FaceSwapDetailResponse.builder()
                .id(faceSwap.getId())
                .title(faceSwap.getTitle())
                .status(faceSwap.getStatus())
                .aiModelVersion(faceSwap.getAiModelVersion())
                .processingTimeMs(faceSwap.getProcessingTimeMs())
                .facesUrl(Optional.ofNullable(faceSwap.getFaceImages())
                        .orElse(List.of())
                        .stream()
                        .map(MediaFile::getFileUrl)
                        .toList())
                .targetUrl(faceSwap.getTargetVideo() != null ? faceSwap.getTargetVideo().getFileUrl() : null)
                .resultUrl(faceSwap.getResultVideo() != null ? faceSwap.getResultVideo().getFileUrl() : null)
                .swapType(faceSwap.getSwapType())
                .completedAt(faceSwap.getCompletedAt())
                .errorMessage(faceSwap.getErrorMessage())
                .updatedAt(faceSwap.getUpdatedAt())
                .createdAt(faceSwap.getCreatedAt())
                .build();
    };
}
