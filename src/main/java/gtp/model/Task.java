package gtp.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a unit of work in the task processing system.
 * <p>
 * Tasks are comparable based on their priority (higher priority comes first)
 * and contain all necessary information for execution. Each task is assigned:
 * </p>
 * <ul>
 *   <li>A unique UUID identifier</li>
 *   <li>A creation timestamp</li>
 *   <li>A retry counter for failure handling</li>
 * </ul>
 *
 * <p>Natural ordering is determined by priority (descending).</p>
 */
public class Task implements Comparable<Task> {
    private final UUID id;
    private final String name;
    private final int priority; // Higher number = higher priority
    private final Instant createdTimestamp;
    private final String payload;
    private int retryCount;

    public Task(String name, int priority, String payload) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.priority = priority;
        this.createdTimestamp = Instant.now();
        this.payload = payload;
        this.retryCount = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getName() { return name; }
    public int getPriority() { return priority; }
    public Instant getCreatedTimestamp() { return createdTimestamp; }
    public String getPayload() { return payload; }
    public int getRetryCount() { return retryCount; }
    public void incrementRetryCount() { retryCount++; }

    @Override
    public int compareTo(Task other) {
        return Integer.compare(other.priority, this.priority);
    }

    /**
     * Returns a string representation of the task.
     * Excludes payload for security/brevity.
     *
     * @return formatted task description
     */
    @Override
    public String toString() {
        return "Task {" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                ", created=" + createdTimestamp +
                ", retries=" + retryCount +
                '}';
    }
}