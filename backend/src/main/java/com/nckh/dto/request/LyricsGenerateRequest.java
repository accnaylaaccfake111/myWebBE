package com.nckh.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LyricsGenerateRequest {
    
    @NotBlank(message = "Theme is required")
    private String theme;

    private String title;
    
    private String note;
    
    private String mood;
    
    private Integer minLines;
    
    private Integer maxLines;
    
    private String language; // vi, en
    
    private Boolean useAI; // true: use AI, false: use templates
}