package com.nckh.repository;

import com.nckh.entity.FaceSwap;
import com.nckh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaceSwapRepository extends JpaRepository<FaceSwap, Long> {
    List<FaceSwap> findAllByUser(User user);
}
