package com.neuralhealer.backend.integration.gmail;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Event-driven listener for PostgreSQL NOTIFY signals on the 'email_queue'
 * channel.
 * This wakes up the EmailQueueProcessor immediately when a new job is created.
 */
@Component
@Slf4j
public class PostgresNotificationListener {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EmailQueueProcessor emailQueueProcessor;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        log.info("🚀 Initializing PostgreSQL NOTIFY listener...");
        executorService.submit(this::listenToQueue);
    }

    private void listenToQueue() {
        while (running) {
            try (Connection connection = dataSource.getConnection()) {
                // Ensure we have a physical connection to PostgreSQL
                PGConnection pgConnection = connection.unwrap(PGConnection.class);

                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("LISTEN email_queue");
                }

                log.info("📡 PostgreSQL listener connected to 'email_queue' channel");

                // STARTUP CATCH-UP: Process any jobs that were pending while we were
                // disconnected/restarting
                // This ensures we don't need periodic polling for backlog
                log.info("📥 Processing backlog of pending emails...");
                emailQueueProcessor.processPendingEmails();

                while (running) {
                    // Check for notifications
                    // We use a timeout to check the 'running' flag periodically
                    PGNotification[] notifications = pgConnection.getNotifications(5000);

                    if (notifications != null) {
                        for (PGNotification notification : notifications) {
                            log.debug("🔔 Received NOTIFY: channel={}, parameter={}",
                                    notification.getName(), notification.getParameter());

                            // Wake up the processor to handle all pending jobs
                            // (We don't strictly need the job ID if we process all pending in one go,
                            // but it's good for logging/tracing)
                            emailQueueProcessor.processPendingEmails();
                        }
                    }

                    // Small sleep to prevent tight loop if getNotifications returns instantly
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                if (running) {
                    log.error("❌ PostgreSQL listener connection error: {}. Retrying in 10s...", e.getMessage());
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    @PreDestroy
    public void stop() {
        log.info("🛑 Stopping PostgreSQL NOTIFY listener...");
        running = false;
        executorService.shutdownNow();
    }
}
