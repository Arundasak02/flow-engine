package com.flow.core.export;

import com.flow.core.graph.CoreEdge;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Exports graph to Neo4j Cypher CREATE statements.
 *
 * Output format:
 * CREATE (n<id>:Node {id, name, type, zoomLevel, isPublic});
 * CREATE (n<source>)-[:TYPE {executionCount}]->(n<target>);
 */
public class Neo4jExporter {

    public String export(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        StringBuilder sb = new StringBuilder();
        exportNodes(graph, sb);
        sb.append("\n");
        exportEdges(graph, sb);

        return sb.toString();
    }

    private void exportNodes(CoreGraph graph, StringBuilder sb) {
        for (CoreNode node : graph.getAllNodes()) {
            String cypher = buildNodeCypher(node);
            sb.append(cypher);
        }
    }

    private String buildNodeCypher(CoreNode node) {
        return String.format(
                "CREATE (n%s:Node {id: \"%s\", name: \"%s\", type: \"%s\", zoomLevel: %d, isPublic: %s});%n",
                escapeId(node.getId()),
                escapeString(node.getId()),
                escapeString(node.getName()),
                node.getType().name(),
                node.getZoomLevel(),
                node.isPublic()
        );
    }

    private void exportEdges(CoreGraph graph, StringBuilder sb) {
        for (CoreEdge edge : graph.getAllEdges()) {
            String cypher = buildEdgeCypher(edge);
            sb.append(cypher);
        }
    }

    private String buildEdgeCypher(CoreEdge edge) {
        return String.format(
                "CREATE (n%s)-[:%s {executionCount: %d}]->(n%s);%n",
                escapeId(edge.getSourceId()),
                edge.getType().name(),
                edge.getExecutionCount(),
                escapeId(edge.getTargetId())
        );
    }

    private String escapeId(String id) {
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String escapeString(String str) {
        return str.replace("\"", "\\\"");
    }
}

