package com.nckh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "media_files", indexes = {
    @Index(name = "idx_media_user", columnList = "user_id"),
    @Index(name = "idx_media_project", columnList = "project_id"),
    @Index(name = "idx_media_type", columnList = "file_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile extends BaseEntity {
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "public_id", unique = true)
    private String publicId;
    
    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;
    
    @Column(name = "file_url", nullable = false, length = 1024)
    private String fileUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;
    
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_music_id")
    private SheetMusic sheetMusic;

    @OneToMany(mappedBy = "garmentImage")
    private List<OutfitMerge> usedAsGarmentImage;

    @OneToMany(mappedBy = "modelImage")
    private List<OutfitMerge> usedAsModelImage;

    @OneToMany(mappedBy = "resultImage")
    private List<OutfitMerge> usedAsResultImage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 20)
    private StorageType storageType;
    
    @Column(name = "bucket_name", length = 100)
    private String bucketName;
    
    @Column(name = "is_processed")
    private boolean processed = false;
    
    @Column(name = "is_public")
    private boolean isPublic = false;
    
    @Column(name = "download_count")
    private Long downloadCount = 0L;
    
    public enum FileType {
        IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER
    }
    
    public enum StorageType {
        LOCAL, MINIO, S3, AZURE, CLOUDINARY
    }
}