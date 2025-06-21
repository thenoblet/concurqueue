package gtp.core;

import gtp.config.AppConfig;
import gtp.model.Task;
import gtp.producer.TaskProducer;
import gtp.util.JsonExporter;
import gtp.worker.TaskWorker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Central coordinator for the task processing system.
 * <p>
 * Manages the complete lifecycle of task processing including:
 * </p>
 * <ul>
 *   <li>Producer thread pool creation and management</li>
 *   <li>Worker thread pool creation and management</li>
 *   <li>Priority-based task queue management</li>
 *   <li>System monitoring initialization</li>
 *   <li>State tracking and JSON export setup</li>
 *   <li>Graceful shutdown coordination</li>
 * </ul>
 *
 * <p>Configuration is driven by {@link AppConfig} for all operational parameters.</p>
 */
public class TaskDispatcher {
    private final BlockingQueue<Task> taskQueue;
    private final TaskStateTracker stateTracker;
    private final ExecutorService producerExecutor;
    private final ExecutorService workerExecutor;
    private final SystemMonitor monitor;
    private final JsonExporter jsonExporter;
    private final int producerCount;
    private final int workerCount;
    private final int maxRetries;

    /**
     * Constructs a new TaskDispatcher with the specified configuration.
     *
     * @param config the application configuration containing:
     *               - producerCount: number of producer threads
     *               - workerCount: number of worker threads
     *               - maxRetries: maximum task retry attempts
     *               - queueCapacity: task queue size
     */
    public TaskDispatcher(AppConfig config) {
        this.producerCount = config.getProducerCount();
        this.workerCount = config.getWorkerCount();
        this.maxRetries = config.getMaxRetries();
        this.taskQueue = new PriorityBlockingQueue<>(config.getQueueCapacity());
        this.stateTracker = new TaskStateTracker();
        this.producerExecutor = Executors.newFixedThreadPool(producerCount);
        this.workerExecutor = Executors.newFixedThreadPool(workerCount);
        this.jsonExporter = new JsonExporter(stateTracker, config);
        this.monitor = new SystemMonitor(taskQueue, workerExecutor, stateTracker);
    }

    /**
     * Starts the task processing system.
     * <p>
     * Initializes and starts:
     * </p>
     * <ol>
     *   <li>Configured number of producer threads</li>
     *   <li>Configured number of worker threads</li>
     *   <li>System monitor thread</li>
     *   <li>JSON exporter service</li>
     * </ol>
     */
    public void start() {
        for (int i = 0; i < producerCount; i++) {
            producerExecutor.execute(
                    new TaskProducer("Producer-" + (i + 1), taskQueue, stateTracker));
        }

        for (int i = 0; i < workerCount; i++) {
            workerExecutor.execute(
                    new TaskWorker("Worker-" + (i + 1), taskQueue, stateTracker, maxRetries));
        }

        new Thread(monitor).start();
        jsonExporter.start();
    }

    /**
     * Initiates a graceful shutdown of the task processing system.
     * <p>
     * Shutdown sequence:
     * </p>
     * <ol>
     *   <li>Stops producers (5s graceful period)</li>
     *   <li>Stops workers (10s graceful period)</li>
     *   <li>Stops system monitor</li>
     *   <li>Stops JSON exporter</li>
     * </ol>
     *
     * <p>Handles interrupts during shutdown process.</p>
     */
    public void shutdown() {
        producerExecutor.shutdown();
        try {
            if (!producerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                producerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            producerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        workerExecutor.shutdown();
        try {
            if (!workerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                workerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        monitor.stop();
        jsonExporter.stop();
    }

    /**
     * Gets the task queue instance.
     *
     * @return the priority blocking queue used for task distribution
     */
    public BlockingQueue<Task> getTaskQueue() {
        return taskQueue;
    }

    /**
     * Gets the task state tracker instance.
     *
     * @return the state tracker monitoring all task lifecycle events
     */
    public TaskStateTracker getStateTracker() {
        return stateTracker;
    }
}