package com.flow.core.export;

import com.flow.core.graph.CoreEdge;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Exports a CoreGraph to Neo4j Cypher format.
 *
 * Generates CREATE and MERGE statements for Neo4j import.
 * Exports:
 * - Nodes with properties (name, type, zoomLevel, isPublic)
 * - Edges/relationships with execution counts
 *
 * Example output:
 * CREATE (n1:Node {id: "ep1", name: "GET /api/users", type: "ENDPOINT", zoomLevel: 1})
 * CREATE (n2:Node {id: "svc1", name: "UserService", type: "SERVICE", zoomLevel: 2})
 * CREATE (n1)-[:CALLS {executionCount: 42}]->(n2)
 */
public class Neo4jExporter {

    /**
     * Export the graph to Neo4j Cypher format.
     *
     * @param graph the CoreGraph to export
     * @return Cypher query string ready for Neo4j import
     */
    public String export(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        StringBuilder sb = new StringBuilder();

        // Export nodes
        exportNodes(graph, sb);
        sb.append("\n");

        // Export edges
        exportEdges(graph, sb);

        return sb.toString();
    }

    private void exportNodes(CoreGraph graph, StringBuilder sb) {
        for (CoreNode node : graph.getAllNodes()) {
            String cypher = String.format(
                    "CREATE (n%s:Node {id: \"%s\", name: \"%s\", type: \"%s\", zoomLevel: %d, isPublic: %s});%n",
                    escapeId(node.getId()),
                    escapeString(node.getId()),
                    escapeString(node.getName()),
                    node.getType(),
                    node.getZoomLevel(),
                    node.isPublic()
            );
            sb.append(cypher);
        }
    }

    private void exportEdges(CoreGraph graph, StringBuilder sb) {
        for (CoreEdge edge : graph.getAllEdges()) {
            String cypher = String.format(
                    "CREATE (n%s)-[:%s {executionCount: %d}]->(n%s);%n",
                    escapeId(edge.getSourceId()),
                    edge.getType(),
                    edge.getExecutionCount(),
                    escapeId(edge.getTargetId())
            );
            sb.append(cypher);
        }
    }

    private String escapeId(String id) {
        // Simple escaping for node identifiers
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String escapeString(String str) {
        // Escape quotes in strings
        return str.replace("\"", "\\\"");
    }
}

