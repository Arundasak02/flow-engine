package com.flow.core.flow;

import com.flow.core.graph.CoreEdge;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;

/**
 * Extracts flows from graph using BFS traversal.
 * Creates one FlowModel per business-level node (zoom level 1).
 */
public class FlowExtractor {

    private static final int MAX_DEPTH = 100;

    /**
     * Extracts flows from all business-level nodes (endpoints, topics).
     */
    public List<FlowModel> extractFlows(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");

        List<FlowModel> flows = new ArrayList<>();
        List<CoreNode> businessNodes = graph.getNodesByZoomLevel(1);

        for (CoreNode businessNode : businessNodes) {
            flows.add(extractFlow(graph, businessNode));
        }

        return flows;
    }

    /**
     * Extracts a single flow via BFS from the given start node.
     */
    public FlowModel extractFlow(CoreGraph graph, CoreNode startNode) {
        Objects.requireNonNull(graph, "Graph cannot be null");
        Objects.requireNonNull(startNode, "Start node cannot be null");

        FlowModel flowModel = createFlowModel(startNode);
        addStartStep(flowModel, startNode);
        traverseGraph(graph, flowModel, startNode);

        return flowModel;
    }

    private FlowModel createFlowModel(CoreNode startNode) {
        return new FlowModel(
                "flow_" + startNode.getId(),
                startNode.getId(),
                startNode.getName()
        );
    }

    private void addStartStep(FlowModel flowModel, CoreNode startNode) {
        FlowStep startStep = new FlowStep(
                startNode.getId(),
                startNode.getName(),
                startNode.getZoomLevel(),
                0,
                new ArrayList<>()
        );
        flowModel.addStep(startStep);
    }

    // BFS traversal: discovers all reachable nodes from start, respects MAX_DEPTH
    private void traverseGraph(CoreGraph graph, FlowModel flowModel, CoreNode startNode) {
        Set<String> visited = new HashSet<>();
        Queue<TraversalNode> queue = new LinkedList<>();
        queue.offer(new TraversalNode(startNode.getId(), 1, new ArrayList<>()));

        while (!queue.isEmpty()) {
            TraversalNode current = queue.poll();

            if (shouldSkipNode(visited, current)) {
                continue;
            }

            visited.add(current.nodeId);
            processNode(graph, flowModel, current, queue, visited);
        }
    }

    private boolean shouldSkipNode(Set<String> visited, TraversalNode node) {
        return visited.contains(node.nodeId) || node.depth > MAX_DEPTH;
    }

    private void processNode(CoreGraph graph, FlowModel flowModel, TraversalNode current,
                             Queue<TraversalNode> queue, Set<String> visited) {
        CoreNode node = graph.getNode(current.nodeId);
        if (node == null) {
            return;
        }

        List<CoreEdge> outgoingEdges = graph.getOutgoingEdges(current.nodeId);
        for (CoreEdge edge : outgoingEdges) {
            processEdge(graph, flowModel, current, edge, queue, visited);
        }
    }

    private void processEdge(CoreGraph graph, FlowModel flowModel, TraversalNode current,
                            CoreEdge edge, Queue<TraversalNode> queue, Set<String> visited) {
        String targetId = edge.getTargetId();

        if (visited.contains(targetId)) {
            return;
        }

        CoreNode targetNode = graph.getNode(targetId);
        if (targetNode == null) {
            return;
        }

        addFlowStep(flowModel, targetNode, current);
        enqueueNextNode(queue, targetId, current);
    }

    private void addFlowStep(FlowModel flowModel, CoreNode targetNode, TraversalNode current) {
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
    }

    private void enqueueNextNode(Queue<TraversalNode> queue, String targetId, TraversalNode current) {
        List<String> newPath = new ArrayList<>(current.path);
        newPath.add(current.nodeId);
        queue.offer(new TraversalNode(targetId, current.depth + 1, newPath));
    }

    private static class TraversalNode {
        final String nodeId;
        final int depth;
        final List<String> path;

        TraversalNode(String nodeId, int depth, List<String> path) {
            this.nodeId = nodeId;
            this.depth = depth;
            this.path = path;
        }
    }
}

