package com.nckh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetGanerateResponse {
    private Long sheetMusicId;
    private String status;
    private String message;
    private String errorMessage;
}
