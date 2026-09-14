package com.florie.repository;

import com.florie.entity.Flower;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import com.florie.entity.FlowerStatus;

public interface FlowerRepository extends JpaRepository<Flower, Long> {

    Optional<Flower> findByUserUserIdAndStatus(Long userId, FlowerStatus status);

    boolean existsByUserUserIdAndStatus(Long userId, FlowerStatus status);

    List<Flower> findByUserUserIdAndStatusOrderByEndedOnDesc(Long userId, FlowerStatus status);

    Optional<Flower> findByFlowerIdAndUserUserId(Long flowerId, Long userId);
}

