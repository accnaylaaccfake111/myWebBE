package com.nckh.service;

import com.nckh.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CloudinaryService {
    FileUploadResponse uploadImage(MultipartFile file) throws IOException;
    FileUploadResponse uploadImage(byte[] fileData) throws IOException;
    FileUploadResponse uploadAudio(MultipartFile file) throws IOException;
    FileUploadResponse uploadAudio(byte[] fileData) throws IOException;
    FileUploadResponse uploadVideo(MultipartFile file) throws IOException;
    FileUploadResponse uploadVideo(byte[] fileData) throws IOException;
    void deleteFile(String publicId, String resourceType) throws IOException;
}
