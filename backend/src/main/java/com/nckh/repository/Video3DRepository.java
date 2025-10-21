package com.nckh.repository;

import com.nckh.dto.response.Video3DResponse;
import com.nckh.entity.User;
import com.nckh.entity.Video3D;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Video3DRepository extends JpaRepository<Video3D, Long> {
    List<Video3D> findAllByUser(User user);
}
