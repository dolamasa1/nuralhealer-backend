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
     * Used by EmailQueueProcessor to fetch pending email notifications.
     *
     * @param jobType  The job type to filter by (e.g., "EMAIL_NOTIFICATION")
     * @param status   The job status to filter by (e.g., "pending")
     * @param pageable Pagination parameters
     * @return List of matching message queue jobs
     */
    @Query("SELECT mq FROM MessageQueue mq WHERE mq.jobType = :jobType AND mq.status = :status ORDER BY mq.createdAt ASC")
    List<MessageQueue> findByJobTypeAndStatus(@Param("jobType") String jobType, @Param("status") String status,
            Pageable pageable);

    /**
     * Find failed jobs that can be retried.
     *
     * @param jobType    The job type to filter by
     * @param maxRetries Maximum number of retries allowed
     * @param pageable   Pagination parameters
     * @return List of failed jobs with retry count below max
     */
    @Query("SELECT mq FROM MessageQueue mq WHERE mq.jobType = :jobType AND mq.status = 'failed' AND mq.retryCount < :maxRetries ORDER BY mq.createdAt ASC")
    List<MessageQueue> findFailedJobsForRetry(@Param("jobType") String jobType, @Param("maxRetries") int maxRetries,
            Pageable pageable);
}
