package com.neuralhealer.backend.repository;

import com.neuralhealer.backend.model.entity.MessageQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
        @Query(value = "SELECT * FROM message_queues WHERE job_type = :jobType AND CAST(status AS TEXT) = :status ORDER BY CASE priority WHEN 'critical' THEN 1 WHEN 'high' THEN 2 WHEN 'normal' THEN 3 WHEN 'low' THEN 4 ELSE 5 END ASC, created_at ASC", countQuery = "SELECT count(*) FROM message_queues WHERE job_type = :jobType AND CAST(status AS TEXT) = :status", nativeQuery = true)
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
        @Query(value = "SELECT * FROM message_queues WHERE job_type = :jobType AND CAST(status AS TEXT) = 'failed' AND retry_count < :maxRetries ORDER BY CASE priority WHEN 'critical' THEN 1 WHEN 'high' THEN 2 WHEN 'normal' THEN 3 WHEN 'low' THEN 4 ELSE 5 END ASC, created_at ASC", countQuery = "SELECT count(*) FROM message_queues WHERE job_type = :jobType AND CAST(status AS TEXT) = 'failed' AND retry_count < :maxRetries", nativeQuery = true)
        List<MessageQueue> findFailedJobsForRetry(@Param("jobType") String jobType, @Param("maxRetries") int maxRetries,
                        Pageable pageable);

        /**
         * Atomically update job status only if current status matches expected value.
         * This prevents race conditions where multiple threads try to process the same
         * job.
         * 
         * @param id            The job ID
         * @param newStatus     The new status to set
         * @param currentStatus The expected current status
         * @return Number of rows updated (1 if successful, 0 if already changed)
         */
        @Query(value = "UPDATE message_queues SET status = CAST(:newStatus AS job_status) WHERE id = :id AND CAST(status AS TEXT) = :currentStatus", nativeQuery = true)
        @Modifying
        int updateStatusIfPending(@Param("id") UUID id, @Param("newStatus") String newStatus,
                        @Param("currentStatus") String currentStatus);

        /**
         * Mark job as completed using native SQL to avoid Enum mapping issues.
         */
        @Query(value = "UPDATE message_queues SET status = 'completed', processed_at = NOW(), error_message = NULL WHERE id = :id", nativeQuery = true)
        @Modifying
        void markAsCompleted(@Param("id") UUID id);

        /**
         * Mark job as failed or update for retry using native SQL.
         */
        @Query(value = "UPDATE message_queues SET status = CAST(:status AS job_status), retry_count = :retryCount, error_message = :error, processed_at = CASE WHEN :status = 'failed' THEN NOW() ELSE processed_at END WHERE id = :id", nativeQuery = true)
        @org.springframework.data.jpa.repository.Modifying
        void updateStatusAndError(@Param("id") UUID id, @Param("status") String status,
                        @Param("retryCount") int retryCount, @Param("error") String error);
}
