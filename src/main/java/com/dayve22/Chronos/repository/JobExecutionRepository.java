package com.dayve22.Chronos.repository;

import com.dayve22.Chronos.entity.ExecutionStatus;
import com.dayve22.Chronos.entity.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {

    List<JobExecution> findByJobIdOrderByStartTimeDesc(Long jobId);

    @Query("SELECT e FROM JobExecution e WHERE e.job.id = :jobId " +
            "ORDER BY e.startTime DESC")
    List<JobExecution> findRecentExecutions(
            @Param("jobId") Long jobId,
            org.springframework.data.domain.Pageable pageable
    );

    default List<JobExecution> findTop10ByJobId(Long jobId) {
        return findRecentExecutions(jobId,
                org.springframework.data.domain.PageRequest.of(0, 10));
    }

    @Query("SELECT COUNT(e) FROM JobExecution e WHERE e.job.id = :jobId " +
            "AND e.status = :status AND e.startTime >= :since")
    long countByJobIdAndStatusSince(
            @Param("jobId") Long jobId,
            @Param("status") ExecutionStatus status,
            @Param("since") LocalDateTime since
    );
}