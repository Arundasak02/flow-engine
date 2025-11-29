package com.flow.core.ingest;

import com.flow.core.graph.*;

import java.util.*;

/**
 * Ingests runtime execution events into the graph.
 *
 * Expected event format:
 * {
 *   "sourceId": "nodeId",
 *   "targetId": "nodeId",
 *   "executionCount": 42
 * }
 *
 * Creates missing runtime nodes automatically. Updates edge execution counts.
 */
public class RuntimeEventIngestor {

    public void ingest(List<Map<String, Object>> events, CoreGraph graph) {
        Objects.requireNonNull(events, "Events list cannot be null");
        Objects.requireNonNull(graph, "Graph cannot be null");

        events.forEach(event -> ingestEvent(event, graph));
    }

    // Processes single event: validates, ensures nodes exist, updates edge count
    private void ingestEvent(Map<String, Object> event, CoreGraph graph) {
        String sourceId = (String) event.get("sourceId");
        String targetId = (String) event.get("targetId");

        if (!isValidEvent(sourceId, targetId)) {
            return;
        }

        ensureNodesExist(graph, sourceId, targetId);
        updateEdge(graph, sourceId, targetId, event);
    }

    private boolean isValidEvent(String sourceId, String targetId) {
        return sourceId != null && targetId != null;
    }

    private void ensureNodesExist(CoreGraph graph, String sourceId, String targetId) {
        ensureNodeExists(graph, sourceId);
        ensureNodeExists(graph, targetId);
    }

    private void ensureNodeExists(CoreGraph graph, String nodeId) {
        if (graph.getNode(nodeId) == null) {
            CoreNode node = createRuntimeNode(nodeId);
            graph.addNode(node);
        }
    }

    private CoreNode createRuntimeNode(String nodeId) {
        return new CoreNode(
                nodeId,
                "Runtime: " + nodeId,
                NodeType.METHOD,
                null,
                Visibility.PUBLIC
        );
    }

    private void updateEdge(CoreGraph graph, String sourceId, String targetId, Map<String, Object> event) {
        String edgeId = sourceId + "->" + targetId;
        CoreEdge edge = getOrCreateEdge(graph, edgeId, sourceId, targetId);
        updateExecutionCount(edge, event);
    }

    private CoreEdge getOrCreateEdge(CoreGraph graph, String edgeId, String sourceId, String targetId) {
        CoreEdge edge = graph.getEdge(edgeId);

        if (edge == null) {
            edge = new CoreEdge(edgeId, sourceId, targetId, EdgeType.RUNTIME_CALL);
            graph.addEdge(edge);
        }

        return edge;
    }

    private void updateExecutionCount(CoreEdge edge, Map<String, Object> event) {
        Long executionCount = getNumericValue(event.get("executionCount"));
        if (executionCount != null) {
            edge.setExecutionCount(executionCount);
        }
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

