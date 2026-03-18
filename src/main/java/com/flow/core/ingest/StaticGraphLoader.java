package com.flow.core.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.core.graph.*;

import java.util.*;

/**
 * Loads static graph from flow.json format.
 *
 * Expected JSON structure:
 * {
 *   "version": "1",
 *   "nodes": [{"id", "type", "name", "data": {...}}],
 *   "edges": [{"id", "from", "to", "type"}]
 * }
 *
 * Node visibility: determined by "visibility" field in data, or inferred from type
 * (PRIVATE_METHOD → private, others → public by default)
 */
public class StaticGraphLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public CoreGraph load(String jsonString) {
        Objects.requireNonNull(jsonString, "JSON string cannot be null");
        validateNotEmpty(jsonString);

        try {
            Map<String, Object> json = parseJson(jsonString);
            return buildGraph(json);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load graph from JSON: " + e.getMessage(), e);
        }
    }

    private void validateNotEmpty(String jsonString) {
        if (jsonString.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be empty");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String jsonString) throws Exception {
        return MAPPER.readValue(jsonString, Map.class);
    }

    @SuppressWarnings("unchecked")
    private CoreGraph buildGraph(Map<String, Object> json) {
        String version = (String) json.getOrDefault("version", "1");
        CoreGraph graph = new CoreGraph(version);

        loadNodes(graph, (List<Map<String, Object>>) json.get("nodes"));
        loadEdges(graph, (List<Map<String, Object>>) json.get("edges"));

        return graph;
    }

    private void loadNodes(CoreGraph graph, List<Map<String, Object>> nodes) {
        if (nodes != null) {
            nodes.forEach(nodeData -> loadNode(graph, nodeData));
        }
    }

    @SuppressWarnings("unchecked")
    private void loadNode(CoreGraph graph, Map<String, Object> nodeData) {
        String id = (String) nodeData.get("id");
        String typeStr = (String) nodeData.get("type");
        String name = (String) nodeData.get("name");
        Map<String, Object> data = (Map<String, Object>) nodeData.get("data");

        if (isValidNode(id, typeStr, name)) {
            NodeType type = NodeType.fromString(typeStr);
            CoreNode node = createNode(id, type, name, data);
            graph.addNode(node);
        }
    }

    private boolean isValidNode(String id, String type, String name) {
        return id != null && type != null && name != null;
    }

    private CoreNode createNode(String id, NodeType type, String name, Map<String, Object> data) {
        Visibility visibility = determineVisibility(type, data);
        String className = extractClassName(data);
        return new CoreNode(id, name, type, className, visibility);
    }

    // Visibility: explicit "visibility" field takes precedence, otherwise inferred from type
    private Visibility determineVisibility(NodeType type, Map<String, Object> data) {
        if (data == null) {
            return Visibility.PUBLIC;
        }

        String visibilityStr = (String) data.get("visibility");
        if (visibilityStr != null) {
            return Visibility.fromString(visibilityStr);
        }

        return type.isPublicByDefault() ? Visibility.PUBLIC : Visibility.PRIVATE;
    }

    private String extractClassName(Map<String, Object> data) {
        return data != null ? (String) data.get("className") : null;
    }

    private void loadEdges(CoreGraph graph, List<Map<String, Object>> edges) {
        if (edges != null) {
            edges.forEach(edgeData -> loadEdge(graph, edgeData));
        }
    }

    private void loadEdge(CoreGraph graph, Map<String, Object> edgeData) {
        String id = (String) edgeData.get("id");
        String from = (String) edgeData.get("from");
        String to = (String) edgeData.get("to");
        String typeStr = (String) edgeData.get("type");

        if (isValidEdge(id, from, to, typeStr) && bothNodesExist(graph, from, to)) {
            EdgeType type = EdgeType.fromString(typeStr);
            CoreEdge edge = new CoreEdge(id, from, to, type);
            graph.addEdge(edge);
        }
    }

    private boolean isValidEdge(String id, String from, String to, String type) {
        return id != null && from != null && to != null && type != null;
    }

    private boolean bothNodesExist(CoreGraph graph, String from, String to) {
        return graph.getNode(from) != null && graph.getNode(to) != null;
    }
}

