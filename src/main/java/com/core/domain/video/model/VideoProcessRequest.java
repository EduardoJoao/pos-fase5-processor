package com.core.domain.video.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoProcessRequest {
    private String s3Key;
    private String videoId;
    private String clientId;
    private String filename;
    private String contentType;
    private Long timestamp;
}