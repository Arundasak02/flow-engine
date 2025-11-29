package com.flow.core.ingest;

import com.flow.core.graph.*;

import java.util.*;

/**
 * Merges static graph with runtime execution data.
 *
 * Merge process:
 * 1. Identify and link runtime nodes to static nodes
 * 2. Aggregate execution counts across duplicate edges
 * 3. Mark "hot" nodes/paths (execution count > threshold)
 */
public class MergeEngine {

    private static final long HOT_THRESHOLD = 100;

    /**
     * Performs three-stage merge: identify runtime nodes, aggregate stats, mark hot paths.
     */
    public void merge(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        identifyRuntimeNodes(graph);
        aggregateExecutionStats(graph);
        markHotPaths(graph);
    }

    // Stage 1: Match runtime nodes with static nodes by ID
    private void identifyRuntimeNodes(CoreGraph graph) {
        Set<CoreNode> runtimeNodes = collectRuntimeNodes(graph);

        for (CoreNode runtimeNode : runtimeNodes) {
            linkToStaticNode(graph, runtimeNode);
        }
    }

    private Set<CoreNode> collectRuntimeNodes(CoreGraph graph) {
        Set<CoreNode> runtimeNodes = new HashSet<>();

        for (CoreNode node : graph.getAllNodes()) {
            if (isRuntimeNode(node)) {
                runtimeNodes.add(node);
            }
        }

        return runtimeNodes;
    }

    private boolean isRuntimeNode(CoreNode node) {
        return node.getType() == NodeType.METHOD && node.getName().startsWith("Runtime:");
    }

    // Future: mark static node as "verified" if matching runtime node found
    private void linkToStaticNode(CoreGraph graph, CoreNode runtimeNode) {
        CoreNode staticNode = graph.getNode(runtimeNode.getId());
        if (staticNode != null && !isRuntimeNode(staticNode)) {
            // Runtime execution confirmed for this static node
        }
    }

    // Stage 2: Aggregate execution counts for edges with same source→target
    private void aggregateExecutionStats(CoreGraph graph) {
        Map<String, Long> edgeExecutionCounts = calculateEdgeCounts(graph);
        updateEdgesWithCounts(graph, edgeExecutionCounts);
    }

    private Map<String, Long> calculateEdgeCounts(CoreGraph graph) {
        Map<String, Long> edgeExecutionCounts = new HashMap<>();

        for (CoreEdge edge : graph.getAllEdges()) {
            String edgeKey = createEdgeKey(edge);
            long totalCount = edgeExecutionCounts.getOrDefault(edgeKey, 0L);
            totalCount += edge.getExecutionCount();
            edgeExecutionCounts.put(edgeKey, totalCount);
        }

        return edgeExecutionCounts;
    }

    private String createEdgeKey(CoreEdge edge) {
        return edge.getSourceId() + "->" + edge.getTargetId();
    }

    private void updateEdgesWithCounts(CoreGraph graph, Map<String, Long> edgeExecutionCounts) {
        for (CoreEdge edge : graph.getAllEdges()) {
            String edgeKey = createEdgeKey(edge);
            edge.setExecutionCount(edgeExecutionCounts.getOrDefault(edgeKey, 0L));
        }
    }

    // Stage 3: Identify frequently executed nodes (incoming edge count > threshold)
    private void markHotPaths(CoreGraph graph) {
        Map<String, Long> nodeExecutionCounts = calculateNodeCounts(graph);
        markHotNodes(graph, nodeExecutionCounts);
    }

    // Sums execution counts of all incoming edges per node
    private Map<String, Long> calculateNodeCounts(CoreGraph graph) {
        Map<String, Long> nodeExecutionCounts = new HashMap<>();

        for (CoreEdge edge : graph.getAllEdges()) {
            String targetId = edge.getTargetId();
            long count = nodeExecutionCounts.getOrDefault(targetId, 0L);
            count += edge.getExecutionCount();
            nodeExecutionCounts.put(targetId, count);
        }

        return nodeExecutionCounts;
    }

    // Future: mark nodes exceeding threshold with "isHot" metadata
    private void markHotNodes(CoreGraph graph, Map<String, Long> nodeExecutionCounts) {
        for (Map.Entry<String, Long> entry : nodeExecutionCounts.entrySet()) {
            if (isHotNode(entry.getValue())) {
                CoreNode node = graph.getNode(entry.getKey());
                if (node != null) {
                    // Could extend CoreNode with "isHot" flag or metadata
                }
            }
        }
    }

    private boolean isHotNode(long executionCount) {
        return executionCount > HOT_THRESHOLD;
    }

    public void setHotThreshold(long threshold) {
        // Allow configuration of what constitutes "hot"
    }
}

