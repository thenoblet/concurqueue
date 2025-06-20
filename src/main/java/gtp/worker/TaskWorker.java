package gtp.worker;

import gtp.core.TaskStateTracker;
import gtp.model.Task;
import gtp.model.TaskStatus;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskWorker implements Runnable {
    private final BlockingQueue<Task> taskQueue;
    private final TaskStateTracker stateTracker;
    private final Random random = new Random();
    private final String workerId;
    private final int maxRetries;

    // Race condition demonstration
    private static int unsafeCounter = 0;
    private static final AtomicInteger safeCounter = new AtomicInteger(0);
    private static final Object counterLock = new Object();

    // Deadlock demonstration
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public TaskWorker(String workerId, BlockingQueue<Task> taskQueue,
                      TaskStateTracker stateTracker, int maxRetries) {
        this.workerId = workerId;
        this.taskQueue = taskQueue;
        this.stateTracker = stateTracker;
        this.maxRetries = maxRetries;
    }

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

    private void processTask(Task task) throws InterruptedException {
        demonstrateRaceCondition();
        demonstrateDeadlockIfNeeded(task);

        stateTracker.updateTaskStatus(task.getId(), TaskStatus.PROCESSING);

        unsafeCounter++;
        safeCounter.incrementAndGet();


        System.out.printf("[%s] Processing %s%n", workerId, task);

        try {
            // Simulate processing time (0.5-2.5 seconds)
            Thread.sleep(random.nextInt(10000) + 500);

            // Simulate occasional failures (10% chance)
            if (random.nextDouble() < 0.1 && task.getRetryCount() < maxRetries) {
                throw new RuntimeException("Simulated processing failure");
            }

            stateTracker.updateTaskStatus(task.getId(), TaskStatus.COMPLETED);
            System.out.printf("[%s] Completed %s%n", workerId, task);
        } catch (Exception e) {
            System.out.printf("[%s] Failed to process %s: %s%n", workerId, task, e.getMessage());
            stateTracker.updateTaskStatus(task.getId(), TaskStatus.FAILED);

            if (task.getRetryCount() < maxRetries) {
                task.incrementRetryCount();
                taskQueue.put(task);
                System.out.printf("[%s] Re-queued %s for retry (attempt %d/%d)%n",
                        workerId, task, task.getRetryCount(), maxRetries);
            } else {
                System.out.printf("[%s] Max retries reached for %s%n", workerId, task);
            }
        }
    }

    private void demonstrateRaceCondition() {
        // Force more contention by incrementing multiple times
        for (int i = 0; i < 1000; i++) {
            // UNSAFE: Race condition
            unsafeCounter++;

            // SAFE: Atomic increment (fix)
            safeCounter.incrementAndGet();
        }
    }

    // Deadlock demo (optional)
    private void demonstrateDeadlockIfNeeded(Task task) {
        if (task.getName().contains("DEADLOCK")) {
            System.out.println("\n=== Initiating Deadlock Scenario ===");

            Thread thread1 = new Thread(() -> {
                synchronized (lockA) {
                    System.out.println("Thread1 acquired lockA");
                    try { Thread.sleep(100); } catch (InterruptedException e) {}

                    synchronized (lockB) {  // Will deadlock
                        System.out.println("Thread1 acquired lockB (never reached)");
                    }
                }
            });

            Thread thread2 = new Thread(() -> {
                synchronized (lockB) {
                    System.out.println("Thread2 acquired lockB");
                    try { Thread.sleep(100); } catch (InterruptedException e) {}

                    synchronized (lockA) {  // Deadlock occurs here
                        System.out.println("Thread2 acquired lockA (never reached)");
                    }
                }
            });

            thread1.start();
            thread2.start();
        }
    }

    public static int getUnsafeCounter() {
        return unsafeCounter;
    }

    public static int getSafeCounter() {
        return safeCounter.get();
    }
}
