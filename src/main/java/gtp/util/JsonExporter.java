package gtp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import gtp.core.TaskStateTracker;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JsonExporter {
    private final TaskStateTracker tracker;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService scheduler;

    public JsonExporter(TaskStateTracker tracker) {
        this.tracker = tracker;
        this.mapper = new ObjectMapper();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::export, 1, 1, TimeUnit.MINUTES);
    }

    private void export() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            File output = new File("task_status_" + timestamp + ".json");
            mapper.writeValue(output, tracker.getTaskStates());
        } catch (Exception e) {
            System.err.println("Failed to export task status: " + e.getMessage());
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}