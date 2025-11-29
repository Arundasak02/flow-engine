package com.flow.core.export;

import com.flow.core.graph.CoreEdge;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;

/**
 * Exports graph to GEF (Graph Exchange Format) JSON for visualization tools.
 *
 * Output structure:
 * {
 *   "version": "1",
 *   "nodes": [{id, label, type, zoomLevel, isPublic}],
 *   "edges": [{id, source, target, label, weight}]
 * }
 */
public class GEFExporter {

    public String export(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": \"").append(graph.getVersion()).append("\",\n");

        appendNodes(graph, json);
        appendEdges(graph, json);

        json.append("}\n");

        return json.toString();
    }

    private void appendNodes(CoreGraph graph, StringBuilder json) {
        json.append("  \"nodes\": [\n");

        List<CoreNode> nodes = new ArrayList<>(graph.getAllNodes());
        for (int i = 0; i < nodes.size(); i++) {
            appendNode(json, nodes.get(i), i < nodes.size() - 1);
        }

        json.append("  ],\n");
    }

    private void appendNode(StringBuilder json, CoreNode node, boolean addComma) {
        json.append("    ").append(nodeToJson(node));
        if (addComma) {
            json.append(",");
        }
        json.append("\n");
    }

    private void appendEdges(CoreGraph graph, StringBuilder json) {
        json.append("  \"edges\": [\n");

        List<CoreEdge> edges = new ArrayList<>(graph.getAllEdges());
        for (int i = 0; i < edges.size(); i++) {
            appendEdge(json, edges.get(i), i < edges.size() - 1);
        }

        json.append("  ]\n");
    }

    private void appendEdge(StringBuilder json, CoreEdge edge, boolean addComma) {
        json.append("    ").append(edgeToJson(edge));
        if (addComma) {
            json.append(",");
        }
        json.append("\n");
    }


    private String nodeToJson(CoreNode node) {
        return String.format(
                "{\"id\": \"%s\", \"label\": \"%s\", \"type\": \"%s\", \"zoomLevel\": %d, \"isPublic\": %s}",
                escapeJson(node.getId()),
                escapeJson(node.getName()),
                node.getType().name(),
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
                edge.getType().name(),
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

