package gtp.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized configuration manager for application settings.
 *
 * <p>This class loads configuration properties from the application.properties file
 * and provides typed access to various application settings with default values.</p>
 *
 * <p>Configuration properties supported:</p>
 * <ul>
 *   <li><b>producer.count</b>: Number of producer threads (default: 3)</li>
 *   <li><b>worker.count</b>: Number of worker threads (default: 3)</li>
 *   <li><b>queue.capacity</b>: Capacity of work queue (default: 10)</li>
 *   <li><b>max.retries</b>: Maximum retry attempts for operations (default: 3)</li>
 *   <li><b>monitor.interval.seconds</b>: Monitoring interval in seconds (default: 5)</li>
 *   <li><b>json.exporter.interval</b>: JSON export interval in seconds (default: 5)</li>
 * </ul>
 *
 * @throws RuntimeException if the configuration file cannot be loaded
 */
public class AppConfig {
    private final Properties props;

    /**
     * Initializes the configuration by loading properties from application.properties.
     * The configuration file must be present in the classpath.
     *
     * @throws RuntimeException if the configuration file cannot be loaded or parsed
     */
    public AppConfig() {
        props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    /**
     * Gets the number of producer threads to create.
     *
     * @return configured producer count or default value (3) if not specified
     */
    public int getProducerCount() {
        return Integer.parseInt(props.getProperty("producer.count", "3"));
    }

    /**
     * Gets the number of worker threads to create.
     *
     * @return configured worker count or default value (3) if not specified
     */
    public int getWorkerCount() {
        return Integer.parseInt(props.getProperty("worker.count", "3"));
    }

    /**
     * Gets the capacity of the work queue.
     *
     * @return configured queue capacity or default value (10) if not specified
     */
    public int getQueueCapacity() {
        return Integer.parseInt(props.getProperty("queue.capacity", "10"));
    }

    /**
     * Gets the maximum number of retry attempts for operations.
     *
     * @return configured maximum retries or default value (3) if not specified
     */
    public int getMaxRetries() {
        return Integer.parseInt(props.getProperty("max.retries", "3"));
    }

    /**
     * Gets the monitoring interval in seconds.
     *
     * @return configured monitoring interval or default value (5) if not specified
     */
    public int getMonitorIntervalSeconds() {
        return Integer.parseInt(props.getProperty("monitor.interval.seconds", "5"));
    }

    /**
     * Gets the JSON export interval in seconds.
     *
     * @return configured export interval or default value (5) if not specified
     */
    public int getJsonExportInterval() {
        return Integer.parseInt(props.getProperty("json.exporter.interval", "5"));
    }

    public String getJsonExportDir() {
        return props.getProperty("json.exporter.dir", "exports/");
    }
}