package com.flow.core.flow;

import com.flow.core.graph.CoreEdge;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;

/**
 * Extracts flows from the graph.
 *
 * A flow is a traversal path from a starting node (e.g., endpoint)
 * through the graph, representing the full call chain or data flow.
 *
 * Uses BFS (Breadth-First Search) by default to discover all nodes
 * reachable from each endpoint.
 *
 * Extracts one flow per business-level node (e.g., per endpoint).
 */
public class FlowExtractor {

    private static final int MAX_DEPTH = 100; // Prevent infinite loops

    /**
     * Extract flows from the graph.
     *
     * Creates a FlowModel for each business-level node (zoom level 1).
     *
     * @param graph the CoreGraph
     * @return list of extracted FlowModel objects
     */
    public List<FlowModel> extractFlows(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        List<FlowModel> flows = new ArrayList<>();

        // Find all business-level nodes (endpoints, topics)
        List<CoreNode> businessNodes = graph.getNodesByZoomLevel(1);

        for (CoreNode businessNode : businessNodes) {
            FlowModel flow = extractFlow(graph, businessNode);
            flows.add(flow);
        }

        return flows;
    }

    /**
     * Extract a single flow starting from a given node.
     *
     * @param graph the CoreGraph
     * @param startNode the starting node for the flow
     * @return the extracted FlowModel
     */
    public FlowModel extractFlow(CoreGraph graph, CoreNode startNode) {
        Objects.requireNonNull(graph, "Graph cannot be null");
        Objects.requireNonNull(startNode, "Start node cannot be null");

        FlowModel flowModel = new FlowModel(
                "flow_" + startNode.getId(),
                startNode.getId(),
                startNode.getName()
        );

        // Add start node as first step
        FlowStep startStep = new FlowStep(
                startNode.getId(),
                startNode.getName(),
                startNode.getZoomLevel(),
                0,
                new ArrayList<>()
        );
        flowModel.addStep(startStep);

        // BFS traversal
        Set<String> visited = new HashSet<>();
        Queue<TraversalNode> queue = new LinkedList<>();
        queue.offer(new TraversalNode(startNode.getId(), 1, new ArrayList<>()));

        while (!queue.isEmpty()) {
            TraversalNode current = queue.poll();

            if (visited.contains(current.nodeId)) {
                continue;
            }
            visited.add(current.nodeId);

            if (current.depth > MAX_DEPTH) {
                break; // Prevent infinite traversal
            }

            CoreNode node = graph.getNode(current.nodeId);
            if (node == null) {
                continue;
            }

            // Get outgoing edges
            List<CoreEdge> outgoingEdges = graph.getOutgoingEdges(current.nodeId);

            for (CoreEdge edge : outgoingEdges) {
                String targetId = edge.getTargetId();

                if (!visited.contains(targetId)) {
                    CoreNode targetNode = graph.getNode(targetId);
                    if (targetNode != null) {
                        // Add step to flow
                        List<String> previousSteps = new ArrayList<>(current.path);
                        previousSteps.add(current.nodeId);

                        FlowStep step = new FlowStep(
                                targetNode.getId(),
                                targetNode.getName(),
                                targetNode.getZoomLevel(),
                                current.depth,
                                previousSteps
                        );
                        flowModel.addStep(step);

                        // Queue for further exploration
                        List<String> newPath = new ArrayList<>(current.path);
                        newPath.add(current.nodeId);
                        queue.offer(new TraversalNode(targetId, current.depth + 1, newPath));
                    }
                }
            }
        }

        return flowModel;
    }

    /**
     * Internal class for BFS traversal state.
     */
    private static class TraversalNode {
        final String nodeId;
        final int depth;
        final List<String> path; // path from start to this node

        TraversalNode(String nodeId, int depth, List<String> path) {
            this.nodeId = nodeId;
            this.depth = depth;
            this.path = path;
        }
    }
}

