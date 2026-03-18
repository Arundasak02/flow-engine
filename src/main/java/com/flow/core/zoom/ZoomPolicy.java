package com.flow.core.zoom;

import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;
import com.flow.core.graph.NodeType;

import java.util.*;

public class ZoomPolicy {

    private final Map<NodeType, Integer> typeToZoomLevel;

    public ZoomPolicy() {
        this.typeToZoomLevel = new EnumMap<>(NodeType.class);
        initializeDefaults();
    }

    private void initializeDefaults() {
        typeToZoomLevel.put(NodeType.ENDPOINT, 1);
        typeToZoomLevel.put(NodeType.TOPIC, 1);

        typeToZoomLevel.put(NodeType.SERVICE, 2);
        typeToZoomLevel.put(NodeType.CLASS, 2);
        typeToZoomLevel.put(NodeType.INTERFACE, 2);

        typeToZoomLevel.put(NodeType.METHOD, 3);

        typeToZoomLevel.put(NodeType.PRIVATE_METHOD, 4);
        typeToZoomLevel.put(NodeType.FIELD, 4);
        typeToZoomLevel.put(NodeType.CONSTRUCTOR, 3);
    }

    public int getZoomLevel(NodeType nodeType) {
        return typeToZoomLevel.getOrDefault(nodeType, -1);
    }

    public void setZoomLevel(NodeType nodeType, int zoomLevel) {
        if (zoomLevel < 1 || zoomLevel > 5) {
            throw new IllegalArgumentException("Zoom level must be between 1 and 5");
        }
        typeToZoomLevel.put(nodeType, zoomLevel);
    }

    public Map<NodeType, Integer> getZoomLevelMap() {
        return Collections.unmodifiableMap(typeToZoomLevel);
    }
}

