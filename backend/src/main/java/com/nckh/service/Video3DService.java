package com.nckh.service;

import com.nckh.dto.response.Video3DResponse;

import java.util.List;

public interface Video3DService {

    List<Video3DResponse> getAll(String username);

    void delete(Long id);
}
