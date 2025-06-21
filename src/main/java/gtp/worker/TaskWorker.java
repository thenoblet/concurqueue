package gtp.worker;

import gtp.core.TaskStateTracker;
import gtp.model.Task;
import gtp.model.TaskStatus;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker implementation that processes tasks from a shared queue.
 * <p>
 * Each worker runs in its own thread and continuously takes tasks from the queue,
 * processes them, and updates their status. Includes demonstration capabilities
 * for common concurrency issues.
 * </p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Task processing with configurable failure rate</li>
 *   <li>Automatic retry mechanism up to maxRetries</li>
 *   <li>Thread-safe state tracking</li>
 *   <li>Demonstrations of race conditions and deadlocks</li>
 * </ul>
 */
public class TaskWorker implements Runnable {
    private final BlockingQueue<Task> taskQueue;
    private final TaskStateTracker stateTracker;
    private final Random random = new Random();
    private final String workerId;
    private final int maxRetries;

    // Race condition demonstration
    private static volatile int unsafeCounter = 0;
    private static final AtomicInteger safeCounter = new AtomicInteger(0);

    // Deadlock demonstration
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();
    private static final ScheduledExecutorService demoExecutor =
            Executors.newScheduledThreadPool(2);

    /**
     * Constructs a new TaskWorker.
     *
     * @param workerId unique identifier for this worker
     * @param taskQueue shared queue to take tasks from
     * @param stateTracker tracker for updating task states
     * @param maxRetries maximum number of retry attempts per task
     */
    public TaskWorker(String workerId, BlockingQueue<Task> taskQueue,
                      TaskStateTracker stateTracker, int maxRetries) {
        this.workerId = workerId;
        this.taskQueue = taskQueue;
        this.stateTracker = stateTracker;
        this.maxRetries = maxRetries;
    }

    /**
     * Main worker loop that continuously processes tasks until interrupted.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Task task = taskQueue.take();
                processTask(task);
            }
        } catch (InterruptedException e) {
            System.out.printf("[%s] Worker interrupted, shutting down%n", workerId);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Processes a single task including status updates and error handling.
     *
     * @param task the task to process
     * @throws InterruptedException if processing is interrupted
     */
    private void processTask(Task task) throws InterruptedException {
        demonstrateRaceCondition();
        demonstrateDeadlockIfNeeded(task);

        stateTracker.updateTaskStatus(task.getId(), TaskStatus.PROCESSING);
        System.out.printf("[%s] Processing %s%n", workerId, task);

        try {
            Thread.sleep(random.nextInt(10000) + 500);

            if (random.nextDouble() < 0.1 && task.getRetryCount() < maxRetries) {
                throw new RuntimeException("Simulated processing failure");
            }

            stateTracker.updateTaskStatus(task.getId(), TaskStatus.COMPLETED);
            System.out.printf("[%s] Completed %s%n", workerId, task);
        } catch (Exception e) {
            handleTaskFailure(task, e);
        }
    }

    /**
     * Demonstrates race condition by incrementing counters.
     */
    private void demonstrateRaceCondition() {
        for (int i = 0; i < 1000; i++) {
            unsafeCounter++;
            safeCounter.incrementAndGet();
        }
    }

    /**
     * Demonstrates deadlock if task is marked as DEADLOCK-DEMO.
     *
     * @param task the task being processed
     */
    private void demonstrateDeadlockIfNeeded(Task task) {
        if (task.getName().contains("DEADLOCK")) {
            System.out.println("\n=== Starting Deadlock Demo (5s max) ===");

            Future<?> future1 = demoExecutor.submit(() -> {
                synchronized (lockA) {
                    System.out.println("Thread1 acquired lockA");
                    try { Thread.sleep(100); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }

                    synchronized (lockB) {
                        System.out.println("Thread1 acquired lockB");
                    }
                }
            });

            Future<?> future2 = demoExecutor.submit(() -> {
                synchronized (lockB) {
                    System.out.println("Thread2 acquired lockB");
                    try { Thread.sleep(100); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }

                    synchronized (lockA) {
                        System.out.println("Thread2 acquired lockA");
                    }
                }
            });

            demoExecutor.schedule(() -> {
                future1.cancel(true);
                future2.cancel(true);
                System.out.println("=== Deadlock demo completed ===");
            }, 5, TimeUnit.SECONDS);
        }
    }

    /**
     * Handles task failure including retry logic.
     *
     * @param task the failed task
     * @param e the exception that caused the failure
     */
    private void handleTaskFailure(Task task, Exception e) {
        System.out.printf("[%s] Failed to process %s: %s%n", workerId, task, e.getMessage());
        stateTracker.updateTaskStatus(task.getId(), TaskStatus.FAILED);

        if (task.getRetryCount() < maxRetries) {
            task.incrementRetryCount();
            taskQueue.add(task);
            System.out.printf("[%s] Re-queued %s for retry (attempt %d/%d)%n",
                    workerId, task, task.getRetryCount(), maxRetries);
        } else {
            System.out.printf("[%s] Max retries reached for %s%n", workerId, task);
        }
    }

    /**
     * @return current value of the unsafe counter (for demo purposes)
     */
    public static int getUnsafeCounter() {
        return unsafeCounter;
    }

    /**
     * @return current value of the thread-safe counter (for demo purposes)
     */
    public static int getSafeCounter() {
        return safeCounter.get();
    }

    /**
     * Shuts down the deadlock demonstration executor.
     */
    public static void shutdown() {
        demoExecutor.shutdownNow();
        try {
            if (!demoExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("Deadlock demo threads did not terminate properly");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}