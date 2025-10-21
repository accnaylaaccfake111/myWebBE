package com.nckh.repository;

import com.nckh.dto.response.SheetMusicResponse;
import com.nckh.entity.SheetMusic;
import com.nckh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SheetMusicRepository extends JpaRepository<SheetMusic, Long> {
    Optional<SheetMusic> findByTaskId(String id);

    List<SheetMusic> findAllByUser(User user);
}
