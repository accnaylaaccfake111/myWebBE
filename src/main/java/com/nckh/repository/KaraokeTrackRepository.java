package com.nckh.repository;

import com.nckh.entity.KaraokeTrack;
import com.nckh.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KaraokeTrackRepository extends JpaRepository<KaraokeTrack, Long> {
    
    List<KaraokeTrack> findByCreatorOrderByCreatedAtDesc(User user);

    @Query("SELECT k FROM KaraokeTrack k WHERE k.isPublic = true AND k.status = 'ACTIVE' ORDER BY k.averageScore DESC, k.playCount DESC")
    List<KaraokeTrack> findTopRatedPublicTracks(Pageable pageable);
}