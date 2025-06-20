package gtp.config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private final Properties props;

    public AppConfig() {
        props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    public int getProducerCount() {
        return Integer.parseInt(props.getProperty("producer.count", "3"));
    }

    public int getWorkerCount() {
        return Integer.parseInt(props.getProperty("worker.count", "3"));
    }

    public int getQueueCapacity() {
        return Integer.parseInt(props.getProperty("queue.capacity", "10"));
    }

    public int getMaxRetries() {
        return Integer.parseInt(props.getProperty("max.retries", "3"));
    }

    public int getMonitorIntervalSeconds() {
        return Integer.parseInt(props.getProperty("monitor.interval.seconds", "5"));
    }
}