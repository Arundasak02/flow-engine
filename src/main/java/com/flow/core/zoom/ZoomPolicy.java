package com.flow.core.zoom;

import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;

/**
 * Defines zoom level assignment policies.
 *
 * A ZoomPolicy determines how to map node types to zoom levels
 * based on node properties (type, visibility, etc.)
 */
public class ZoomPolicy {

    private final Map<String, Integer> typeToZoomLevel;

    public ZoomPolicy() {
        this.typeToZoomLevel = new HashMap<>();
        initializeDefaults();
    }

    /**
     * Initialize default zoom level assignments.
     *
     * Note: The ZoomEngine will refine METHOD nodes to level 3 or 4 based on visibility.
     */
    private void initializeDefaults() {
        // Level 1: Business endpoints and topics
        typeToZoomLevel.put("ENDPOINT", 1);
        typeToZoomLevel.put("TOPIC", 1);
        typeToZoomLevel.put("BUSINESS_OPERATION", 1);

        // Level 2: Services and classes
        typeToZoomLevel.put("SERVICE", 2);
        typeToZoomLevel.put("CLASS", 2);
        typeToZoomLevel.put("COMPONENT", 2);

        // Level 3: Public methods and functions
        // Will be refined in ZoomEngine based on isPublic flag
        typeToZoomLevel.put("METHOD", 3);
        typeToZoomLevel.put("FUNCTION", 3);

        // Level 4: Private/internal methods
        typeToZoomLevel.put("PRIVATE_METHOD", 4);
        typeToZoomLevel.put("INTERNAL_FUNCTION", 4);

        // Level 5: Runtime execution nodes
        typeToZoomLevel.put("RUNTIME_CALL", 5);
        typeToZoomLevel.put("STACK_FRAME", 5);
    }

    /**
     * Get the zoom level for a node type.
     *
     * @param nodeType the node type
     * @return the zoom level (1-5), or -1 if not found
     */
    public int getZoomLevel(String nodeType) {
        return typeToZoomLevel.getOrDefault(nodeType, -1);
    }

    /**
     * Set a custom zoom level for a node type.
     *
     * @param nodeType the node type
     * @param zoomLevel the zoom level (1-5)
     */
    public void setZoomLevel(String nodeType, int zoomLevel) {
        if (zoomLevel < 1 || zoomLevel > 5) {
            throw new IllegalArgumentException("Zoom level must be between 1 and 5");
        }
        typeToZoomLevel.put(nodeType, zoomLevel);
    }

    /**
     * Get all configured type-to-zoom mappings.
     *
     * @return unmodifiable map of type to zoom level
     */
    public Map<String, Integer> getZoomLevelMap() {
        return Collections.unmodifiableMap(typeToZoomLevel);
    }
}

