package com.flow.core.ingest;

import com.flow.core.graph.CoreEdge;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;

/**
 * Ingests runtime events into the CoreGraph.
 *
 * Runtime events are execution traces captured at runtime that record:
 * - Which methods/endpoints were actually called
 * - How many times (execution count)
 * - Call sequences and timing (optional)
 *
 * Expected event format (example):
 * {
 *   "timestamp": 1234567890,
 *   "sourceId": "meth1",
 *   "targetId": "meth2",
 *   "executionCount": 42,
 *   "duration": 150
 * }
 */
public class RuntimeEventIngestor {

    /**
     * Ingest runtime events into the graph.
     *
     * @param events list of runtime event maps
     * @param graph the CoreGraph to update
     */
    public void ingest(List<Map<String, Object>> events, CoreGraph graph) {
        Objects.requireNonNull(events, "Events list cannot be null");
        Objects.requireNonNull(graph, "Graph cannot be null");

        for (Map<String, Object> event : events) {
            ingestEvent(event, graph);
        }
    }

    private void ingestEvent(Map<String, Object> event, CoreGraph graph) {
        String sourceId = (String) event.get("sourceId");
        String targetId = (String) event.get("targetId");

        if (sourceId == null || targetId == null) {
            // Skip malformed events
            return;
        }

        CoreNode source = graph.getNode(sourceId);
        CoreNode target = graph.getNode(targetId);

        if (source == null || target == null) {
            // Create runtime nodes if they don't exist
            if (source == null) {
                source = createRuntimeNode(sourceId);
                graph.addNode(source);
            }
            if (target == null) {
                target = createRuntimeNode(targetId);
                graph.addNode(target);
            }
        }

        // Update or create edge with execution count
        String edgeId = sourceId + "->" + targetId;
        CoreEdge edge = graph.getEdge(edgeId);

        if (edge == null) {
            edge = new CoreEdge(edgeId, sourceId, targetId, "RUNTIME_CALL");
            graph.addEdge(edge);
        }

        // Update execution count
        Long executionCount = getNumericValue(event.get("executionCount"));
        if (executionCount != null) {
            edge.setExecutionCount(executionCount);
        }
    }

    private CoreNode createRuntimeNode(String nodeId) {
        // Create a minimal runtime node
        return new CoreNode(
                nodeId,
                "Runtime: " + nodeId,
                "RUNTIME_CALL",
                null,
                false
        );
    }

    private Long getNumericValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

