package gtp.producer;

import gtp.core.TaskStateTracker;
import gtp.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Runnable implementation that generates and submits tasks to a shared task queue.
 * <p>
 * The producer generates tasks with random priorities and payloads, with a small chance
 * (5%) to generate special deadlock demonstration tasks. It submits these tasks to a
 * shared blocking queue and tracks their lifecycle using a {@link TaskStateTracker}.
 * </p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Generates 1-3 tasks per production cycle</li>
 *   <li>Random delays (1-4 seconds) between production cycles</li>
 *   <li>Graceful handling of queue full situations</li>
 *   <li>Proper interrupt response and shutdown support</li>
 * </ul>
 *
 * @see Runnable
 * @see BlockingQueue
 * @see TaskStateTracker
 */
public class TaskProducer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TaskProducer.class);

    private final BlockingQueue<Task> taskQueue;
    private final TaskStateTracker stateTracker;
    private final String producerId;
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    private final Random random = new Random();
    private volatile boolean running = true;

    /**
     * Constructs a new TaskProducer instance.
     *
     * @param producerId the unique identifier for this producer
     * @param taskQueue the shared queue to which tasks will be submitted
     * @param stateTracker the tracker used to monitor task lifecycle
     */
    public TaskProducer(String producerId, BlockingQueue<Task> taskQueue, TaskStateTracker stateTracker) {
        this.producerId = producerId;
        this.taskQueue = taskQueue;
        this.stateTracker = stateTracker;
    }

    /**
     * The main execution loop for the task producer.
     * <p>
     * Continuously generates and submits tasks until stopped. Each iteration:
     * </p>
     * <ol>
     *   <li>Generates 1-3 tasks</li>
     *   <li>Attempts to submit each task with a 1-second timeout</li>
     *   <li>Sleeps for 1-4 seconds before next cycle</li>
     * </ol>
     *
     * <p>Responds to both interrupt signals and stop requests.</p>
     */
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
                            logger.warn("[{}] Queue full, dropping task {}", producerId, task);
                            continue;
                        }
                    } catch (InterruptedException e) {
                        logger.warn("[{}] Task submission interrupted", producerId);
                        Thread.currentThread().interrupt();
                        return;
                    }

                    logger.info("[{}] Submitted {} (Queue size: {})",
                            producerId, task, taskQueue.size());
                }

                Thread.sleep(random.nextInt(1000) + 1000); // 1-4 sec delay
            }
        } catch (InterruptedException e) {
            logger.info("[{}] Producer interrupted, shutting down", producerId);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generates a new task with random characteristics.
     * <p>
     * There is a 5% chance to generate a special deadlock demonstration task.
     * Normal tasks have random priorities (1-5) and sequentially numbered payloads.
     * </p>
     *
     * @return a newly generated Task object
     */
    private Task generateTask() {
        // 5% chance to generate deadlock demo task
        if (random.nextInt(100) < 5) {
            logger.debug("[{}] Generating deadlock demo task", producerId);
            return new Task("DEADLOCK-DEMO", 1, "deadlock");
        }

        int priority = random.nextInt(5) + 1; // 1-5 priority
        String payload = "Payload-" + taskCounter.incrementAndGet();
        Task task = new Task("Task-" + taskCounter.get(), priority, payload);
        logger.debug("[{}] Generated new task: {}", producerId, task);
        return task;
    }

    /**
     * Signals the producer to stop after completing its current cycle.
     * <p>
     * This method is thread-safe and can be called from any thread to initiate
     * a graceful shutdown of the producer.
     * </p>
     */
    public void stop() {
        running = false;
        logger.debug("[{}] Stop requested", producerId);
    }
}