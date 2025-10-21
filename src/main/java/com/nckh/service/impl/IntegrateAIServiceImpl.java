package com.nckh.service.impl;

import com.nckh.dto.request.LyricsGenerateRequest;
import com.nckh.service.IntegrateAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrateAIServiceImpl implements IntegrateAIService {
    private final ChatModel chatModel;

    @Override
    public String generateLyrics(LyricsGenerateRequest request) {
        try {
            String prompt = buildLyricsPrompt(request);

            Prompt aiPrompt = new Prompt(prompt);
            ChatResponse response = callAIModel(aiPrompt);

            String generatedLyrics = extractLyricsFromResponse(response);
            return postProcessAILyrics(generatedLyrics);

        } catch (Exception e) {
            log.error("Error generating lyrics with AI: ", e);
            throw new RuntimeException("AI generation failed: " + e.getMessage());
        }
    }

    private ChatResponse callAIModel(Prompt prompt) {
        try {
            return chatModel.call(prompt);
        } catch (Exception e) {
            log.error("AI model call failed", e);
            throw new RuntimeException("AI model call failed", e);
        }
    }

    private String extractLyricsFromResponse(ChatResponse response) {
        if (response == null || response.getResult() == null ||
                response.getResult().getOutput() == null) {
            throw new RuntimeException("Invalid response from AI model");
        }

        String lyrics = response.getResult().getOutput().getText();
        if (lyrics == null || lyrics.trim().isEmpty()) {
            throw new RuntimeException("AI model returned empty lyrics");
        }

        return lyrics;
    }

    private String buildLyricsPrompt(LyricsGenerateRequest request) {
        StringBuilder prompt = new StringBuilder();

        // System instruction for Sắc Bùa
        prompt.append("Bạn là một nghệ nhân dân gian hát **SẮC BÙA PHÚ LỄ** – loại hình diễn xướng Nam Bộ,");

        // Nhiệm vụ
        prompt.append("Nhiệm vụ: Hãy sáng tác một bài hát Sắc Bùa bằng tiếng Việt, ngắn gọn, mộc mạc, đậm chất dân gian. ");
        prompt.append("đúng tinh thần dân ca, chỉ trả về phần lời hát, không kèm giải thích.\n\n");

        // Đặc trưng
        prompt.append("ĐẶC TRƯNG SẮC BÙA:\n");
        prompt.append("- Ngôn ngữ mộc mạc, dân dã, có thể pha chất địa phương.\n");
        prompt.append("- Nội dung gắn với mùa xuân, Tết, lao động sản xuất.\n");
        prompt.append("- Câu hát ngắn gọn, 6–8 chữ, có thể lặp lại để tạo nhịp điệu.\n");

        prompt.append("TIÊU ĐỀ BÀI HÁT: ").append(request.getTitle());

        // Theme specification
        prompt.append("CHỦ ĐỀ: ").append(request.getTheme()).append("\n");
        prompt.append(getThemeGuidance(request.getTheme())).append("\n\n");

        // Mood specification
        if (request.getMood() != null && !request.getMood().isEmpty()) {
            prompt.append("TÂM TRẠNG: ").append(request.getMood()).append("\n");
            prompt.append(getMoodGuidance(request.getMood())).append("\n");
        }

        prompt.append("SỐ CÂU: ").append("Tối thiểu: ").append(request.getMinLines())
                .append(", ").append("Tối đa: ").append(request.getMaxLines()).append("\n");

        prompt.append("LƯU Ý: ").append(request.getNote());

        prompt.append("Ngôn từ gần gũi gắn liền với người dân phú lễ - bến tre.\n");
        prompt.append("Bắt đầu sáng tác ngay. Chỉ viết lời bài hát, không tiêu đề, không giải thích gì thêm:\n");

        return prompt.toString();
    }

    private String getThemeGuidance(String theme) {
        return switch (theme.toLowerCase()) {
            case "mở cửa", "mở rào", "mở ngõ", "khai môn" -> """
                - Xin phép gia chủ mở cửa, mở rào để đoàn vào hát
                - Hình ảnh: cửa rào, cổng ngõ, đèn sáng, liễn đỏ
                - Ý nghĩa: đón xuân mới, nghinh phúc lộc vào nhà
                - Lời chúc: mở cửa đón may mắn, tài lộc, bình an
            """;
            case "chúc gia chủ", "chúc tụng" -> """
                - Chúc gia chủ, gia đạo an khang, thịnh vượng
                - Hình ảnh: ông bà, cha mẹ, con cháu quây quần
                - Ý nghĩa: sum họp, hiếu đạo, phúc lộc đầy nhà
                - Lời chúc: gia đình hòa thuận, con cháu hiếu thảo
            """;
            case "chúc mùa màng", "ban lộc" -> """
                - Chúc nông nghiệp trù phú, mùa màng bội thu
                - Hình ảnh: lúa chín vàng, trâu bò đầy đàn, gà vịt sum xuê
                - Ý nghĩa: làm ăn phát đạt, kho lẫm đầy ắp
                - Lời chúc: lúa nhiều thóc đầy, gia súc khỏe mạnh
            """;
            case "cầu an", "cầu phúc" -> """
                - Cầu cho quốc thái dân an, mưa thuận gió hòa
                - Hình ảnh: đình làng, lễ hội, bàn thờ tổ tiên
                - Ý nghĩa: cầu phúc cho cả cộng đồng, xóm làng
                - Lời chúc: trời yên biển lặng, dân an vật thịnh
            """;
            case "quê hương", "ca ngợi quê hương" -> """
                - Ca ngợi phong cảnh, con người, tình làng nghĩa xóm
                - Hình ảnh: cây đa, bến nước, mái đình, ruộng lúa, sông quê
                - Ý nghĩa: tự hào cội nguồn, tình yêu đất nước
                - Lời chúc: quê hương phồn thịnh, bền vững lâu dài
            """;
            case "tiễn biệt", "kết thúc" -> """
                - Kết thúc buổi hát, tiễn đoàn ra về
                - Hình ảnh: ánh trăng, con đường làng, trống dứt nhịp
                - Ý nghĩa: lời cảm ơn, lời tiễn, hẹn năm sau
                - Lời chúc: tiễn đưa trong an vui, phúc lộc còn mãi
            """;
            default -> """
                - Tập trung vào chúc tụng, cầu an, ca ngợi quê hương
                - Sử dụng hình ảnh dân gian gần gũi: ruộng đồng, cây đa, mái đình
                - Giữ tinh thần vui tươi, rộn ràng, gắn kết cộng đồng
            """;
        };

    }

    private String getMoodGuidance(String mood) {
        return switch (mood.toLowerCase()) {
            case "vui tươi", "phấn khởi", "rộn ràng" -> """
                - Nhịp điệu nhanh, dồn dập
                - Mô tả không khí Tết, mùa màng bội thu, đoàn hát náo nhiệt
                - Dùng từ: rộn ràng, sum vầy, khởi sắc, hò reo""";
            case "trang nghiêm", "thành kính" -> """
                - Giọng điệu chậm rãi, đều đặn
                - Bối cảnh: đình làng, bàn thờ, lễ cầu an
                - Dùng từ: quốc thái, dân an, mưa thuận, gió hòa""";
            case "ân cần", "thân mật" -> """
                - Giọng gần gũi, chân thành
                - Nhấn mạnh tình cảm gia đình, làng xóm, tình người
                - Dùng từ: hiền hòa, chan chứa, sum họp, ấm áp""";
            case "dí dỏm", "hóm hỉnh" -> """
                - Có thể xen câu vui, tiếng đùa
                - Mô tả cảnh vật đời thường theo cách hài hước
                - Dùng từ: heo mập, gà kêu, trâu cày, vui lắm""";
            case "tự hào", "yêu quê hương" -> """
                - Giọng mạnh mẽ, khẳng định
                - Khen ngợi quê hương, con người, đất nước
                - Dùng từ: tự hào, xinh đẹp, nghĩa tình, giàu có""";
            default -> """
                - Giữ tông điệu bình dị, mộc mạc
                - Phản ánh niềm vui xuân mới và sự gắn kết cộng đồng""";
        };
    }

    private String postProcessAILyrics(String rawLyrics) {
        if (rawLyrics == null) return "";

        // Clean up the response
        String processed = rawLyrics.trim();

        // Remove any markdown formatting
        processed = processed.replaceAll("```[a-zA-Z]*", "");
        processed = processed.replaceAll("```", "");

        // Remove common AI response prefixes
        processed = processed.replaceAll("^(Đây là|Bài hát|Lời bài hát):?\\s*", "");

        // Ensure proper line breaks for verses
        processed = processed.replaceAll("\\n\\n+", "\n\n");

        // Remove excessive whitespace
        processed = processed.replaceAll("[ \\t]+", " ");

        return processed.trim();
    }
}
