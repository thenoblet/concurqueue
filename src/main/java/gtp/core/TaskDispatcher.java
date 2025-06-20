package gtp.core;

import gtp.config.AppConfig;
import gtp.model.Task;
import gtp.producer.TaskProducer;
import gtp.worker.TaskWorker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TaskDispatcher {
    private final BlockingQueue<Task> taskQueue;
    private final TaskStateTracker stateTracker;
    private final ExecutorService producerExecutor;
    private final ExecutorService workerExecutor;
    private final SystemMonitor monitor;
    private final int producerCount;
    private final int workerCount;
    private final int maxRetries;

    public TaskDispatcher(AppConfig config) {
        this.producerCount = config.getProducerCount();
        this.workerCount = config.getWorkerCount();
        this.maxRetries = config.getMaxRetries();
        this.taskQueue = new PriorityBlockingQueue<>(config.getQueueCapacity());
        this.stateTracker = new TaskStateTracker();
        this.producerExecutor = Executors.newFixedThreadPool(producerCount);
        this.workerExecutor = Executors.newFixedThreadPool(workerCount);
        this.monitor = new SystemMonitor(taskQueue, workerExecutor, stateTracker);
    }

    public void start() {
        // Start producers
        for (int i = 0; i < producerCount; i++) {
            producerExecutor.execute(
                    new TaskProducer("Producer-" + (i + 1), taskQueue, stateTracker));
        }

        // Start workers
        for (int i = 0; i < workerCount; i++) {
            workerExecutor.execute(
                    new TaskWorker("Worker-" + (i + 1), taskQueue, stateTracker, maxRetries));
        }

        // Start monitor in a separate thread
        new Thread(monitor).start();
    }

    public void shutdown() {
        // Shutdown producers first
        producerExecutor.shutdown();
        try {
            if (!producerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                producerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            producerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Shutdown workers
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
    }

    public BlockingQueue<Task> getTaskQueue() {
        return taskQueue;
    }

    public TaskStateTracker getStateTracker() {
        return stateTracker;
    }
}