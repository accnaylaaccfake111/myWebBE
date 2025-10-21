package com.nckh.repository;

import com.nckh.entity.MusicGenTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MusicGenTaskRepository extends JpaRepository<MusicGenTask, Long> {
    Optional<MusicGenTask> findByTaskId(String taskId);
    Optional<MusicGenTask> findByExternalTaskId(String externalTaskId);
}
