package gtp.core;

import gtp.model.Task;
import gtp.model.TaskStatus;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TaskStateTracker {
    private final ConcurrentHashMap<UUID, TaskStatus> taskStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Task> taskMap = new ConcurrentHashMap<>();

    public void updateTaskStatus(UUID taskId, TaskStatus status) {
        taskStates.put(taskId, status);
    }

    public void registerTask(Task task) {
        taskMap.put(task.getId(), task);
        updateTaskStatus(task.getId(), TaskStatus.SUBMITTED);
    }

    public TaskStatus getTaskStatus(UUID taskId) {
        return taskStates.getOrDefault(taskId, null);
    }

    public ConcurrentHashMap<UUID, TaskStatus> getTaskStates() {
        return new ConcurrentHashMap<>(taskStates);
    }

    public Task getTask(UUID taskId) {
        return taskMap.get(taskId);
    }
}
