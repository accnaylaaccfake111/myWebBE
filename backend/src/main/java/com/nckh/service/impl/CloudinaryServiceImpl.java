package com.nckh.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nckh.dto.response.FileUploadResponse;
import com.nckh.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {
    private final Cloudinary cloudinary;

    @Override
    public FileUploadResponse uploadImage(MultipartFile file) throws IOException {
        return uploadFile(file.getInputStream(), "image", "images", true);
    }

    @Override
    public FileUploadResponse uploadImage(byte[] fileBytes) throws IOException {
        validateBytes(fileBytes);
        return uploadFile(fileBytes, "image", "images", true);
    }

    @Override
    public FileUploadResponse uploadAudio(MultipartFile file) throws IOException {
        return uploadFile(file.getInputStream(), "video", "audios", false);
    }

    @Override
    public FileUploadResponse uploadAudio(byte[] fileBytes) throws IOException {
        validateBytes(fileBytes);
        return uploadFile(fileBytes, "video", "audios", false);
    }

    @Override
    public FileUploadResponse uploadVideo(MultipartFile file) throws IOException {
        return uploadFile(file.getInputStream(), "video", "videos", true);
    }

    @Override
    public FileUploadResponse uploadVideo(byte[] fileBytes) throws IOException {
        validateBytes(fileBytes);
        return uploadFile(fileBytes, "video", "videos", true);
    }

    private FileUploadResponse uploadFile(Object fileData, String resourceType,
                                          String folder, boolean optimize) throws IOException {
        Map<String, Object> params = ObjectUtils.asMap(
                "resource_type", resourceType,
                "folder", folder
        );

        if (optimize) {
            params.put("quality", "auto:good");
            params.put("fetch_format", "auto");
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(fileData, params);
        return mapToResponse(uploadResult);
    }

    @Override
    public void deleteFile(String publicId, String resourceType) throws IOException {
        Map<?, ?> deleteResult = cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("resource_type", resourceType));

        if (!"ok".equals(String.valueOf(deleteResult.get("result")))) {
            log.error("Failed to delete file. PublicId: {}, Result: {}", publicId, deleteResult);
            throw new IOException("Failed to delete file with publicId: " + publicId);
        }
    }

    private void validateBytes(byte[] fileBytes) throws IOException {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IOException("File bytes is empty or null");
        }
    }

    private FileUploadResponse mapToResponse(Map<?, ?> uploadResult) {
        Object secureUrl = uploadResult.get("secure_url");
        Object publicId = uploadResult.get("public_id");

        if (secureUrl == null || publicId == null) {
            log.error("Missing required fields in upload result: {}", uploadResult);
            throw new RuntimeException("Upload failed: missing secure_url or public_id");
        }

        return new FileUploadResponse(secureUrl.toString(), publicId.toString());
    }
}