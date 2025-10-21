package com.nckh.service;

import java.io.File;
import java.io.InputStream;

public interface MinioService {
    String uploadFile(File file, String fileName, String bucketName);
    String uploadFile(File file, String fileName, String bucketName, int expiryHours);
    String uploadFile(InputStream inputStream, long size, String fileName,
                      String bucketName, String contentType);
}
