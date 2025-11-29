package com.flow.core.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.core.graph.CoreEdge;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;

/**
 * Loads a static graph from flow.json JSON string.
 *
 * Supports unified node/edge array format:
 * {
 *   "graphId": "project-name",
 *   "nodes": [
 *     {
 *       "id": "unique-id",
 *       "type": "METHOD|ENDPOINT|TOPIC|CLASS|SERVICE|PRIVATE_METHOD",
 *       "name": "display-name",
 *       "data": { metadata }
 *     }
 *   ],
 *   "edges": [
 *     {
 *       "id": "edge-id",
 *       "from": "source-node-id",
 *       "to": "target-node-id",
 *       "type": "CALL|HANDLES|PRODUCES|CONSUMES|BELONGS_TO|DEFINES"
 *     }
 *   ]
 * }
 */
public class StaticGraphLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Load a CoreGraph from JSON string in GEF 1.1 format.
     *
     * @param jsonString the flow.json content
     * @return the loaded CoreGraph
     * @throws IllegalArgumentException if JSON format is invalid or parsing fails
     */
    public CoreGraph load(String jsonString) {
        Objects.requireNonNull(jsonString, "JSON string cannot be null");

        if (jsonString.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be empty");
        }

        try {
            Map<String, Object> json = parseJson(jsonString);
            return buildGraph(json);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load graph from JSON: " + e.getMessage(), e);
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

        // Load nodes from unified nodes array
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) json.get("nodes");
        if (nodes != null) {
            loadNodesFromArray(graph, nodes);
        }

        // Load edges from unified edges array
        List<Map<String, Object>> edges = (List<Map<String, Object>>) json.get("edges");
        if (edges != null) {
            loadEdgesFromArray(graph, edges);
        }

        return graph;
    }

    @SuppressWarnings("unchecked")
    private void loadNodesFromArray(CoreGraph graph, List<Map<String, Object>> nodes) {
        for (Map<String, Object> nodeData : nodes) {
            String id = (String) nodeData.get("id");
            String type = (String) nodeData.get("type");
            String name = (String) nodeData.get("name");
            Map<String, Object> data = (Map<String, Object>) nodeData.get("data");

            if (id != null && type != null && name != null) {
                boolean isPublic = isNodePublic(type, data);
                String className = data != null ? (String) data.get("className") : null;

                CoreNode node = new CoreNode(id, name, type, className, isPublic);
                graph.addNode(node);
            }
        }
    }

    private boolean isNodePublic(String type, Map<String, Object> data) {
        if (data == null) {
            return true;  // default to public if no data
        }

        String visibility = (String) data.get("visibility");
        if (visibility != null) {
            return "public".equals(visibility);
        }

        // Default: ENDPOINT, TOPIC, SERVICE, CLASS, METHOD are public
        // PRIVATE_METHOD is private
        return !"PRIVATE_METHOD".equals(type);
    }

    private void loadEdgesFromArray(CoreGraph graph, List<Map<String, Object>> edges) {
        for (Map<String, Object> edgeData : edges) {
            String id = (String) edgeData.get("id");
            String from = (String) edgeData.get("from");
            String to = (String) edgeData.get("to");
            String type = (String) edgeData.get("type");

            if (id != null && from != null && to != null && type != null) {
                // Only add edge if both nodes exist
                if (graph.getNode(from) != null && graph.getNode(to) != null) {
                    CoreEdge edge = new CoreEdge(id, from, to, type);
                    graph.addEdge(edge);
                }
            }
        }
    }
}

