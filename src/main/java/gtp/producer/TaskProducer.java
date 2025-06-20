package gtp.producer;

import gtp.core.TaskStateTracker;
import gtp.model.Task;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskProducer implements Runnable {
    private final BlockingQueue<Task> taskQueue;
    private final TaskStateTracker stateTracker;
    private final String producerId;
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    private final Random random = new Random();
    private volatile boolean running = true;

    public TaskProducer(String producerId, BlockingQueue<Task> taskQueue, TaskStateTracker stateTracker) {
        this.producerId = producerId;
        this.taskQueue = taskQueue;
        this.stateTracker = stateTracker;
    }

    @Override
    public void run() {
        try {
            while (running) {
                int tasksToGenerate = random.nextInt(3) + 1; // 1-3 tasks
                for (int i = 0; i < tasksToGenerate; i++) {
                    Task task = generateTask();
                    stateTracker.registerTask(task);

                    try {
                        if (!taskQueue.offer(task, 1, TimeUnit.SECONDS)) {
                            System.out.printf("[%s] Queue full, dropping task %s%n", producerId, task);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    System.out.printf("[%s] Submitted %s (Queue size: %d)%n",
                            producerId, task, taskQueue.size());
                }

                Thread.sleep(random.nextInt(1000) + 1000); // 1-4 sec delay
            }
        } catch (InterruptedException e) {
            System.out.printf("[%s] Producer interrupted, shutting down%n", producerId);
            Thread.currentThread().interrupt();
        }
    }

    private Task generateTask() {
        // 5% chance to generate deadlock demo task
        if (random.nextInt(100) < 5) {
            return new Task("DEADLOCK-DEMO", 1, "deadlock");
        }

        int priority = random.nextInt(5) + 1; // 1-5 priority
        String payload = "Payload-" + taskCounter.incrementAndGet();
        return new Task("Task-" + taskCounter.get(), priority, payload);
    }

    public void stop() {
        running = false;
    }
}
