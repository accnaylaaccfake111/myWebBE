package com.nckh.repository;

import com.nckh.entity.OutfitMerge;
import com.nckh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutfitMergeRepository extends JpaRepository<OutfitMerge, Long> {
    List<OutfitMerge> findAllByUser(User user);
}
