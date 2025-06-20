package gtp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import gtp.config.AppConfig;
import gtp.core.TaskStateTracker;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JsonExporter {
    private final TaskStateTracker tracker;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService scheduler;
    private final long interval;

    public JsonExporter(TaskStateTracker tracker, AppConfig config) {
        this.tracker = tracker;
        this.mapper = new ObjectMapper();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.interval = config.getJsonExportInterval();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::export, interval, interval, TimeUnit.MINUTES);
    }

    private void export() {
        try {
            Path exportsDir = Paths.get("exports");
            if (!Files.exists(exportsDir)) {
                Files.createDirectories(exportsDir);
            }

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeFilename = "task_status_" + timestamp + ".json";
            Path outputPath = exportsDir.resolve(safeFilename);

            mapper.writeValue(outputPath.toFile(), tracker.getTaskStates());
            System.out.println("Successfully exported to " + outputPath);
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}