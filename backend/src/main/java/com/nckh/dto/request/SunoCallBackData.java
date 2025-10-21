package com.nckh.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SunoCallBackData {
    private String callbackType;
    @JsonProperty("task_id")
    private String taskId;
    private List<SunoMusicData> data;
}
