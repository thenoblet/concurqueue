package gtp.util;

import gtp.core.TaskDispatcher;
import gtp.model.Task;

import java.util.concurrent.BlockingQueue;

public class GracefulShutdown implements Runnable {
    private final TaskDispatcher dispatcher;

    public GracefulShutdown(TaskDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        System.out.println("\nShutdown initiated, draining queue...");
        BlockingQueue<Task> queue = dispatcher.getTaskQueue();

        while (!queue.isEmpty()) {
            try {
                Task task = queue.poll(1, java.util.concurrent.TimeUnit.SECONDS);
                if (task != null) {
                    System.out.println("Processing remaining task: " + task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        dispatcher.shutdown();
        System.out.println("System shutdown complete");
    }
}
