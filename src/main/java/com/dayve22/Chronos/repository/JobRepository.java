package com.dayve22.Chronos.repository;

import com.dayve22.Chronos.entity.ExecutionStatus;
import com.dayve22.Chronos.entity.Job;
import com.dayve22.Chronos.entity.JobExecution;
import com.dayve22.Chronos.entity.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // Updated query - removed the >= :now condition to catch past-due jobs
    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE' " +
            "AND j.nextRunTime <= :lookAhead " +
            "ORDER BY j.nextRunTime ASC")
    List<Job> findJobsToRun(
            @Param("lookAhead") LocalDateTime lookAhead,
            org.springframework.data.domain.Pageable pageable
    );

    default List<Job> findJobsToRun(LocalDateTime now, LocalDateTime lookAhead, int limit) {
        return findJobsToRun(lookAhead,
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    Job findByIdWithLock(@Param("id") Long id);

    List<Job> findByUserIdAndStatus(Long userId, JobStatus status);

    List<Job> findByUserId(Long userId);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.user.id = :userId AND j.status = 'ACTIVE'")
    long countActiveJobsByUserId(@Param("userId") Long userId);
}