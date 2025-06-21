package gtp.util;

import gtp.core.TaskDispatcher;
import gtp.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Handles graceful shutdown of the task processing system.
 * <p>
 * This runnable implementation is designed to be executed during JVM shutdown
 * to ensure all pending tasks in the queue are processed before termination.
 * </p>
 *
 * <p>Shutdown sequence:</p>
 * <ol>
 *   <li>Drains the task queue, processing remaining tasks</li>
 *   <li>Handles interrupts during shutdown process</li>
 *   <li>Initiates dispatcher shutdown</li>
 *   <li>Logs completion status</li>
 * </ol>
 *
 * @see Runnable
 * @see TaskDispatcher
 */
public class GracefulShutdown implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdown.class);
    private final TaskDispatcher dispatcher;

    /**
     * Constructs a GracefulShutdown handler for the specified dispatcher.
     *
     * @param dispatcher the TaskDispatcher to shut down gracefully
     */
    public GracefulShutdown(TaskDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Executes the graceful shutdown sequence.
     * <p>
     * Processes all remaining tasks in the queue with a 1-second timeout per task,
     * then shuts down the dispatcher. Responds to interrupts during shutdown.
     * </p>
     */
    @Override
    public void run() {
        logger.info("\n\nShutdown initiated, draining queue...");
        BlockingQueue<Task> queue = dispatcher.getTaskQueue();

        int processedTasks = 0;
        while (!queue.isEmpty()) {
            try {
                Task task = queue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    logger.info("Processing remaining task: {}", task);
                    processedTasks++;
                }
            } catch (InterruptedException e) {
                logger.warn("Shutdown process interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }

        dispatcher.shutdown();
        logger.info("System shutdown complete. Processed {} remaining tasks", processedTasks);
    }
}