package com.dayve22.Chronos.repository;

import com.dayve22.Chronos.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {
    List<ExecutionLog> findByJobGroupAndStartTimeBetween(String jobGroup, LocalDateTime start, LocalDateTime end);

    List<ExecutionLog> findByJobGroup(String jobGroup);
}