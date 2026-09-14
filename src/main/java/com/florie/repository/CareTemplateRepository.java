package com.florie.repository;

import com.florie.entity.CareTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareTemplateRepository extends JpaRepository<CareTemplate, Long> {

    List<CareTemplate> findByFlowerTypeFlowerTypeIdOrderByDisplayOrderAsc(Long flowerTypeId);
}

