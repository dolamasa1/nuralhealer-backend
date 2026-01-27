package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.MessageQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageQueueRepository extends JpaRepository<MessageQueue, UUID> {

    /**
     * Find message queue jobs by job type and status.
     * Uses native query to handle Postgres custom enum type (job_status) casting.
     *
     * @param jobType  The job type to filter by (e.g., "EMAIL_NOTIFICATION")
     * @param status   The job status to filter by (e.g., "pending")
     * @param pageable Pagination parameters
     * @return List of matching message queue jobs
     */
    @Query(value = "SELECT * FROM message_queues WHERE job_type = :jobType AND CAST(status AS TEXT) = :status ORDER BY created_at ASC", countQuery = "SELECT count(*) FROM message_queues WHERE job_type = :jobType AND CAST(status AS TEXT) = :status", nativeQuery = true)
    List<MessageQueue> findByJobTypeAndStatus(@Param("jobType") String jobType, @Param("status") String status,
            Pageable pageable);

    /**
     * Find failed jobs that can be retried.
     * Uses native query to handle Postgres custom enum type (job_status) casting.
     *
     * @param jobType    The job type to filter by
     * @param maxRetries Maximum number of retries allowed
     * @param pageable   Pagination parameters
     * @return List of failed jobs with retry count below max
     */
    @Query(value = "SELECT * FROM message_queues WHERE job_type = :jobType AND CAST(status AS TEXT) = 'failed' AND retry_count < :maxRetries ORDER BY created_at ASC", countQuery = "SELECT count(*) FROM message_queues WHERE job_type = :jobType AND CAST(status AS TEXT) = 'failed' AND retry_count < :maxRetries", nativeQuery = true)
    List<MessageQueue> findFailedJobsForRetry(@Param("jobType") String jobType, @Param("maxRetries") int maxRetries,
            Pageable pageable);
}
