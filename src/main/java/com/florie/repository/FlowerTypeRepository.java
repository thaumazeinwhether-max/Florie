package com.florie.repository;

import com.florie.entity.FlowerType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlowerTypeRepository extends JpaRepository<FlowerType, Long> {

    List<FlowerType> findAllByOrderByDisplayOrderAsc();
}

