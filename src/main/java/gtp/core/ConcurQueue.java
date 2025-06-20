package gtp.core;

import gtp.config.AppConfig;
import gtp.util.GracefulShutdown;

public class ConcurQueue {
    public static void main(String[] args) {
        AppConfig config = new AppConfig();
        TaskDispatcher dispatcher = new TaskDispatcher(config);

        Runtime.getRuntime().addShutdownHook(new Thread(
                new GracefulShutdown(dispatcher)
        ));

        dispatcher.start();

        // Keep the main thread alive
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
