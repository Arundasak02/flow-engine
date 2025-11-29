package com.flow.core.flow;

import java.util.*;

/**
 * Represents a single step within a flow.
 *
 * A flow step is a node visited during traversal of the graph
 * from a starting point (e.g., an endpoint).
 */
public class FlowStep {

    private final String nodeId;
    private final String nodeName;
    private final int zoomLevel;
    private final int depth; // distance from the starting node
    private final List<String> previousSteps; // parent step node IDs

    public FlowStep(String nodeId, String nodeName, int zoomLevel, int depth, List<String> previousSteps) {
        this.nodeId = Objects.requireNonNull(nodeId, "Node ID cannot be null");
        this.nodeName = Objects.requireNonNull(nodeName, "Node name cannot be null");
        this.zoomLevel = zoomLevel;
        this.depth = depth;
        this.previousSteps = previousSteps != null ? new ArrayList<>(previousSteps) : new ArrayList<>();
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public int getDepth() {
        return depth;
    }

    public List<String> getPreviousSteps() {
        return Collections.unmodifiableList(previousSteps);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowStep)) return false;
        FlowStep flowStep = (FlowStep) o;
        return Objects.equals(nodeId, flowStep.nodeId) &&
               depth == flowStep.depth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, depth);
    }

    @Override
    public String toString() {
        return "FlowStep{" +
                "nodeId='" + nodeId + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", zoomLevel=" + zoomLevel +
                ", depth=" + depth +
                ", previousSteps=" + previousSteps +
                '}';
    }
}

