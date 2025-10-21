package com.nckh.entity;

import com.nckh.util.JsonConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "video3d")
public class Video3D extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "title")
    private String title;

    @Column(columnDefinition = "json")
    @Convert(converter = JsonConverter.class)
    private Map<String, Object> json;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "result_url")
    private String resultUrl;

    @Enumerated(EnumType.STRING)
    private ProcessType processType;

    public enum ProcessType{
        COMPARE_VIDEOS,
        PROCESS_VIDEO_STREAM,
    }
}


