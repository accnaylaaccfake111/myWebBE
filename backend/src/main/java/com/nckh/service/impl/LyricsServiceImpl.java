package com.nckh.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nckh.dto.request.LyricsGenerateRequest;
import com.nckh.dto.response.LyricsResponse;
import com.nckh.entity.LyricsComposition;
import com.nckh.entity.LyricsTemplate;
import com.nckh.entity.User;
import com.nckh.exception.ResourceNotFoundException;
import com.nckh.repository.LyricsCompositionRepository;
import com.nckh.repository.LyricsTemplateRepository;
import com.nckh.repository.UserRepository;
import com.nckh.service.IntegrateAIService;
import com.nckh.service.LyricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LyricsServiceImpl implements LyricsService {
    private final IntegrateAIService integrateAIService;
    private final LyricsCompositionRepository lyricsRepository;
    private final LyricsTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    // Vietnamese lyrics templates
    private static final Map<String, List<String>> THEME_TEMPLATES = new HashMap<>();
    
    static {
        // Mùa xuân templates
        THEME_TEMPLATES.put("khai môn", Arrays.asList(
            "Mở ngõ mở ngô, trong nhà ơ đèn tỏ cậu mỏ của ra\nChúng tôi là bạn sắc bùa\nNăm cũ bước tới năm mới buớc sang\nNhà cậu mợ giăng giăng coi đà lịch sự\nCon trai hay chữ con gái giỏi giang\nCậu mợ hiền lành mai sau gặp phước ở.",

            "Nhà ông cửa kín rào cao Dán liễn đèn màu câu đổi tứ giăng\nNha ông đón Tết khang trang, con chau quay quan bái tạ to tiên\nNhà ông kín cồng cao tường, mở rào ông đón lộc xuân sắc bùa\nMở rào đón giao thừa, nghinh tân rồi tiền cựu ám no an lành\nNhà .. ... no an lành."
        ));

        // Chúc tụng templates
        THEME_TEMPLATES.put("chúc tụng", Arrays.asList(
            "Chúc cho ông bà mạnh giỏi bình an\nCon cháu sum vầy, nhà cửa rộn ràng\nXuân sang tài lộc vô nhà\nĐông con nhiều cháu thuận hòa ấm êm\nLúa ngoài đồng xanh tươi hứa hẹn\nTrâu bò đầy chuồng, thóc gạo đầy kho\nMột năm no ấm đủ no\nCầu cho vạn sự vuông tròn tốt tươi.",

            "Đầu năm con cháu kính chào\nChúc ông bà sống lâu trăm tuổi\nCha mẹ mạnh khỏe vui vầy\nAnh em hoà thuận ấm no\nĐồng xanh lúa thóc đầy bồ\nTrên bến dưới thuyền hàng hóa dồi dào\nCầu cho gia đạo an khang\nMột năm phát đạt giàu sang thịnh vượng."
        ));

        // Quê hương templates
        THEME_TEMPLATES.put("quê hương", Arrays.asList(
            "Quê mình có ruộng có đồng\nCó con sông chảy bên làng\nNgười dân lao động cần cù\nSớm hôm tay cấy tay cày\nMỗi mùa lúa chín vàng đầy\nTiếng ca vọng khắp, tình quê đậm đà.",

            "Quê hương đất mẹ hiền hòa\nBến sông cây trái mặn mà tình thương\nAi đi muôn dặm tha hương\nCũng mong ngày Tết quay về sum vầy\nDẫu xa nhớ mãi chẳng quên\nTình quê son sắt, ơn trên vun bồi."
        ));

        // Tiễn biệt templates
        THEME_TEMPLATES.put("tiễn biệt", Arrays.asList(
            "Hôm nay hát sắc bùa xong\nXin chào ông bà, kính chúc an khang\nBước chân chia biệt đôi đàng\nHẹn mùa xuân tới lại sang hát mừng\nRa về xin chút quà xuân\nCâu ca giữ nghĩa, tình dân mặn nồng.",

            "Xuân nay ta đến hát vui\nChúc cho gia đạo sáng tươi phúc lành\nGiờ thì chia nẻo chân tình\nTạm xa lưu luyến, hẹn ngày gặp nhau\nRa về dạ những khát khao\nSắc bùa còn mãi ngọt ngào đầu năm."
        ));
    }
    
    @Override
    public LyricsResponse generateLyrics(LyricsGenerateRequest request, String username) {
        long startTime = System.currentTimeMillis();
        
        try {
            String theme = request.getTheme();
            
            String lyrics;
            String generationMethod;
            LyricsTemplate usedTemplate = null;
            
            // Check if we should use AI or templates
            if (Boolean.TRUE.equals(request.getUseAI())) {
                // Use GeminiAI to generate lyrics
                lyrics = generateWithGeniniAI(request);
                generationMethod = "GEMINI_AI";
            } else {
                // Use template-based generation
                Optional<LyricsTemplate> template = findBestTemplate(theme, request.getMood());
                
                if (template.isPresent()) {
                    usedTemplate = template.get();
                    lyrics = processTemplate(usedTemplate);
                    generationMethod = "TEMPLATE";
                    
                    // Increment usage count
                    templateRepository.incrementUsageCount(usedTemplate.getId());
                } else {
                    // Fallback to hardcoded templates
                    lyrics = generateFromHardcodedTemplates(theme, request.getMood());
                    generationMethod = "HARDCODED";
                }
            }
            
            // Post-process lyrics
            lyrics = postProcessLyrics(lyrics, request);
            
            // Calculate metrics
            String[] lines = lyrics.split("\n");
            int lineCount = lines.length;
            int wordCount = Arrays.stream(lines)
                .mapToInt(line -> line.split("\\s+").length)
                .sum();
            double rhymeScore = calculateRhymeScore(lyrics);
            
            // Save to database
            LyricsComposition composition = LyricsComposition.builder()
                .theme(theme)
                .mood(request.getMood())
                .title(request.getTitle())
                .lyrics(lyrics)
                .formattedLyrics(formatLyrics(lyrics))
                .lineCount(lineCount)
                .wordCount(wordCount)
                .rhymeScore(rhymeScore)
                .generationMethod(generationMethod)
                .generationTimeMs(System.currentTimeMillis() - startTime)
                .templateId(usedTemplate != null ? usedTemplate.getId() : null)
                .build();
            
            if (username != null && !"anonymous".equals(username)) {
                User user = userRepository.findByUsername(username)
                    .orElse(null);
                composition.setUser(user);
            }
            
            LyricsComposition saved = lyricsRepository.save(composition);
            
            // Build response
            return buildLyricsResponse(saved);
            
        } catch (Exception e) {
            log.error("Error generating lyrics", e);
            throw new RuntimeException("Failed to generate lyrics: " + e.getMessage());
        }
    }

    public String generateWithGeniniAI(LyricsGenerateRequest request) {
        StringBuilder lyrics = new StringBuilder();
        String generatedLyrics = integrateAIService.generateLyrics(request);
        lyrics.append(generatedLyrics);

        // Add generated ending
        lyrics.append("\n\n[AI Generated - Theme: ").append(request.getTheme());
        if (request.getMood() != null) {
            lyrics.append(", Mood: ").append(request.getMood());
        }
        lyrics.append("]");

        return lyrics.toString();
    }
    
    private String generateFromHardcodedTemplates(String theme, String mood) {
        String normalizedTheme = normalizeTheme(theme);
        List<String> templates = THEME_TEMPLATES.getOrDefault(normalizedTheme, THEME_TEMPLATES.get("spring"));
        
        // Select random template
        Random random = new Random();
        String selectedTemplate = templates.get(random.nextInt(templates.size()));
        
        // Apply mood modifications if specified
        if (mood != null) {
            selectedTemplate = applyMoodToLyrics(selectedTemplate, mood);
        }
        
        return selectedTemplate;
    }
    
    private String normalizeTheme(String theme) {
        if (theme == null) return "spring";
        
        String lower = theme.toLowerCase();
        if (lower.contains("mở") || lower.contains("mở rào") || lower.contains("mở cửa") || lower.contains("mở ngõ") || lower.contains("khai môn")) return "khai môn";
        if (lower.contains("chúc") || lower.contains("chúc tụng")) return "chúc tụng";
        if (lower.contains("quê") || lower.contains("quê hương")) return "quê hương";
        if (lower.contains("tiễn biệt") || lower.contains("kết thúc")) return "tiễn biệt";
        
        return "chúc tụng"; // default
    }
    
    private String applyMoodToLyrics(String lyrics, String mood) {
        // Simple mood application - in real implementation, this would be more sophisticated
        if (mood.toLowerCase().contains("vui")) {
            return "(Giai điệu vui tươi)\n" + lyrics;
        } else if (mood.toLowerCase().contains("buồn")) {
            return "(Giai điệu trầm buồn)\n" + lyrics;
        } else if (mood.toLowerCase().contains("lãng mạn")) {
            return "(Giai điệu lãng mạn)\n" + lyrics;
        }
        return lyrics;
    }
    
    private Optional<LyricsTemplate> findBestTemplate(String theme, String mood) {
        if (mood != null && !mood.isEmpty()) {
            return templateRepository.findRandomByThemeAndMood(theme, mood);
        } else {
            return templateRepository.findRandomByTheme(theme);
        }
    }
    
    private String processTemplate(LyricsTemplate template) {
        String lyrics = template.getTemplate();
        
        // Replace placeholders if any
        if (template.getPlaceholders() != null) {
            try {
                List<String> placeholders = objectMapper.readValue(template.getPlaceholders(), List.class);
                for (String placeholder : placeholders) {
                    lyrics = lyrics.replace("{" + placeholder + "}", generatePlaceholderValue(placeholder));
                }
            } catch (Exception e) {
                log.error("Error processing placeholders", e);
            }
        }
        
        return lyrics;
    }
    
    private String generatePlaceholderValue(String placeholder) {
        // Generate contextual values for placeholders
        Map<String, List<String>> placeholderValues = Map.of(
            "season", Arrays.asList("xuân", "hạ", "thu", "đông"),
            "flower", Arrays.asList("hoa đào", "hoa mai", "hoa sen", "hoa hồng"),
            "emotion", Arrays.asList("vui vẻ", "hạnh phúc", "yêu thương", "ấm áp"),
            "time", Arrays.asList("sáng sớm", "chiều tà", "hoàng hôn", "bình minh")
        );
        
        List<String> values = placeholderValues.getOrDefault(placeholder.toLowerCase(),
                List.of(placeholder));
        return values.get(new Random().nextInt(values.size()));
    }
    
    private String postProcessLyrics(String lyrics, LyricsGenerateRequest request) {
        // Apply line limits if specified
        if (request.getMaxLines() != null) {
            String[] lines = lyrics.split("\n");
            if (lines.length > request.getMaxLines()) {
                lyrics = String.join("\n", Arrays.copyOfRange(lines, 0, request.getMaxLines()));
            }
        }
        
        return lyrics.trim();
    }
    
    private String formatLyrics(String lyrics) {
        // Format lyrics with proper line breaks and verse separation
        String[] lines = lyrics.split("\n");
        StringBuilder formatted = new StringBuilder();
        
        int lineCount = 0;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                formatted.append("\n");
                lineCount = 0;
            } else {
                formatted.append(line).append("\n");
                lineCount++;
                if (lineCount == 4) {
                    formatted.append("\n");
                    lineCount = 0;
                }
            }
        }
        
        return formatted.toString().trim();
    }
    
    private double calculateRhymeScore(String lyrics) {
        // Simple rhyme scoring algorithm
        String[] lines = lyrics.split("\n");
        int rhymeCount = 0;
        int totalPairs = 0;
        
        for (int i = 0; i < lines.length - 1; i += 2) {
            if (i + 1 < lines.length) {
                totalPairs++;
                if (checkRhyme(lines[i], lines[i + 1])) {
                    rhymeCount++;
                }
            }
        }
        
        return totalPairs > 0 ? (rhymeCount * 100.0 / totalPairs) : 0;
    }
    
    private boolean checkRhyme(String line1, String line2) {
        // Simple rhyme check - check last syllable
        if (line1.isEmpty() || line2.isEmpty()) return false;
        
        String[] words1 = line1.trim().split("\\s+");
        String[] words2 = line2.trim().split("\\s+");
        
        if (words1.length == 0 || words2.length == 0) return false;
        
        String lastWord1 = words1[words1.length - 1].toLowerCase();
        String lastWord2 = words2[words2.length - 1].toLowerCase();
        
        // Check if last 2-3 characters match (simple rhyme)
        if (lastWord1.length() >= 2 && lastWord2.length() >= 2) {
            String ending1 = lastWord1.substring(lastWord1.length() - 2);
            String ending2 = lastWord2.substring(lastWord2.length() - 2);
            return ending1.equals(ending2);
        }
        
        return false;
    }
    
    private LyricsResponse buildLyricsResponse(LyricsComposition composition) {
        String[] lines = composition.getLyrics().split("\n");
        
        // Group lines into verses (4 lines per verse)
        List<List<String>> verses = new ArrayList<>();
        List<String> currentVerse = new ArrayList<>();
        
        for (String line : lines) {
            if (line.trim().isEmpty() && !currentVerse.isEmpty()) {
                verses.add(new ArrayList<>(currentVerse));
                currentVerse.clear();
            } else if (!line.trim().isEmpty()) {
                currentVerse.add(line);
                if (currentVerse.size() == 4) {
                    verses.add(new ArrayList<>(currentVerse));
                    currentVerse.clear();
                }
            }
        }
        
        if (!currentVerse.isEmpty()) {
            verses.add(currentVerse);
        }
        
        String[][] lyricsVerses = verses.stream()
            .map(verse -> verse.toArray(new String[0]))
            .toArray(String[][]::new);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("templateId", composition.getTemplateId());
        metadata.put("generatedAt", LocalDateTime.now().toString());
        
        return LyricsResponse.builder()
            .id(composition.getId())
            .theme(composition.getTheme())
            .mood(composition.getMood())
            .style(composition.getStyle())
            .lyrics(composition.getLyrics())
            .formattedLyrics(composition.getFormattedLyrics())
            .lineCount(composition.getLineCount())
            .wordCount(composition.getWordCount())
            .rhymeScore(composition.getRhymeScore())
            .generationMethod(composition.getGenerationMethod())
            .generationTimeMs(composition.getGenerationTimeMs())
            .createdAt(composition.getCreatedAt())
            .createdBy(composition.getUser() != null ? composition.getUser().getUsername() : "anonymous")
            .rating(composition.getRating())
            .metadata(metadata)
            .lyricsLines(lines)
            .lyricsVerses(lyricsVerses)
            .build();
    }
    
    @Override
    public List<LyricsResponse> getUserLyricsHistory(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<LyricsComposition> compositions = lyricsRepository.findTop10ByUserOrderByCreatedAtDesc(user);
        
        return compositions.stream()
            .map(this::buildLyricsResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable("available-themes")
    public List<String> getAvailableThemes() {
        List<String> themes = Arrays.asList(
            "mở cửa", "mở rào", "mở ngõ", "khai môn", "chúc gia chủ", "chúc tụng", "chúc mùa màng",
            "ban lộc", "cầu an", "cầu phúc", "quê hương", "ca ngợi quê hương", "tiễn biệt", "kết thúc"
        );
        
        // Add themes from database
        List<String> dbThemes = templateRepository.findDistinctThemes();
        themes.addAll(dbThemes);
        
        return themes.stream().distinct().collect(Collectors.toList());
    }
    
    @Override
    @Cacheable("available-moods")
    public List<String> getAvailableMoods() {
        List<String> moods = Arrays.asList(
            "vui tươi", "phấn khởi", "rộn ràng", "trang nghiêm", "thành kính",
            "thân mật", "dí dỏm", "hóm hỉnh", "tự hào", "yêu quê hương", "ân cần"
        );
        
        // Add moods from database
        List<String> dbMoods = templateRepository.findDistinctMoods();
        moods = new ArrayList<>(moods);
        moods.addAll(dbMoods);
        
        return moods.stream().distinct().collect(Collectors.toList());
    }
    
    @Override
    public void saveLyricsAsProject(Long lyricsId, String username) {
        LyricsComposition composition = lyricsRepository.findById(lyricsId)
            .orElseThrow(() -> new ResourceNotFoundException("Lyrics not found"));
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify ownership
        if (!composition.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to lyrics");
        }
        
        composition.setIsSavedAsProject(true);
        lyricsRepository.save(composition);
    }
    
    @Override
    public void rateLyrics(Long lyricsId, Integer rating, String username) {
        LyricsComposition composition = lyricsRepository.findById(lyricsId)
            .orElseThrow(() -> new ResourceNotFoundException("Lyrics not found"));
        
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        composition.setRating(rating);
        lyricsRepository.save(composition);
        
        log.info("User {} rated lyrics {} with {} stars", username, lyricsId, rating);
    }
    
    @Override
    public LyricsResponse getLyricsById(Long id) {
        LyricsComposition composition = lyricsRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lyrics not found"));
        
        return buildLyricsResponse(composition);
    }
    
    @Override
    public void deleteLyrics(Long id, String username) {
        LyricsComposition composition = lyricsRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lyrics not found"));
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify ownership
        if (!composition.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to lyrics");
        }
        
        lyricsRepository.delete(composition);
    }
}