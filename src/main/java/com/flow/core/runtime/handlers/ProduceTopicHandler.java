package com.flow.core.runtime.handlers;

import com.flow.core.graph.*;
import com.flow.core.runtime.*;

/**
 * Handles PRODUCE_TOPIC events by creating topic nodes and edges.
 */
public class ProduceTopicHandler implements RuntimeEventHandler {

    private static final int RUNTIME_ZOOM_LEVEL = 5;

    @Override
    public void handle(RuntimeEvent event, CoreGraph graph) {
        // Create runtime topic node if needed
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

        // Create PRODUCES edge if topicId is present
        String topicId = (String) event.getDataValue("topicId");
        if (topicId != null && graph.getNode(topicId) != null) {
            String edgeId = "runtime:produces:" + event.getNodeId() + "->" + topicId;
            if (graph.getEdge(edgeId) == null) {
                graph.addEdge(new CoreEdge(edgeId, event.getNodeId(), topicId, EdgeType.PRODUCES));
            }
        }
    }

    @Override
    public boolean canHandle(EventType eventType) {
        return eventType == EventType.PRODUCE_TOPIC;
    }
}

