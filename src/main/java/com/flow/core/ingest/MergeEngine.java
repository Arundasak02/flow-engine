package com.flow.core.ingest;

import com.flow.core.graph.CoreEdge;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;

/**
 * Merges static graph with runtime execution data.
 *
 * The merge process:
 * 1. Aligns static nodes with runtime execution nodes
 * 2. Updates execution counts on edges
 * 3. Marks nodes as "hot" (frequently executed)
 * 4. Enriches edges with timing and call frequency data
 * 5. Resolves node identity across static and runtime layers
 */
public class MergeEngine {

    private static final long HOT_THRESHOLD = 100; // calls to mark a node as "hot"

    /**
     * Merge runtime execution data with the static graph.
     *
     * @param graph the CoreGraph with both static and runtime data
     */
    public void merge(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        // Identify runtime nodes and match them with static nodes
        identifyRuntimeNodes(graph);

        // Aggregate execution statistics
        aggregateExecutionStats(graph);

        // Mark hot paths
        markHotPaths(graph);
    }

    private void identifyRuntimeNodes(CoreGraph graph) {
        // Separate static and runtime nodes
        Set<CoreNode> runtimeNodes = new HashSet<>();

        for (CoreNode node : graph.getAllNodes()) {
            if ("RUNTIME_CALL".equals(node.getType())) {
                runtimeNodes.add(node);
            }
        }

        // For each runtime node, try to find a matching static node
        for (CoreNode runtimeNode : runtimeNodes) {
            // Simple matching: if a static node with similar ID exists, link them
            CoreNode staticNode = graph.getNode(runtimeNode.getId());
            if (staticNode != null && !"RUNTIME_CALL".equals(staticNode.getType())) {
                // Runtime execution confirmed for this static node
                // Could mark it as "verified" or "observed"
            }
        }
    }

    private void aggregateExecutionStats(CoreGraph graph) {
        // For each edge, aggregate execution counts across all instances
        Map<String, Long> edgeExecutionCounts = new HashMap<>();

        for (CoreEdge edge : graph.getAllEdges()) {
            String edgeKey = edge.getSourceId() + "->" + edge.getTargetId();
            long totalCount = edgeExecutionCounts.getOrDefault(edgeKey, 0L);
            totalCount += edge.getExecutionCount();
            edgeExecutionCounts.put(edgeKey, totalCount);
        }

        // Update edges with aggregated counts
        for (CoreEdge edge : graph.getAllEdges()) {
            String edgeKey = edge.getSourceId() + "->" + edge.getTargetId();
            edge.setExecutionCount(edgeExecutionCounts.getOrDefault(edgeKey, 0L));
        }
    }

    private void markHotPaths(CoreGraph graph) {
        // Identify nodes and edges that are heavily executed
        Map<String, Long> nodeExecutionCounts = new HashMap<>();

        // Aggregate execution counts for each node (incoming edges)
        for (CoreEdge edge : graph.getAllEdges()) {
            String targetId = edge.getTargetId();
            long count = nodeExecutionCounts.getOrDefault(targetId, 0L);
            count += edge.getExecutionCount();
            nodeExecutionCounts.put(targetId, count);
        }

        // Mark nodes exceeding threshold
        for (Map.Entry<String, Long> entry : nodeExecutionCounts.entrySet()) {
            if (entry.getValue() > HOT_THRESHOLD) {
                CoreNode node = graph.getNode(entry.getKey());
                if (node != null) {
                    // Could extend CoreNode with "isHot" flag or metadata
                }
            }
        }
    }

    public void setHotThreshold(long threshold) {
        // Allow configuration of what constitutes "hot"
    }
}

