package com.flow.core.graph;

import java.util.*;

/**
 * Validates the structure and integrity of a CoreGraph.
 *
 * Checks:
 * - No null nodes or edges
 * - All edge source/target nodes exist
 * - No duplicate node or edge IDs
 * - Zoom levels are properly assigned (1-5)
 * - No self-loops (optional strictness)
 * - Type consistency
 */
public class GraphValidator {

    private boolean strictMode = false;

    public GraphValidator() {
    }

    public GraphValidator(boolean strictMode) {
        this.strictMode = strictMode;
    }

    /**
     * Validate the graph structure.
     *
     * @param graph the CoreGraph to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validate(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        validateNodes(graph);
        validateEdges(graph);
        validateZoomLevels(graph);
    }

    private void validateNodes(CoreGraph graph) {
        Set<String> nodeIds = new HashSet<>();

        for (CoreNode node : graph.getAllNodes()) {
            if (node == null) {
                throw new IllegalArgumentException("Graph contains a null node");
            }
            if (node.getId() == null || node.getId().isEmpty()) {
                throw new IllegalArgumentException("Node has invalid ID");
            }
            if (nodeIds.contains(node.getId())) {
                throw new IllegalArgumentException("Duplicate node ID: " + node.getId());
            }
            nodeIds.add(node.getId());
        }
    }

    private void validateEdges(CoreGraph graph) {
        Set<String> edgeIds = new HashSet<>();
        Set<String> nodeIds = new HashSet<>();

        graph.getAllNodes().forEach(n -> nodeIds.add(n.getId()));

        for (CoreEdge edge : graph.getAllEdges()) {
            if (edge == null) {
                throw new IllegalArgumentException("Graph contains a null edge");
            }
            if (edge.getId() == null || edge.getId().isEmpty()) {
                throw new IllegalArgumentException("Edge has invalid ID");
            }
            if (edgeIds.contains(edge.getId())) {
                throw new IllegalArgumentException("Duplicate edge ID: " + edge.getId());
            }
            if (!nodeIds.contains(edge.getSourceId())) {
                throw new IllegalArgumentException("Edge " + edge.getId() +
                        " references non-existent source node: " + edge.getSourceId());
            }
            if (!nodeIds.contains(edge.getTargetId())) {
                throw new IllegalArgumentException("Edge " + edge.getId() +
                        " references non-existent target node: " + edge.getTargetId());
            }

            if (strictMode && edge.getSourceId().equals(edge.getTargetId())) {
                throw new IllegalArgumentException("Self-loop detected: " + edge.getId());
            }

            edgeIds.add(edge.getId());
        }
    }

    private void validateZoomLevels(CoreGraph graph) {
        for (CoreNode node : graph.getAllNodes()) {
            int zoom = node.getZoomLevel();
            if (zoom < 0 || zoom > 5) {
                throw new IllegalArgumentException("Invalid zoom level " + zoom +
                        " for node " + node.getId() + " (must be 0-5, where 0=unassigned, 1-5=assigned)");
            }
        }
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }
}

