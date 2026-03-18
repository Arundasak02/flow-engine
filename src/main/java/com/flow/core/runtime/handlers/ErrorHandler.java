package com.flow.core.runtime.handlers;

import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;
import com.flow.core.runtime.*;

/**
 * Handles ERROR events by attaching error information to nodes.
 */
public class ErrorHandler implements RuntimeEventHandler {

    @Override
    public void handle(RuntimeEvent event, CoreGraph graph) {
        CoreNode node = graph.getNode(event.getNodeId());
        if (node != null) {
            node.setMetadata("error", event.getData());
            node.setMetadata("status", "FAILED");
        }
    }

    @Override
    public boolean canHandle(EventType eventType) {
        return eventType == EventType.ERROR;
    }
}

