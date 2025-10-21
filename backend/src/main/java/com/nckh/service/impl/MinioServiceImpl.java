package com.nckh.service.impl;

import com.nckh.service.MinioService;
import io.micrometer.common.util.StringUtils;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {
    private final MinioClient minioClient;

    @Value("${minio.expiry.default.hours:24}")
    private int defaultExpiryHours;

    /**
     * Creates bucket if it doesn't exist
     * @param bucketName the name of the bucket
     * @throws RuntimeException if bucket creation fails
     */
    private void createBucketIfNotExists(String bucketName) {
        validateBucketName(bucketName);

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!exists) {
                log.info("Creating bucket: {}", bucketName);
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Bucket created successfully: {}", bucketName);
            }
        } catch (RuntimeException e) {
            log.error("MinIO error while creating bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to create bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error while creating bucket: {}", bucketName, e);
            throw new RuntimeException("Unexpected error creating bucket: " + bucketName, e);
        }
    }

    /**
     * Uploads a file to MinIO and returns a presigned URL
     * @param file the file to upload
     * @param fileName the name for the uploaded file
     * @param bucketName the target bucket name
     * @return presigned URL for accessing the uploaded file
     * @throws RuntimeException if upload fails
     */
    @Override
    public String uploadFile(File file, String fileName, String bucketName) {
        validateUploadParameters(file, fileName, bucketName);

        try {
            createBucketIfNotExists(bucketName);

            log.info("Uploading file {} to bucket {} as {}",
                    file.getName(), bucketName, fileName);

            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .filename(file.getAbsolutePath())
                            .build()
            );

            String presignedUrl = generatePresignedUrl(bucketName, fileName, defaultExpiryHours);
            log.info("File uploaded successfully: {}", fileName);

            return presignedUrl;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", fileName, e);
            throw new RuntimeException("Unexpected error uploading file: " + fileName, e);
        }
    }

    /**
     * Uploads a file with custom expiry time
     */
    @Override
    public String uploadFile(File file, String fileName, String bucketName, int expiryHours) {
        validateUploadParameters(file, fileName, bucketName);
        validateExpiryHours(expiryHours);

        try {
            createBucketIfNotExists(bucketName);

            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .filename(file.getAbsolutePath())
                            .build()
            );

            return generatePresignedUrl(bucketName, fileName, expiryHours);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading file with custom expiry: {}", fileName, e);
            throw new RuntimeException("Failed to upload file: " + fileName, e);
        }
    }

    /**
     * Upload file from InputStream
     */
    @Override
    public String uploadFile(InputStream inputStream, long size, String fileName,
                             String bucketName, String contentType) {
        validateStreamUploadParameters(inputStream, fileName, bucketName, contentType);

        try {
            createBucketIfNotExists(bucketName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            return generatePresignedUrl(bucketName, fileName, defaultExpiryHours);

        } catch (Exception e) {
            log.error("Error uploading file from stream: {}", fileName, e);
            throw new RuntimeException("Failed to upload file from stream: " + fileName, e);
        }
    }

    /**
     * Generates presigned URL for accessing object
     */
    private String generatePresignedUrl(String bucketName, String fileName, int expiryHours) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(expiryHours, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned URL for: {}/{}", bucketName, fileName, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Validates upload parameters
     */
    private void validateUploadParameters(File file, String fileName, String bucketName) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File must be a valid existing file");
        }

        if (file.length() == 0) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        validateBucketName(bucketName);
    }

    /**
     * Validates stream upload parameters
     */
    private void validateStreamUploadParameters(InputStream inputStream, String fileName,
                                                String bucketName, String contentType) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        if (StringUtils.isBlank(contentType)) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }

        validateBucketName(bucketName);
    }

    /**
     * Validates bucket name
     */
    private void validateBucketName(String bucketName) {
        if (StringUtils.isBlank(bucketName)) {
            throw new IllegalArgumentException("Bucket name cannot be null or empty");
        }

        // MinIO bucket naming rules validation
        if (bucketName.length() < 3 || bucketName.length() > 63) {
            throw new IllegalArgumentException("Bucket name must be between 3 and 63 characters");
        }

        if (!bucketName.matches("^[a-z0-9][a-z0-9.-]*[a-z0-9]$")) {
            throw new IllegalArgumentException("Invalid bucket name format");
        }
    }

    /**
     * Validates expiry hours
     */
    private void validateExpiryHours(int expiryHours) {
        if (expiryHours <= 0 || expiryHours > 168) { // Max 7 days
            throw new IllegalArgumentException("Expiry hours must be between 1 and 168 (7 days)");
        }
    }
}
