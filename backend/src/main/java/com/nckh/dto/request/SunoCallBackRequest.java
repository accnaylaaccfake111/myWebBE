package com.nckh.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SunoCallBackRequest {
    private int code;
    private String msg;
    private SunoCallBackData data;
}
