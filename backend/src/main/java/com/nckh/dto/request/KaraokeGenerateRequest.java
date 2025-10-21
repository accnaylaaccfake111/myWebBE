package com.nckh.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KaraokeGenerateRequest {
    
    @NotNull(message = "Sheet Music ID is required")
    private Long sheetMusicId;
    
    private String generationMethod = "AI"; // AI, TEMPLATE, MANUAL
    
    private Integer targetBpm;
    
    private String targetKeySignature;
    
    private String targetTimeSignature = "4/4";
    
    private String musicStyle; // ballad, pop, rock, folk, etc.
    
    private String instrumentalType; // acoustic, electronic, orchestral
    
    private Boolean includeVocalGuide = false;
    
    private Boolean isPublic = false;
}