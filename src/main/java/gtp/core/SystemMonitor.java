package gtp.core;

import gtp.model.Task;
import gtp.model.TaskStatus;
import gtp.worker.*;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemMonitor implements Runnable {
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
                System.out.println("\n=== System Status ===");
                System.out.println("Queue size: " + taskQueue.size());

                ConcurrentHashMap<UUID, TaskStatus> states = stateTracker.getTaskStates();
                long submitted = states.values()
                        .stream()
                        .filter(s -> s == TaskStatus.SUBMITTED).count();
                long processing = states.values().
                        stream()
                        .filter(s -> s == TaskStatus.PROCESSING).count();
                long completed = states.values()
                        .stream()
                        .filter(s -> s == TaskStatus.COMPLETED).count();
                long failed = states.values()
                        .stream()
                        .filter(s -> s == TaskStatus.FAILED).count();

                System.out.printf("Tasks: %d submitted, %d processing, %d completed, %d failed%n",
                        submitted, processing, completed, failed);

                System.out.printf("Counters: unsafe=%d (race condition visible) / safe=%d (correct)%n",
                        TaskWorker.unsafeCounter,
                        TaskWorker.safeCounter.get());

                System.out.println("Thread pool: " + executorService.toString());
                System.out.println("=====================\n");

                TimeUnit.SECONDS.sleep(5);
            }
        } catch (InterruptedException e) {
            System.out.println("Monitor interrupted, shutting down");
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running = false;
    }
}
