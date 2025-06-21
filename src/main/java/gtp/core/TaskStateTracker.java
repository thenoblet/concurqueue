package gtp.core;

import gtp.model.Task;
import gtp.model.TaskStatus;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe tracker for monitoring task lifecycle states.
 * <p>
 * Maintains two parallel mappings to track both task objects and their current states.
 * Provides atomic operations for registering tasks and updating their status.
 * </p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Thread-safe status tracking using {@link ConcurrentHashMap}</li>
 *   <li>Complete task object retention</li>
 *   <li>Atomic registration and status updates</li>
 *   <li>Immutable state snapshots</li>
 * </ul>
 */
public class TaskStateTracker {
    private final ConcurrentHashMap<UUID, TaskStatus> taskStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Task> taskMap = new ConcurrentHashMap<>();

    /**
     * Updates the status of a tracked task.
     *
     * @param taskId the UUID of the task to update
     * @param status the new status to record
     * @throws NullPointerException if either parameter is null
     */
    public void updateTaskStatus(UUID taskId, TaskStatus status) {
        taskStates.put(taskId, status);
    }

    /**
     * Registers a new task and sets its initial status to SUBMITTED.
     *
     * @param task the task to register (must not be null)
     * @throws NullPointerException if task is null
     */
    public void registerTask(Task task) {
        taskMap.put(task.getId(), task);
        updateTaskStatus(task.getId(), TaskStatus.SUBMITTED);
    }

    /**
     * Retrieves the current status of a task.
     *
     * @param taskId the UUID of the task to query
     * @return the current TaskStatus, or null if task is not tracked
     */
    public TaskStatus getTaskStatus(UUID taskId) {
        return taskStates.getOrDefault(taskId, null);
    }

    /**
     * Gets an immutable snapshot of all task states.
     *
     * @return new ConcurrentHashMap containing all current task states
     */
    public ConcurrentHashMap<UUID, TaskStatus> getTaskStates() {
        return new ConcurrentHashMap<>(taskStates);
    }

    /**
     * Retrieves a tracked task by its ID.
     *
     * @param taskId the UUID of the task to retrieve
     * @return the Task object, or null if not found
     */
    public Task getTask(UUID taskId) {
        return taskMap.get(taskId);
    }
}