package com.dayve22.Chronos.repository;

import com.dayve22.Chronos.entity.JobExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobLogRepository extends JpaRepository<JobExecutionLog, Long> {

    Page<JobExecutionLog> findByJobIdOrderByStartTimeDesc(String jobId, Pageable pageable);

    Optional<JobExecutionLog> findTopByJobIdAndStatusOrderByStartTimeDesc(String jobId, String status);

    List<JobExecutionLog> findByJobIdAndStartTimeAfterOrderByStartTimeDesc(String jobId, LocalDateTime after);

    Long countByJobIdAndStatus(String jobId, String status);

    List<JobExecutionLog> findTop10ByOrderByStartTimeDesc();
}