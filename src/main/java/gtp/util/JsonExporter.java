package gtp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import gtp.config.AppConfig;
import gtp.core.TaskStateTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically exports task state information to JSON files.
 * <p>
 * This component schedules regular exports of task state information from the
 * {@link TaskStateTracker} to timestamped JSON files in a configurable directory.
 * </p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Scheduled exports at configurable intervals</li>
 *   <li>Automatic directory creation if needed</li>
 *   <li>Timestamped output filenames</li>
 *   <li>Graceful shutdown support</li>
 * </ul>
 *
 * @see TaskStateTracker
 * @see AppConfig
 */
public class JsonExporter {
    private static final Logger logger = LoggerFactory.getLogger(JsonExporter.class);

    private final TaskStateTracker tracker;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService scheduler;
    private final long interval;
    private final Path exportsDir;

    /**
     * Constructs a new JsonExporter instance.
     *
     * @param tracker the TaskStateTracker to monitor for task states
     * @param config the AppConfig containing export settings (interval and directory)
     */
    public JsonExporter(TaskStateTracker tracker, AppConfig config) {
        this.tracker = tracker;
        this.mapper = new ObjectMapper();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "json-exporter-thread")
        );
        this.interval = config.getJsonExportInterval();
        this.exportsDir = Paths.get(config.getJsonExportDir());
    }

    /**
     * Starts the periodic export process.
     * <p>
     * Begins scheduled exports at the configured interval. The first export
     * occurs after the initial interval delay.
     * </p>
     */
    public void start() {
        logger.info("Starting JSON exporter with interval: {} minutes", interval);
        scheduler.scheduleAtFixedRate(
                this::export,
                interval,
                interval,
                TimeUnit.MINUTES
        );
    }

    /**
     * Performs a single export operation.
     * <p>
     * Creates the export directory if needed, generates a timestamped filename,
     * and writes the current task states to a JSON file.
     * </p>
     */
    private void export() {
        try {
            ensureExportDirectoryExists();
            Path outputPath = generateOutputPath();

            mapper.writeValue(outputPath.toFile(), tracker.getTaskStates());
            logger.info("Successfully exported task states to: {}", outputPath);

        } catch (Exception e) {
            logger.error("Failed to export task states", e);
        }
    }

    /**
     * Ensures the export directory exists, creating it if necessary.
     *
     * @throws Exception if directory creation fails
     */
    private void ensureExportDirectoryExists() throws Exception {
        if (!Files.exists(exportsDir)) {
            logger.debug("Creating export directory: {}", exportsDir);
            Files.createDirectories(exportsDir);
        }
    }

    /**
     * Generates a timestamped output file path.
     *
     * @return Path for the new export file with format: task_status_yyyyMMdd_HHmmss.json
     */
    private Path generateOutputPath() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("task_status_%s.json", timestamp);
        return exportsDir.resolve(filename);
    }

    /**
     * Initiates a graceful shutdown of the exporter.
     * <p>
     * Attempts to stop the scheduler gracefully, waiting up to 5 seconds
     * before forcing shutdown. Handles interrupts during shutdown.
     * </p>
     */
    public void stop() {
        logger.info("Shutting down JSON exporter");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Forcing shutdown of JSON exporter");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("JSON exporter shutdown interrupted");
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}