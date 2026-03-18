package com.flow.core.runtime.handlers;

import com.flow.core.graph.*;
import com.flow.core.runtime.*;

/**
 * Handles METHOD_ENTER events by creating runtime nodes.
 */
public class MethodEnterHandler implements RuntimeEventHandler {

    private static final int RUNTIME_ZOOM_LEVEL = 5;

    @Override
    public void handle(RuntimeEvent event, CoreGraph graph) {
        // Only create node if it doesn't exist (don't overwrite static nodes)
        if (graph.getNode(event.getNodeId()) == null) {
            CoreNode node = new CoreNode(
                event.getNodeId(),
                "Runtime: " + event.getNodeId(),
                NodeType.METHOD,
                null,
                Visibility.PUBLIC
            );
            node.setZoomLevel(RUNTIME_ZOOM_LEVEL);
            graph.addNode(node);
        }
    }

    @Override
    public boolean canHandle(EventType eventType) {
        return eventType == EventType.METHOD_ENTER;
    }
}

