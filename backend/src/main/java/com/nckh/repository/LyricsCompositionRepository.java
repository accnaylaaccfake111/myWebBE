package com.nckh.repository;

import com.nckh.entity.LyricsComposition;
import com.nckh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LyricsCompositionRepository extends JpaRepository<LyricsComposition, Long> {
    List<LyricsComposition> findTop10ByUserOrderByCreatedAtDesc(User user);
}