package com.flow.core.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central graph data structure holding nodes and edges.
 * Thread-safe via ConcurrentHashMap.
 * Flows through pipeline: load → validate → zoom → flow extract → merge → export
 */
public class CoreGraph {

    private final Map<String, CoreNode> nodes;
    private final Map<String, CoreEdge> edges;
    private final String version;
    private final long createdAt;

    public CoreGraph(String version) {
        this.version = Objects.requireNonNull(version, "Graph version cannot be null");
        this.nodes = new ConcurrentHashMap<>();
        this.edges = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public void addNode(CoreNode node) {
        Objects.requireNonNull(node, "Node cannot be null");
        validateNodeNotExists(node);
        nodes.put(node.getId(), node);
    }

    private void validateNodeNotExists(CoreNode node) {
        if (nodes.containsKey(node.getId())) {
            throw new IllegalArgumentException("Node with ID " + node.getId() + " already exists");
        }
    }

    public void addEdge(CoreEdge edge) {
        Objects.requireNonNull(edge, "Edge cannot be null");
        validateEdgeNotExists(edge);
        validateEdgeNodes(edge);
        edges.put(edge.getId(), edge);
    }

    private void validateEdgeNotExists(CoreEdge edge) {
        if (edges.containsKey(edge.getId())) {
            throw new IllegalArgumentException("Edge with ID " + edge.getId() + " already exists");
        }
    }

    // Validates both source and target nodes exist before adding edge
    private void validateEdgeNodes(CoreEdge edge) {
        if (!nodes.containsKey(edge.getSourceId())) {
            throw new IllegalArgumentException("Source node " + edge.getSourceId() + " does not exist");
        }
        if (!nodes.containsKey(edge.getTargetId())) {
            throw new IllegalArgumentException("Target node " + edge.getTargetId() + " does not exist");
        }
    }

    public CoreNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public CoreEdge getEdge(String edgeId) {
        return edges.get(edgeId);
    }

    public Collection<CoreNode> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Collection<CoreEdge> getAllEdges() {
        return Collections.unmodifiableCollection(edges.values());
    }

    public List<CoreEdge> getOutgoingEdges(String nodeId) {
        return edges.values().stream()
                .filter(e -> e.getSourceId().equals(nodeId))
                .toList();
    }

    public List<CoreEdge> getIncomingEdges(String nodeId) {
        return edges.values().stream()
                .filter(e -> e.getTargetId().equals(nodeId))
                .toList();
    }

    public List<CoreNode> getNodesByZoomLevel(int zoomLevel) {
        return nodes.values().stream()
                .filter(n -> n.getZoomLevel() == zoomLevel)
                .toList();
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        return edges.size();
    }

    public String getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "CoreGraph{" +
                "version='" + version + '\'' +
                ", nodes=" + nodes.size() +
                ", edges=" + edges.size() +
                ", createdAt=" + createdAt +
                '}';
    }
}

