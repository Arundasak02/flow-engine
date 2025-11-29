package com.flow.core.runtime.handlers;

import com.flow.core.graph.*;
import com.flow.core.runtime.*;

/**
 * Handles CONSUME_TOPIC events by creating consumer nodes.
 */
public class ConsumeTopicHandler implements RuntimeEventHandler {

    private static final int RUNTIME_ZOOM_LEVEL = 5;

    @Override
    public void handle(RuntimeEvent event, CoreGraph graph) {
        // Create runtime consumer node if needed
        if (graph.getNode(event.getNodeId()) == null) {
            CoreNode node = new CoreNode(
                event.getNodeId(),
                "Runtime: " + event.getNodeId(),
                NodeType.TOPIC,
                null,
                Visibility.PUBLIC
            );
            node.setZoomLevel(RUNTIME_ZOOM_LEVEL);
            graph.addNode(node);
        }
    }

    @Override
    public boolean canHandle(EventType eventType) {
        return eventType == EventType.CONSUME_TOPIC;
    }
}

