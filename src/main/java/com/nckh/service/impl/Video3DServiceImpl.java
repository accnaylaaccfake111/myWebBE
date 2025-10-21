package com.nckh.service.impl;


import com.nckh.dto.response.Video3DResponse;
import com.nckh.entity.User;
import com.nckh.entity.Video3D;
import com.nckh.repository.UserRepository;
import com.nckh.repository.Video3DRepository;
import com.nckh.service.Video3DService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class Video3DServiceImpl implements Video3DService {
    private final Video3DRepository video3DRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Video3DResponse> getAll(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return video3DRepository.findAllByUser(user).stream()
                .map(this::mapToVideo3DResponse)
                .toList();
    }

    @Override
    public void delete(Long id) {
        Video3D v3d = video3DRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video 3D not found"));

        video3DRepository.delete(v3d);
    }

    @Transactional
    protected Video3DResponse mapToVideo3DResponse(Video3D v3d){
        return Video3DResponse.builder()
                .id(v3d.getId())
                .title(v3d.getTitle())
                .videoUrl(v3d.getVideoUrl())
                .resultUrl(v3d.getResultUrl())
                .type(v3d.getProcessType().name())
                .updateAt(v3d.getUpdatedAt())
                .createAt(v3d.getUpdatedAt())
                .build();
    }
}
