package com.flow.core.export;

import com.flow.core.graph.CoreEdge;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;

/**
 * Exports a CoreGraph to GEF (Graph Exchange Format) JSON.
 *
 * GEF is a JSON-based format suitable for graph visualization tools.
 * Exports graph structure with nodes, edges, and layout information.
 *
 * Example structure:
 * {
 *   "version": "1",
 *   "nodes": [
 *     { "id": "ep1", "label": "GET /api/users", "type": "ENDPOINT", "zoomLevel": 1, "x": 100, "y": 100 }
 *   ],
 *   "edges": [
 *     { "id": "e1", "source": "ep1", "target": "svc1", "label": "CALLS", "weight": 42 }
 *   ]
 * }
 */
public class GEFExporter {

    /**
     * Export the graph to GEF JSON format.
     *
     * @param graph the CoreGraph to export
     * @return JSON string in GEF format
     */
    public String export(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": \"").append(graph.getVersion()).append("\",\n");
        json.append("  \"nodes\": [\n");

        // Export nodes
        List<CoreNode> nodes = new ArrayList<>(graph.getAllNodes());
        for (int i = 0; i < nodes.size(); i++) {
            CoreNode node = nodes.get(i);
            json.append("    ").append(nodeToJson(node));
            if (i < nodes.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ],\n");
        json.append("  \"edges\": [\n");

        // Export edges
        List<CoreEdge> edges = new ArrayList<>(graph.getAllEdges());
        for (int i = 0; i < edges.size(); i++) {
            CoreEdge edge = edges.get(i);
            json.append("    ").append(edgeToJson(edge));
            if (i < edges.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        return json.toString();
    }

    private String nodeToJson(CoreNode node) {
        return String.format(
                "{\"id\": \"%s\", \"label\": \"%s\", \"type\": \"%s\", \"zoomLevel\": %d, \"isPublic\": %s}",
                escapeJson(node.getId()),
                escapeJson(node.getName()),
                node.getType(),
                node.getZoomLevel(),
                node.isPublic()
        );
    }

    private String edgeToJson(CoreEdge edge) {
        return String.format(
                "{\"id\": \"%s\", \"source\": \"%s\", \"target\": \"%s\", \"label\": \"%s\", \"weight\": %d}",
                escapeJson(edge.getId()),
                escapeJson(edge.getSourceId()),
                escapeJson(edge.getTargetId()),
                edge.getType(),
                edge.getExecutionCount()
        );
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

