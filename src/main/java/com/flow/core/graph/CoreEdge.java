package com.flow.core.graph;

import java.util.Objects;

/**
 * Represents a directed edge between two nodes in the flow graph.
 *
 * An edge represents a call, dependency, or data flow relationship.
 */
public class CoreEdge {

    private final String id;
    private final String sourceId; // references CoreNode.id
    private final String targetId; // references CoreNode.id
    private final String type; // e.g., "CALLS", "DEPENDS_ON", "FLOWS_TO"
    private long executionCount; // runtime execution count (0 for static only)

    public CoreEdge(String id, String sourceId, String targetId, String type) {
        this.id = Objects.requireNonNull(id, "Edge ID cannot be null");
        this.sourceId = Objects.requireNonNull(sourceId, "Source ID cannot be null");
        this.targetId = Objects.requireNonNull(targetId, "Target ID cannot be null");
        this.type = Objects.requireNonNull(type, "Edge type cannot be null");
        this.executionCount = 0;
    }

    public String getId() {
        return id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getType() {
        return type;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(long executionCount) {
        this.executionCount = executionCount;
    }

    public void incrementExecutionCount(long delta) {
        this.executionCount += delta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoreEdge)) return false;
        CoreEdge coreEdge = (CoreEdge) o;
        return Objects.equals(id, coreEdge.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CoreEdge{" +
                "id='" + id + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", targetId='" + targetId + '\'' +
                ", type='" + type + '\'' +
                ", executionCount=" + executionCount +
                '}';
    }
}

