package com.flow.core.graph;

import java.util.*;

/**
 * Validates graph structure and integrity.
 * Checks: node/edge validity, referential integrity, zoom levels, optional self-loop detection.
 */
public class GraphValidator {

    private boolean strictMode = false;

    public GraphValidator() {
    }

    public GraphValidator(boolean strictMode) {
        this.strictMode = strictMode;
    }

    /**
     * Validates the complete graph structure.
     * Order: nodes → edges → zoom levels
     */
    public void validate(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        validateNodes(graph);
        validateEdges(graph);
        validateZoomLevels(graph);
    }

    // Node validation: null checks, ID validity, uniqueness
    private void validateNodes(CoreGraph graph) {
        Set<String> nodeIds = new HashSet<>();

        for (CoreNode node : graph.getAllNodes()) {
            validateNodeNotNull(node);
            validateNodeId(node);
            validateNodeIdUnique(node, nodeIds);
            nodeIds.add(node.getId());
        }
    }

    private void validateNodeNotNull(CoreNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Graph contains a null node");
        }
    }

    private void validateNodeId(CoreNode node) {
        if (node.getId() == null || node.getId().isEmpty()) {
            throw new IllegalArgumentException("Node has invalid ID");
        }
    }

    private void validateNodeIdUnique(CoreNode node, Set<String> nodeIds) {
        if (nodeIds.contains(node.getId())) {
            throw new IllegalArgumentException("Duplicate node ID: " + node.getId());
        }
    }

    // Edge validation: null checks, ID validity, uniqueness, referential integrity, self-loops
    private void validateEdges(CoreGraph graph) {
        Set<String> edgeIds = new HashSet<>();
        Set<String> nodeIds = collectNodeIds(graph);

        for (CoreEdge edge : graph.getAllEdges()) {
            validateEdgeNotNull(edge);
            validateEdgeId(edge);
            validateEdgeIdUnique(edge, edgeIds);
            validateEdgeNodes(edge, nodeIds);
            validateNoSelfLoop(edge);
            edgeIds.add(edge.getId());
        }
    }

    private Set<String> collectNodeIds(CoreGraph graph) {
        Set<String> nodeIds = new HashSet<>();
        graph.getAllNodes().forEach(n -> nodeIds.add(n.getId()));
        return nodeIds;
    }

    private void validateEdgeNotNull(CoreEdge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("Graph contains a null edge");
        }
    }

    private void validateEdgeId(CoreEdge edge) {
        if (edge.getId() == null || edge.getId().isEmpty()) {
            throw new IllegalArgumentException("Edge has invalid ID");
        }
    }

    private void validateEdgeIdUnique(CoreEdge edge, Set<String> edgeIds) {
        if (edgeIds.contains(edge.getId())) {
            throw new IllegalArgumentException("Duplicate edge ID: " + edge.getId());
        }
    }

    // Ensures both source and target nodes exist in the graph
    private void validateEdgeNodes(CoreEdge edge, Set<String> nodeIds) {
        if (!nodeIds.contains(edge.getSourceId())) {
            throw new IllegalArgumentException("Edge " + edge.getId() +
                    " references non-existent source node: " + edge.getSourceId());
        }
        if (!nodeIds.contains(edge.getTargetId())) {
            throw new IllegalArgumentException("Edge " + edge.getId() +
                    " references non-existent target node: " + edge.getTargetId());
        }
    }

    // In strict mode, rejects edges where source equals target
    private void validateNoSelfLoop(CoreEdge edge) {
        if (strictMode && edge.getSourceId().equals(edge.getTargetId())) {
            throw new IllegalArgumentException("Self-loop detected: " + edge.getId());
        }
    }

    // Zoom level validation: must be 0 (unassigned) or 1-5 (assigned)
    private void validateZoomLevels(CoreGraph graph) {
        for (CoreNode node : graph.getAllNodes()) {
            validateZoomLevel(node);
        }
    }

    private void validateZoomLevel(CoreNode node) {
        int zoom = node.getZoomLevel();
        if (zoom < 0 || zoom > 5) {
            throw new IllegalArgumentException("Invalid zoom level " + zoom +
                    " for node " + node.getId() + " (must be 0-5, where 0=unassigned, 1-5=assigned)");
        }
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }
}

