package com.nckh.service;

import org.springframework.web.multipart.MultipartFile;

public interface MergeService {
    byte[] merge(MultipartFile videoFile, MultipartFile lyricFile, MultipartFile musicFile);
}
