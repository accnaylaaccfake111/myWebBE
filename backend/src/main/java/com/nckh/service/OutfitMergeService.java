package com.nckh.service;


import com.nckh.dto.response.OutfitMergeDetailResponse;
import com.nckh.dto.response.OutfitMergeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OutfitMergeService {
    OutfitMergeResponse merge(String username, MultipartFile garmentImage, MultipartFile modelImage);

    OutfitMergeDetailResponse getById(Long id);

    List<OutfitMergeDetailResponse> getAlls(String username);

    void delete(Long id);
}
