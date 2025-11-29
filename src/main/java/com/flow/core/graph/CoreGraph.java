package com.flow.core.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the enriched flow graph containing nodes and edges.
 *
 * This is the central data structure that flows through the pipeline:
 * static graph load → validation → zoom assignment → flow extraction →
 * runtime merge → export
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

    /**
     * Add a node to the graph.
     *
     * @param node the CoreNode to add
     * @throws IllegalArgumentException if node ID already exists
     */
    public void addNode(CoreNode node) {
        Objects.requireNonNull(node, "Node cannot be null");
        if (nodes.containsKey(node.getId())) {
            throw new IllegalArgumentException("Node with ID " + node.getId() + " already exists");
        }
        nodes.put(node.getId(), node);
    }

    /**
     * Add an edge to the graph.
     *
     * @param edge the CoreEdge to add
     * @throws IllegalArgumentException if edge ID already exists or source/target nodes don't exist
     */
    public void addEdge(CoreEdge edge) {
        Objects.requireNonNull(edge, "Edge cannot be null");
        if (edges.containsKey(edge.getId())) {
            throw new IllegalArgumentException("Edge with ID " + edge.getId() + " already exists");
        }
        if (!nodes.containsKey(edge.getSourceId())) {
            throw new IllegalArgumentException("Source node " + edge.getSourceId() + " does not exist");
        }
        if (!nodes.containsKey(edge.getTargetId())) {
            throw new IllegalArgumentException("Target node " + edge.getTargetId() + " does not exist");
        }
        edges.put(edge.getId(), edge);
    }

    /**
     * Get a node by ID.
     *
     * @param nodeId the node ID
     * @return the CoreNode or null if not found
     */
    public CoreNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Get an edge by ID.
     *
     * @param edgeId the edge ID
     * @return the CoreEdge or null if not found
     */
    public CoreEdge getEdge(String edgeId) {
        return edges.get(edgeId);
    }

    /**
     * Get all nodes.
     *
     * @return unmodifiable collection of nodes
     */
    public Collection<CoreNode> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Get all edges.
     *
     * @return unmodifiable collection of edges
     */
    public Collection<CoreEdge> getAllEdges() {
        return Collections.unmodifiableCollection(edges.values());
    }

    /**
     * Get all outgoing edges from a node.
     *
     * @param nodeId the source node ID
     * @return list of edges originating from this node
     */
    public List<CoreEdge> getOutgoingEdges(String nodeId) {
        return edges.values().stream()
                .filter(e -> e.getSourceId().equals(nodeId))
                .toList();
    }

    /**
     * Get all incoming edges to a node.
     *
     * @param nodeId the target node ID
     * @return list of edges ending at this node
     */
    public List<CoreEdge> getIncomingEdges(String nodeId) {
        return edges.values().stream()
                .filter(e -> e.getTargetId().equals(nodeId))
                .toList();
    }

    /**
     * Get nodes by zoom level.
     *
     * @param zoomLevel the zoom level (1-5)
     * @return list of nodes at this zoom level
     */
    public List<CoreNode> getNodesByZoomLevel(int zoomLevel) {
        return nodes.values().stream()
                .filter(n -> n.getZoomLevel() == zoomLevel)
                .toList();
    }

    /**
     * Get the number of nodes in the graph.
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * Get the number of edges in the graph.
     */
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

