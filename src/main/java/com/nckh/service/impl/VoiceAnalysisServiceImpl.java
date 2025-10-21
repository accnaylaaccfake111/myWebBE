package com.nckh.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nckh.dto.response.VoiceAnalysResponse;
import com.nckh.httpclient.OpenAIClient;
import com.nckh.service.VoiceAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceAnalysisServiceImpl implements VoiceAnalysisService {
    private final OpenAIClient openAIClient;
    private final ChatModel chatModel;
    private final GoogleSheetService googleSheetService;
    @Value("${ai-service.openai.model-type}")
    private String modelType;
    @Value("${ai-service.openai.api-key}")
    private String authHeader;

    @Override
    public VoiceAnalysResponse analyst(MultipartFile voiceFile, String lyricId) {
        try {
            ResponseEntity<JsonNode> response = openAIClient.transcrip("Bearer " + authHeader, modelType, voiceFile);
            String textFromVoice;
            if(response.getStatusCode().is2xxSuccessful()){
                textFromVoice = response.getBody().get("text").asText();
                log.info("SPEECH-TO-TEXT: {}", textFromVoice);

                return analyzeWithAI(textFromVoice, lyricId);
            }
            return VoiceAnalysResponse.builder()
                    .build();
        }catch (Exception ex){
            throw new RuntimeException("Cannot processing json" + ex.getMessage());
        }
    }

    @Override
    public VoiceAnalysResponse analyzeWithAI(String voice, String lyrics) throws JsonProcessingException {
        String prompt = buildPromScore(voice, lyrics);

        Prompt aiPrompt = new Prompt(prompt);
        ChatResponse response = chatModel.call(aiPrompt);
        return extractResponse(response);
    }

    private String buildPromScore(String voice, String lyrics) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Bạn là chuyên gia phân tích giọng hát karaoke.\n");
        prompt.append("Nhiệm vụ của bạn là chấm điểm phần thể hiện của người hát dựa trên văn bản họ hát (chuyển từ giọng nói thành text) và lời gốc.\n\n");

        prompt.append("Đoạn người dùng đã hát:\n");
        prompt.append("{{").append(voice).append("}}\n\n");

        prompt.append("Lyrics gốc của bài hát:\n");
        prompt.append("{{").append(lyrics).append("}}\n\n");

        prompt.append("Hãy đánh giá theo các tiêu chí sau (thang điểm 0–100):\n");
        prompt.append("- Độ khớp lời (hát đúng từ, đúng thứ tự)\n");
        prompt.append("- Phát âm (rõ ràng, đúng âm) (tối đa 90)\n");
        prompt.append("- Ngữ điệu / nhịp đọc (tối đa 85)\n");
        prompt.append("- Lưu loát (tối đa 85)\n");
        prompt.append("- Tổng thể (tối đa 85)\n\n");

        prompt.append("Chỉ trả về JSON đúng định dạng sau (không markdown, không thêm văn bản giải thích):\n");
        prompt.append("""
        {
          "scores": {
            "lyricMatch": 0,
            "pronunciation": 0,
            "intonation": 0,
            "fluency": 0,
            "overall": 0
          },
          "comments": {
            "lyricMatch": "",
            "pronunciation": "",
            "intonation": "",
            "fluency": "",
            "overall": ""
          }
        }
        """);

        return prompt.toString();
    }

    private VoiceAnalysResponse extractResponse(ChatResponse response) throws JsonProcessingException {
        if (response == null || response.getResult() == null ||
                response.getResult().getOutput() == null) {
            throw new RuntimeException("Invalid response from AI model");
        }

        String raw = response.getResult().getOutput().getText();
        String responseJson = raw
                .replaceAll("(?s)```json", "") // bỏ đầu ```json
                .replaceAll("(?s)```", "")     // bỏ cuối ```
                .trim();
        if (responseJson == null || responseJson.trim().isEmpty()) {
            throw new RuntimeException("AI model returned empty lyrics");
        }

        log.info("COMMENT: {}", responseJson);

        ObjectMapper mapper = new ObjectMapper();
        VoiceAnalysResponse tmp = mapper.readValue(responseJson, VoiceAnalysResponse.class);
        try {
            List<Object> scores = List.of(tmp.getScores().getLyricMatch(), tmp.getScores().getPronunciation(), tmp.getScores().getIntonation(), tmp.getScores().getFluency(), tmp.getScores().getOverall());
            googleSheetService.updateRange(scores);
        }catch (IOException e){
            log.info("Cannot write data into sheet ");
        }
        return tmp;
    }
}