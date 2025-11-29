package com.flow.core.runtime;

import java.util.*;

/**
 * Represents a single step in the runtime execution flow.
 */
public class FlowStep {

    private final String nodeId;
    private final long timestamp;
    private final Long durationMs;
    private final Map<String, Object> checkpoints;
    private final Map<String, Object> error;
    private final String spanId;
    private final String parentSpanId;

    private FlowStep(Builder builder) {
        this.nodeId = Objects.requireNonNull(builder.nodeId, "nodeId cannot be null");
        this.timestamp = builder.timestamp;
        this.durationMs = builder.durationMs;
        this.checkpoints = builder.checkpoints != null ? new HashMap<>(builder.checkpoints) : new HashMap<>();
        this.error = builder.error != null ? new HashMap<>(builder.error) : null;
        this.spanId = builder.spanId;
        this.parentSpanId = builder.parentSpanId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public Map<String, Object> getCheckpoints() {
        return new HashMap<>(checkpoints);
    }

    public Map<String, Object> getError() {
        return error != null ? new HashMap<>(error) : null;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public boolean hasError() {
        return error != null;
    }

    public static Builder builder(String nodeId, long timestamp) {
        return new Builder(nodeId, timestamp);
    }

    public static class Builder {
        private final String nodeId;
        private final long timestamp;
        private Long durationMs;
        private Map<String, Object> checkpoints;
        private Map<String, Object> error;
        private String spanId;
        private String parentSpanId;

        private Builder(String nodeId, long timestamp) {
            this.nodeId = nodeId;
            this.timestamp = timestamp;
        }

        public Builder durationMs(Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder checkpoints(Map<String, Object> checkpoints) {
            this.checkpoints = checkpoints;
            return this;
        }

        public Builder error(Map<String, Object> error) {
            this.error = error;
            return this;
        }

        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder parentSpanId(String parentSpanId) {
            this.parentSpanId = parentSpanId;
            return this;
        }

        public FlowStep build() {
            return new FlowStep(this);
        }
    }

    @Override
    public String toString() {
        return "FlowStep{" +
                "nodeId='" + nodeId + '\'' +
                ", timestamp=" + timestamp +
                ", durationMs=" + durationMs +
                ", checkpoints=" + checkpoints +
                ", hasError=" + hasError() +
                ", spanId='" + spanId + '\'' +
                '}';
    }
}

