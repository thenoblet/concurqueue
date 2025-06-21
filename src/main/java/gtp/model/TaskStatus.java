package gtp.model;

/**
 * Represents the lifecycle states of a {@link Task} in the processing system.
 * <p>
 * The state progression typically flows from SUBMITTED → PROCESSING → (COMPLETED or FAILED).
 * This enum enables tracking and monitoring of task execution throughout the system.
 * </p>
 *
 * <p>Possible states:</p>
 * <ul>
 *   <li><b>SUBMITTED</b>: Task has been created and queued for processing</li>
 *   <li><b>PROCESSING</b>: A worker is currently executing a task</li>
 *   <li><b>COMPLETED</b>: Task finished successfully</li>
 *   <li><b>FAILED</b>: Task encountered an error during processing</li>
 * </ul>
 *
 * @see Task
 * @see gtp.core.TaskStateTracker
 */
public enum TaskStatus {
    SUBMITTED,
    PROCESSING,
    COMPLETED,
    FAILED
}
