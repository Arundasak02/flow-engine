package com.flow.core.zoom;

import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.Objects;

/**
 * Assigns zoom levels to all nodes in the graph.
 *
 * Zoom levels determine the granularity of the graph visualization:
 * 1 = BUSINESS (endpoints, topics, business operations)
 * 2 = SERVICE (services, classes)
 * 3 = PUBLIC (public methods, exported functions)
 * 4 = PRIVATE (private/internal methods)
 * 5 = RUNTIME (actual runtime execution nodes)
 *
 * The zoom engine uses a policy-based approach to assign levels
 * based on node type and visibility.
 */
public class ZoomEngine {

    private final ZoomPolicy policy;

    public ZoomEngine() {
        this.policy = new ZoomPolicy();
    }

    public ZoomEngine(ZoomPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "Policy cannot be null");
    }

    /**
     * Assign zoom levels to all nodes in the graph.
     *
     * For METHOD nodes, the zoom level is refined based on visibility:
     * - public methods → zoom level 3
     * - private methods → zoom level 4
     *
     * @param graph the CoreGraph to process
     * @throws IllegalArgumentException if a node type has no zoom level assigned
     */
    public void assignZoomLevels(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        for (CoreNode node : graph.getAllNodes()) {
            assignZoomLevel(node);
        }
    }

    private void assignZoomLevel(CoreNode node) {
        int zoomLevel = policy.getZoomLevel(node.getType());

        if (zoomLevel < 1 || zoomLevel > 5) {
            throw new IllegalArgumentException(
                    "No zoom level configured for node type: " + node.getType() +
                    " (node ID: " + node.getId() + ")");
        }

        // Refine METHOD nodes based on visibility (PRIVATE_METHOD is already level 4)
        if ("METHOD".equals(node.getType())) {
            zoomLevel = node.isPublic() ? 3 : 4;
        }

        node.setZoomLevel(zoomLevel);
    }

    /**
     * Get the zoom policy.
     *
     * @return the ZoomPolicy
     */
    public ZoomPolicy getPolicy() {
        return policy;
    }
}

