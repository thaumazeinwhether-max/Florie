package com.florie.repository;

import com.florie.entity.CareTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import com.florie.entity.FlowerStatus;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CareTaskRepository extends JpaRepository<CareTask, Long> {

    List<CareTask> findByFlowerFlowerId(Long flowerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from CareTask task where task.careTaskId = :taskId and task.flower.user.userId = :userId")
    Optional<CareTask> findOwnedTaskForUpdate(@Param("taskId") Long taskId, @Param("userId") Long userId);

    @Query("""
            select task from CareTask task
            where task.flower.user.userId = :userId
              and task.flower.status = :status
              and task.nextCareDate <= :date
            order by task.nextCareDate asc, task.careTaskId asc
            """)
    List<CareTask> findDueTasks(@Param("userId") Long userId,
                                @Param("status") FlowerStatus status,
                                @Param("date") LocalDate date);
}
