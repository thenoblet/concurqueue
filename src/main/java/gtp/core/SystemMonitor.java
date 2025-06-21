package gtp.core;

import gtp.model.Task;
import gtp.model.TaskStatus;
import gtp.worker.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The SystemMonitor class is responsible for monitoring and reporting the status of the task processing system.
 */
public class SystemMonitor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SystemMonitor.class);
    private final BlockingQueue<Task> taskQueue;
    private final ExecutorService executorService;
    private final TaskStateTracker stateTracker;
    private volatile boolean running = true;

    public SystemMonitor(BlockingQueue<Task> taskQueue, ExecutorService executorService,
                         TaskStateTracker stateTracker) {
        this.taskQueue = taskQueue;
        this.executorService = executorService;
        this.stateTracker = stateTracker;
    }

    @Override
    public void run() {
        try {
            while (running) {
                logSystemStatus();
                TimeUnit.SECONDS.sleep(5);
            }
        } catch (InterruptedException e) {
            logger.warn("Monitor interrupted, shutting down");
            Thread.currentThread().interrupt();
        }
    }

    private void logSystemStatus() {
        ConcurrentHashMap<UUID, TaskStatus> states = stateTracker.getTaskStates();

        logger.info("\n\n=== System Status ===");
        logger.info("Queue size: {}", taskQueue.size());

        long submitted = states.values().stream()
                .filter(s -> s == TaskStatus.SUBMITTED).count();
        long processing = states.values().stream()
                .filter(s -> s == TaskStatus.PROCESSING).count();
        long completed = states.values().stream()
                .filter(s -> s == TaskStatus.COMPLETED).count();
        long failed = states.values().stream()
                .filter(s -> s == TaskStatus.FAILED).count();

        logger.info("Tasks: {} submitted, {} processing, {} completed, {} failed",
                submitted, processing, completed, failed);

        logger.info("\n\n--- Synchronization Demo ---");
        logger.info("Counters: unsafe={} | safe={} (lost updates: {})",
                String.format("%,d", TaskWorker.getUnsafeCounter()),
                String.format("%,d", TaskWorker.getSafeCounter()),
                String.format("%,d", TaskWorker.getSafeCounter() - TaskWorker.getUnsafeCounter()));

        if (TaskWorker.getUnsafeCounter() == TaskWorker.getSafeCounter()) {
            logger.warn("No race conditions detected - try:");
            logger.warn("1. Increase worker threads (> CPU cores)");
            logger.warn("2. Add more contention in demonstrateRaceCondition()");
        }

        logger.info("Thread pool: {}\n", executorService.toString());
    }

    public void stop() {
        running = false;
        logger.debug("Monitor shutdown requested");
    }
}