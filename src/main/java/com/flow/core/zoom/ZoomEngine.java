package com.flow.core.zoom;

import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;
import com.flow.core.graph.NodeType;

import java.util.Objects;

/**
 * Assigns zoom levels to graph nodes using policy-based rules.
 *
 * Zoom levels (1-5):
 * 1=Business (endpoints, topics), 2=Service/Class, 3=Public methods,
 * 4=Private methods, 5=Runtime execution
 *
 * Special handling: METHOD nodes refined based on visibility (public→3, private→4)
 */
public class ZoomEngine {

    private final ZoomPolicy policy;

    public ZoomEngine() {
        this.policy = new ZoomPolicy();
    }

    public ZoomEngine(ZoomPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "Policy cannot be null");
    }

    public void assignZoomLevels(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        for (CoreNode node : graph.getAllNodes()) {
            assignZoomLevel(node);
        }
    }

    private void assignZoomLevel(CoreNode node) {
        int zoomLevel = determineZoomLevel(node);
        validateZoomLevel(zoomLevel, node);
        node.setZoomLevel(zoomLevel);
    }

    // Refines METHOD type: public→3, private→4 (overrides policy default)
    private int determineZoomLevel(CoreNode node) {
        int zoomLevel = policy.getZoomLevel(node.getType());

        if (node.getType() == NodeType.METHOD) {
            return node.isPublic() ? 3 : 4;
        }

        return zoomLevel;
    }

    private void validateZoomLevel(int zoomLevel, CoreNode node) {
        if (zoomLevel < 1 || zoomLevel > 5) {
            throw new IllegalArgumentException(
                    "No zoom level configured for node type: " + node.getType() +
                    " (node ID: " + node.getId() + ")");
        }
    }

    public ZoomPolicy getPolicy() {
        return policy;
    }
}

