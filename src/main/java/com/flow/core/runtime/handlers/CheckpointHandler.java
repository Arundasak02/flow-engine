package com.flow.core.runtime.handlers;

import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;
import com.flow.core.runtime.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles CHECKPOINT events by attaching checkpoint data to nodes.
 */
public class CheckpointHandler implements RuntimeEventHandler {

    @Override
    public void handle(RuntimeEvent event, CoreGraph graph) {
        CoreNode node = graph.getNode(event.getNodeId());
        if (node != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> checkpoints = (Map<String, Object>) node.getMetadata("checkpoints");
            if (checkpoints == null) {
                checkpoints = new java.util.HashMap<>();
                node.setMetadata("checkpoints", checkpoints);
            }
            checkpoints.putAll(event.getData());
        }
    }

    @Override
    public boolean canHandle(EventType eventType) {
        return eventType == EventType.CHECKPOINT;
    }
}

