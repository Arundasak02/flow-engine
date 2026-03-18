package com.flow.core.runtime;

import java.util.*;

/**
 * Represents the complete runtime execution flow for a single trace.
 * This is the output sent to UI for animation.
 */
public class RuntimeFlow {

    private final String traceId;
    private final List<FlowStep> steps;
    private final long startTimestamp;
    private final long endTimestamp;
    private final long totalDurationMs;

    public RuntimeFlow(String traceId, List<FlowStep> steps) {
        this.traceId = Objects.requireNonNull(traceId, "traceId cannot be null");
        this.steps = steps != null ? new ArrayList<>(steps) : new ArrayList<>();

        if (!this.steps.isEmpty()) {
            this.startTimestamp = this.steps.get(0).getTimestamp();
            this.endTimestamp = this.steps.get(this.steps.size() - 1).getTimestamp();
            this.totalDurationMs = endTimestamp - startTimestamp;
        } else {
            this.startTimestamp = 0;
            this.endTimestamp = 0;
            this.totalDurationMs = 0;
        }
    }

    public String getTraceId() {
        return traceId;
    }

    public List<FlowStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public int getStepCount() {
        return steps.size();
    }

    public boolean hasErrors() {
        return steps.stream().anyMatch(FlowStep::hasError);
    }

    @Override
    public String toString() {
        return "RuntimeFlow{" +
                "traceId='" + traceId + '\'' +
                ", stepCount=" + steps.size() +
                ", totalDurationMs=" + totalDurationMs +
                ", hasErrors=" + hasErrors() +
                '}';
    }
}

