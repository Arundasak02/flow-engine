package com.flow.core.flow;

import java.util.*;

/**
 * Represents a flow from a starting point (e.g., endpoint) to its destinations.
 *
 * A FlowModel contains:
 * - The root/starting node (e.g., an API endpoint)
 * - All steps in the flow (sequence of nodes traversed)
 * - Graph structure showing connections between steps
 *
 * Flows are extracted per endpoint/topic using BFS or DFS traversal.
 */
public class FlowModel {

    private final String flowId;
    private final String startNodeId;
    private final String startNodeName;
    private final List<FlowStep> steps;
    private final long createdAt;

    public FlowModel(String flowId, String startNodeId, String startNodeName) {
        this.flowId = Objects.requireNonNull(flowId, "Flow ID cannot be null");
        this.startNodeId = Objects.requireNonNull(startNodeId, "Start node ID cannot be null");
        this.startNodeName = Objects.requireNonNull(startNodeName, "Start node name cannot be null");
        this.steps = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public String getFlowId() {
        return flowId;
    }

    public String getStartNodeId() {
        return startNodeId;
    }

    public String getStartNodeName() {
        return startNodeName;
    }

    /**
     * Add a step to the flow.
     *
     * @param step the FlowStep to add
     */
    public void addStep(FlowStep step) {
        Objects.requireNonNull(step, "Step cannot be null");
        steps.add(step);
    }

    /**
     * Get all steps in the flow.
     *
     * @return unmodifiable list of FlowStep objects
     */
    public List<FlowStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /**
     * Get the number of steps in the flow.
     */
    public int getStepCount() {
        return steps.size();
    }

    /**
     * Get the maximum depth of the flow (longest path from root).
     */
    public int getMaxDepth() {
        return steps.stream()
                .mapToInt(FlowStep::getDepth)
                .max()
                .orElse(0);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "FlowModel{" +
                "flowId='" + flowId + '\'' +
                ", startNodeId='" + startNodeId + '\'' +
                ", startNodeName='" + startNodeName + '\'' +
                ", steps=" + steps.size() +
                ", maxDepth=" + getMaxDepth() +
                '}';
    }
}

