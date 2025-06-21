package gtp.core;

import gtp.config.AppConfig;
import gtp.util.GracefulShutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the concurrent task queue system.
 *
 * <p>This class initializes and manages the lifecycle of the task processing system,
 * which includes:</p>
 * <ul>
 *   <li>Loading application configuration from {@link AppConfig}</li>
 *   <li>Initializing and starting the {@link TaskDispatcher}</li>
 *   <li>Registering a {@link GracefulShutdown} hook for clean termination</li>
 *   <li>Maintaining the main thread until system shutdown</li>
 * </ul>
 *
 * <p>The system provides:</p>
 * <ul>
 *   <li>Thread-safe task processing with configurable worker pools</li>
 *   <li>Graceful shutdown handling for in-progress tasks</li>
 *   <li>Configuration through application.properties</li>
 * </ul>
 */
public class ConcurQueue {
    private static final Logger logger = LoggerFactory.getLogger(ConcurQueue.class);

    /**
     * Main entry point for the application.
     *
     * <p>Execution flow:</p>
     * <ol>
     *   <li>Initializes application configuration</li>
     *   <li>Creates and configures task dispatcher with loaded configuration</li>
     *   <li>Registers shutdown hook for graceful termination</li>
     *   <li>Starts the task processing system</li>
     *   <li>Maintains the main thread until shutdown signal is received</li>
     * </ol>
     *
     * @param args command line arguments (currently unused)
     * @throws IllegalStateException if system initialization fails
     *
     * @see AppConfig For configuration parameters
     * @see TaskDispatcher For task processing implementation
     * @see GracefulShutdown For shutdown behavior details
     */
    public static void main(String[] args) {
        try {
            logger.info("Initializing concurrent task queue system");

            AppConfig config = new AppConfig();
            TaskDispatcher dispatcher = new TaskDispatcher(config);

            Runtime.getRuntime().addShutdownHook(new Thread(
                    new GracefulShutdown(dispatcher),
                    "Shutdown-Hook"
            ));

            dispatcher.start();
            logger.info("Task dispatcher started successfully");

            // Keep the main thread alive until shutdown
            awaitShutdown();

        } catch (Exception e) {
            logger.error("System initialization failed", e);
            throw new IllegalStateException("Failed to initialize task queue system", e);
        }
    }

    /**
     * Blocks the main thread indefinitely until shutdown signal is received.
     * Uses object wait instead of sleep to be more responsive to interrupts.
     */
    private static void awaitShutdown() {
        final Object lock = new Object();
        synchronized (lock) {
            try {
                logger.debug("Main thread entering wait state");
                lock.wait();
            } catch (InterruptedException e) {
                logger.info("Main thread interrupted, initiating shutdown");
                Thread.currentThread().interrupt();
            }
        }
    }
}