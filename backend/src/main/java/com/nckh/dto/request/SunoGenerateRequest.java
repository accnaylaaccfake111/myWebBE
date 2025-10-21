package com.nckh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SunoGenerateRequest {
    private String prompt;
    private String style;
    private String title;
    private boolean customMode;
    private boolean instrumental;
    private String model;
    private String negativeTags;
    private String vocalGender;
    private float styleWeight;
    private float weirdnessConstraint;
    private float audioWeight;
    private String callBackUrl;
}
