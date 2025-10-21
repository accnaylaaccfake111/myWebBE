package com.nckh.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.nckh.dto.request.MetaMusicGenerateRequest;
import com.nckh.dto.request.MusicGenerateRequest;
import com.nckh.dto.request.SunoCallBackRequest;
import com.nckh.dto.request.SunoGenerateRequest;
import com.nckh.dto.response.FileUploadResponse;
import com.nckh.dto.response.MusicGenerateResponse;
import com.nckh.entity.*;
import com.nckh.repository.*;
import com.nckh.service.CloudinaryService;
import com.nckh.service.MusicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicServiceImpl implements MusicService {
    private final RestTemplate restTemplate;
    private final LyricsCompositionRepository lyricRepository;
    private final SheetMusicRepository sheetMusicRepository;
    private final UserRepository userRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MusicGenTaskRepository musicGenTaskRepository;
    private final CloudinaryService cloudinaryService;

    private static final int MINIMUM_CREDITS_REQUIRED = 12;

    @Value("${ai-service.suno.api-key}")
    private String sunoApiKey;

    @Value("${ai-service.suno.url}")
    private String sunoApiUrl;

    @Value("${ai-service.suno.call-back-url}")
    private String sunoApiCallbackUrl;

    @Value("${ai-service.musicgen.url}")
    private String musicGenUrl;

    @Value("${minio.bucket.audio}")
    private String bucketNameAudio;

    // ThreadPool cho MusicGen
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(10);

    @Override
    @Transactional
    public MusicGenerateResponse generateMusic(MusicGenerateRequest request) {
        log.info("Starting music generation process");

        User user = findUser(request.getUserName());

        SheetMusic sheetMusic = createSheetMusic(request.getLyricId(), user);
        int duration = estimateDurationFromLyrics(sheetMusic.getLyricsComposition().getLyrics());
        request.setDuration(duration);
        sheetMusic.setDuration(duration);

        // Tạo internal task ID
        String taskId = UUID.randomUUID().toString();

        int availableCredits = getCredits();
        String prompt = buildPrompt(request.getMood(), request.getTheme());

        if (availableCredits > MINIMUM_CREDITS_REQUIRED) {
            sheetMusic.setMusicModelAI("SUNO_V5");
            return generateWithSunoAI(sheetMusic, taskId, prompt);
        } else {
            sheetMusic.setMusicModelAI("MUSICGEN");
            return generateWithMusicGen(sheetMusic, taskId, prompt, request.getDuration());
        }
    }

    @Transactional
    protected MusicGenerateResponse generateWithSunoAI(SheetMusic sheetMusic, String taskId, String prompt) {
        log.info("Generating music with Suno AI for Sheet Music: {}", sheetMusic.getId());

        // Tạo task record
        MusicGenTask task = createMusicGenTask(sheetMusic, taskId, MusicGenTask.MusicProvider.SUNO, prompt, 30);

        // Build request
        SunoGenerateRequest sunoRequest = buildGenerateRequest(prompt);
        String url = sunoApiUrl;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(sunoApiKey);
        HttpEntity<SunoGenerateRequest> entity = new HttpEntity<>(sunoRequest, headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);

            if (response.getBody() == null ||
                    response.getBody().get("data") == null ||
                    response.getBody().get("data").get("taskId") == null) {
                throw new RuntimeException("Invalid response from Suno AI API");
            }

            String externalTaskId = response.getBody().get("data").get("taskId").asText();

            // Update task với external ID
            task.setExternalTaskId(externalTaskId);
            task.setStatus(MusicGenTask.TaskStatus.PROCESSING);
            musicGenTaskRepository.save(task);

            log.info("Suno task created: internal={}, external={}", taskId, externalTaskId);

            return MusicGenerateResponse.builder()
                    .sheetMusicId(sheetMusic.getId())
                    .taskId(taskId)
                    .status("PROCESSING")
                    .message("Music generation is processing with Suno AI...")
                    .build();

        } catch (RestClientException e) {
            task.setStatus(MusicGenTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            musicGenTaskRepository.save(task);

            log.error("Error calling Suno AI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate music", e);
        }
    }

    @Transactional
    protected MusicGenerateResponse generateWithMusicGen(SheetMusic sheetMusic, String taskId,
                                                         String prompt, int duration) {
        log.info("Generating music with MusicGen for project: {}", sheetMusic.getId());

        // Tạo task record
        MusicGenTask task = createMusicGenTask(sheetMusic, taskId, MusicGenTask.MusicProvider.MUSICGEN, prompt, duration);
        task.setStatus(MusicGenTask.TaskStatus.PROCESSING);
        musicGenTaskRepository.save(task);

        // Chạy async trong background
        processMusicGenAsync(task);

        return MusicGenerateResponse.builder()
                .sheetMusicId(sheetMusic.getId())
                .taskId(taskId)
                .status("PROCESSING")
                .message("Music generation is processing with MusicGen...")
                .build();
    }

    @Async
    @Transactional
    protected void processMusicGenAsync(MusicGenTask task) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting MusicGen processing for task: {}", task.getTaskId());

                // Call MusicGen API (blocking)
                byte[] resultData = callMusicGenApi(task.getPrompt(), task.getDuration());

                // Upload và lưu kết quả
                completeGenTask(task, resultData);
            } catch (Exception e) {
                log.error("MusicGen processing failed for task: {}", task.getTaskId(), e);
                failMusicGenTask(task, e.getMessage());
            }
        }, taskExecutor);
    }

    protected void completeGenTask(MusicGenTask task, byte[] resultData) {
        try {
            SheetMusic sheetMusic = task.getSheetMusic();

            // Upload file
            MediaFile mediaFile = uploadResult(resultData, sheetMusic);

            // Update task
            task.setStatus(MusicGenTask.TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            musicGenTaskRepository.save(task);
            long processingTime = java.time.Duration.between(
                    task.getCreatedAt(),
                    LocalDateTime.now()
            ).toMillis();

            // Update project
            sheetMusic.setStatus(SheetMusic.SheetMusicStatus.MUSIC_COMPLETED);
            sheetMusic.setCompletedAt(LocalDateTime.now());
            sheetMusic.setProcessingTimeMs(processingTime);
            sheetMusic.setAudioFile(mediaFile);
            sheetMusicRepository.save(sheetMusic);

            log.info("MusicGen task completed successfully: {}", task.getTaskId());

        } catch (Exception e) {
            log.error("Error completing MusicGen task: {}", task.getTaskId(), e);
            failMusicGenTask(task, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void processCompletedGeneration(SunoCallBackRequest request) {
        String externalTaskId = request.getData().getTaskId();
        log.info("Processing Suno callback for taskId: {}", externalTaskId);

        MusicGenTask task = musicGenTaskRepository.findByExternalTaskId(externalTaskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + externalTaskId));

        try {
            String audioUrl = request.getData().getData().get(0).getAudio_url();
            if (audioUrl == null || audioUrl.isEmpty()) {
                throw new RuntimeException("No audio URL in Suno callback");
            }else{
                // Update status to downloading
                task.setStatus(MusicGenTask.TaskStatus.DOWNLOADING);
                task.setAudioUrl(audioUrl);
                musicGenTaskRepository.save(task);

                // Download audio từ Suno
                log.info("Downloading audio from: {}", audioUrl);
                byte[] audioData = downloadMusic(audioUrl);

                // Upload và lưu kết quả
                completeGenTask(task, audioData);
                log.info("Successfully processed Suno callback for taskId: {}", externalTaskId);
            }
        } catch (Exception e) {
            log.error("Error processing Suno callback: {}", e.getMessage(), e);
            failMusicGenTask(task, e.getMessage());
            throw new RuntimeException("Failed to process completed generation", e);
        }
    }

    @Override
    @Transactional
    public void processFailedGeneration(SunoCallBackRequest request) {
        String externalTaskId = request.getData().getTaskId();
        log.error("Processing failed Suno generation for taskId: {}", externalTaskId);

        MusicGenTask task = musicGenTaskRepository.findByExternalTaskId(externalTaskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + externalTaskId));

        String errorMessage = request.getMsg() != null
                ? request.getMsg()
                : "Unknown error from Suno API";

        failMusicGenTask(task, errorMessage);

        log.info("Successfully processed failed Suno generation for taskId: {}", externalTaskId);
    }

    private void failMusicGenTask(MusicGenTask task, String errorMessage) {
        task.setStatus(MusicGenTask.TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setCompletedAt(LocalDateTime.now());
        musicGenTaskRepository.save(task);

        SheetMusic sheetMusic = task.getSheetMusic();
        sheetMusic.setStatus(SheetMusic.SheetMusicStatus.MUSIC_FAILED);
        sheetMusicRepository.save(sheetMusic);
    }

    @Transactional(readOnly = true)
    public MusicGenerateResponse getTaskStatus(String taskId) {
        MusicGenTask task = musicGenTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        SheetMusic sheetMusic = task.getSheetMusic();

        MusicGenerateResponse.MusicGenerateResponseBuilder builder = MusicGenerateResponse.builder()
                .sheetMusicId(sheetMusic.getId())
                .taskId(task.getTaskId())
                .status(task.getSheetMusic().getStatus().name());

        if (task.getStatus() == MusicGenTask.TaskStatus.COMPLETED) {
            builder.outputUrl(sheetMusic.getAudioFile().getFileUrl());
            builder.sheetMusic(sheetMusic.getSheetMusicXML());
            builder.message("Music generation completed successfully.");
        } else if (task.getStatus() == MusicGenTask.TaskStatus.FAILED) {
            builder.message(task.getErrorMessage());
        } else {
            builder.message("Music generation is in progress...");
        }

        return builder.build();
    }

    private MusicGenTask createMusicGenTask(SheetMusic sheetMusic, String taskId, MusicGenTask.MusicProvider provider, String prompt, int duration) {
        MusicGenTask task = new MusicGenTask();
        task.setTaskId(taskId);
        task.setSheetMusic(sheetMusic);
        task.setProvider(provider);
        task.setStatus(MusicGenTask.TaskStatus.PENDING);
        task.setPrompt(prompt);
        task.setDuration(duration);
        return musicGenTaskRepository.save(task);
    }

    private byte[] callMusicGenApi(String prompt, int duration) {
        String url = musicGenUrl + "/generate-music/";
        MetaMusicGenerateRequest request = MetaMusicGenerateRequest.builder()
                .prompt(prompt)
                .duration(duration)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));

        HttpEntity<MetaMusicGenerateRequest> requestEntity = new HttpEntity<>(request, headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, byte[].class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException("MusicGen returned error: " + response.getStatusCode());
        }

        return response.getBody();
    }

    public byte[] downloadMusic(String audioUrl) {
        log.info("Downloading music from: {}", audioUrl);

        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(audioUrl, byte[].class);

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response when downloading music");
            }

            log.info("Successfully downloaded music, size: {} bytes", response.getBody().length);
            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error downloading music from {}: {}", audioUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to download music", e);
        }
    }

    private int getCredits() {
        String url = sunoApiUrl + "/credit";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(sunoApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JsonNode.class);

            if (response.getBody() == null ||
                    response.getBody().get("data") == null) {
                throw new RuntimeException("Invalid response when fetching credits");
            }

            int credits = response.getBody().get("data").asInt();
            log.debug("Available credits: {}", credits);
            return credits;

        } catch (RestClientException e) {
            log.error("Error getting credits: {}", e.getMessage(), e);
            return 0;
        }
    }

    private int estimateDurationFromLyrics(String lyrics) {
        String[] lines = lyrics.split("\\n");
        int totalSeconds = 0;

        double secondsPerBeat = 60.0 / 70;  // tempo = 80–100 BPM
        double avgBeatsPerSyllable = 0.5;          // 2 âm tiết = 1 phách

        for (String line : lines) {
            if (line.isBlank()) continue;
            int syllables = countVietnameseSyllables(line);
            double beats = syllables * avgBeatsPerSyllable;
            totalSeconds += (int) (beats * secondsPerBeat);
        }

        // Giới hạn thời lượng MusicGen
        return Math.max(15, Math.min(totalSeconds, 60));
    }

    private int countVietnameseSyllables(String text) {
        String cleaned = text.replaceAll("[^\\p{L}\\s]", "").trim();
        if (cleaned.isEmpty()) return 0;
        return cleaned.split("\\s+").length;
    }

    private SunoGenerateRequest buildGenerateRequest(String prompt) {
        return SunoGenerateRequest.builder()
                .prompt(prompt)
                .title("Generate Music")
                .customMode(true)
                .instrumental(true)
                .model("V5")
                .negativeTags("Heavy Metal, Rock guitars, Upbeat EDM drums, Hip Hop beats, Jazz")
                .vocalGender("m")
                .styleWeight(0.9f)
                .weirdnessConstraint(0.2f)
                .audioWeight(0.65f)
                .callBackUrl(sunoApiCallbackUrl)
                .build();
    }

    private String buildPrompt(String mood, String theme) {
        return """
                Create music based on Hát Sắc Bùa Phú Lễ – a traditional folk performing art of Vietnam. Use the
                characteristic instruments: đàn cò (two-string fiddle), trống cơm (rice drum), sanh cái and sanh
                tiền (traditional clappers).
                The sound must reflect Vietnamese folk tradition, suitable for festivals and community performances.
                """ + "Mood: " + getMoodGuidance(mood) + "/n/n" + "Theme: " + getThemeGuidance(theme);
    }

    private String getThemeGuidance(String theme) {
        return switch (theme.toLowerCase()) {
            case "mở cửa", "mở rào", "mở ngõ", "khai môn" -> """
            - Instruments: ceremonial drums, gongs, wooden clappers
            - Rhythm: energetic, pounding, like opening a gateway
            - Atmosphere: bright, welcoming, festive
        """;
            case "chúc gia chủ", "chúc tụng" -> """
            - Instruments: fiddle, bamboo flute, light percussion
            - Rhythm: gentle, steady, warm
            - Atmosphere: harmonious, affectionate, family gathering mood
        """;
            case "chúc mùa màng", "ban lộc" -> """
            - Instruments: rice drum, monochord, flute
            - Rhythm: joyful, lively, bouncy
            - Atmosphere: abundant, fertile, full of life
        """;
            case "cầu an", "cầu phúc" -> """
            - Instruments: temple bell, moon lute, wooden fish drum
            - Rhythm: slow, solemn
            - Atmosphere: sacred, meditative, respectful
        """;
            case "quê hương", "ca ngợi quê hương" -> """
            - Instruments: kite flute, zither, rice drum
            - Rhythm: flowing, graceful
            - Atmosphere: nostalgic, pastoral, proud of the homeland
        """;
            case "tiễn biệt", "kết thúc" -> """
            - Instruments: fading drumbeats, soft flute
            - Rhythm: slowing down, tapering off
            - Atmosphere: lingering, tender, closing ceremony
        """;
            default -> """
            - Instruments: flute, rice drum, monochord
            - Rhythm: adaptive, fitting the moment
            - Atmosphere: joyful, communal, folk-inspired
        """;
        };
    }

    private String getMoodGuidance(String mood) {
        return switch (mood.toLowerCase()) {
            case "vui tươi", "phấn khởi", "rộn ràng" -> """
            - Rhythm: fast, energetic, driving
            - Instruments: drums, cymbals, horns
            - Atmosphere: bustling, cheerful, full of life
        """;
            case "trang nghiêm", "thành kính" -> """
            - Rhythm: slow, steady
            - Instruments: bell, fiddle, wooden percussion
            - Atmosphere: sacred, ritualistic, calm
        """;
            case "ân cần", "thân mật" -> """
            - Rhythm: smooth, moderate
            - Instruments: bamboo flute, monochord
            - Atmosphere: warm, heartfelt, close
        """;
            case "dí dỏm", "hóm hỉnh" -> """
            - Rhythm: quick, with quirky syncopation
            - Instruments: small drums, clappers, fiddle
            - Atmosphere: cheerful, teasing, lighthearted
        """;
            case "tự hào", "yêu quê hương" -> """
            - Rhythm: strong, steady, uplifting
            - Instruments: large drum, horn, moon lute
            - Atmosphere: bold, inspiring, full of pride
        """;
            default -> """
            - Rhythm: moderate, natural
            - Instruments: flute, rice drum
            - Atmosphere: simple, folk-like, communal
        """;
        };
    }

    private SheetMusic createSheetMusic(Long lyricId, User user) {
        LyricsComposition lyric = lyricRepository.findById(lyricId)
                .orElseThrow(() -> new RuntimeException("Lyrics not found"));

        SheetMusic sheetMusic = SheetMusic.builder()
                .lyricsComposition(lyric)
                .title(lyric.getTitle())
                .status(SheetMusic.SheetMusicStatus.DRAFT)
                .user(user)
                .build();
        return sheetMusicRepository.save(sheetMusic);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private MediaFile uploadResult(byte[] data, SheetMusic sheetMusic) throws IOException {
        String fileName = "music_gen_" + sheetMusic.getId() + "_" + System.currentTimeMillis() + ".mp3";

        /* Upload file to MinIO
        InputStream inputStream = new ByteArrayInputStream(data);
        String url = minioService.uploadFile(
                inputStream,
                data.length,
                fileName,
                bucketNameAudio,
                "audio/mpeg"
        );*/

        FileUploadResponse resultUpload = cloudinaryService.uploadAudio(data);
        String url = resultUpload.getSecureUrl();
        return saveResultFile(fileName, url, sheetMusic, data.length, resultUpload.getPublicId());
    }

    private MediaFile saveResultFile(String fileName, String fileUrl, SheetMusic sheetMusic, long fileSize, String publicId) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileName(fileName);
        mediaFile.setPublicId(publicId);
        mediaFile.setOriginalName(fileName);
        mediaFile.setFilePath(fileUrl);
        mediaFile.setFileUrl(fileUrl);
        mediaFile.setFileType(MediaFile.FileType.AUDIO);
        mediaFile.setMimeType("audio/mpeg");
        mediaFile.setFileSize(fileSize);
        mediaFile.setUser(sheetMusic.getUser());
        mediaFile.setSheetMusic(sheetMusic);
        mediaFile.setProcessed(true);
        mediaFile.setBucketName(bucketNameAudio);
        mediaFile.setStorageType(MediaFile.StorageType.CLOUDINARY);

        return mediaFileRepository.save(mediaFile);
    }
}