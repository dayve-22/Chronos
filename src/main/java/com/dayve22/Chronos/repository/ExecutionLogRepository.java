package com.dayve22.Chronos.repository;

import com.dayve22.Chronos.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {

    List<ExecutionLog> findByJobName(String jobName);
}