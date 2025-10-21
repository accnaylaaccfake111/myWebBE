package com.nckh.repository;

import com.nckh.entity.UserPerformance;
import com.nckh.entity.User;
import com.nckh.entity.KaraokeTrack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPerformanceRepository extends JpaRepository<UserPerformance, Long> {
    
    List<UserPerformance> findByUserOrderByCreatedAtDesc(User user);
    
    List<UserPerformance> findByKaraokeTrackOrderByOverallScoreDesc(KaraokeTrack karaokeTrack);
    
    Optional<UserPerformance> findByIdAndUser(Long id, User user);
    
    @Query("SELECT up FROM UserPerformance up WHERE up.isPublicScore = true AND up.status = 'COMPLETED' ORDER BY up.overallScore DESC")
    List<UserPerformance> findTopPerformances(Pageable pageable);
    
    @Query("SELECT AVG(up.overallScore) FROM UserPerformance up WHERE up.user = :user AND up.status = 'COMPLETED'")
    Optional<Double> findAverageScoreByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(up) FROM UserPerformance up WHERE up.user = :user AND up.status = 'COMPLETED'")
    long countCompletedPerformancesByUser(@Param("user") User user);
    
    @Query("SELECT up FROM UserPerformance up WHERE up.karaokeTrack = :track AND up.status = 'COMPLETED' ORDER BY up.overallScore DESC")
    List<UserPerformance> findBestPerformancesForTrack(@Param("track") KaraokeTrack track, Pageable pageable);
    
    Page<UserPerformance> findByUserAndStatus(User user, UserPerformance.PerformanceStatus status, Pageable pageable);
    
    @Query("SELECT up FROM UserPerformance up WHERE up.user = :user AND up.karaokeTrack = :track ORDER BY up.createdAt DESC")
    List<UserPerformance> findUserPerformancesForTrack(@Param("user") User user, @Param("track") KaraokeTrack track);
}