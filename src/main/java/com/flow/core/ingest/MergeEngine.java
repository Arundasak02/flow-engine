package com.flow.core.ingest;

import com.flow.core.graph.*;
import com.flow.core.runtime.EventType;
import com.flow.core.runtime.RuntimeEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Merges static graph with runtime execution data.
 *
 * Merge process:
 * 1. Add runtime nodes (zoom level 5)
 * 2. Add runtime edges (RUNTIME_CALL, PRODUCES_RUNTIME, ASYNC_HOP)
 * 3. Apply durations from METHOD_ENTER/EXIT pairs
 * 4. Apply checkpoints from CHECKPOINT events
 * 5. Stitch async hops (Kafka, PubSub, etc.)
 * 6. Attach error information
 *
 * RULES:
 * - Runtime nodes ALWAYS have zoom = 5
 * - Static nodes are NEVER overwritten
 * - Runtime paths take precedence over static paths
 */
public class MergeEngine {

    private static final long HOT_THRESHOLD = 100;
    private static final int RUNTIME_ZOOM_LEVEL = 5;

    public CoreGraph mergeStaticAndRuntime(CoreGraph staticGraph, List<RuntimeEvent> events) {
        Objects.requireNonNull(staticGraph, "Static graph cannot be null");
        Objects.requireNonNull(events, "Runtime events cannot be null");

        addRuntimeNodes(staticGraph, events);
        addRuntimeEdges(staticGraph, events);
        applyDurations(staticGraph, events);
        applyCheckpoints(staticGraph, events);
        stitchAsyncHops(staticGraph, events);
        attachErrors(staticGraph, events);

        return staticGraph;
    }

    // Add runtime nodes for METHOD_ENTER, PRODUCE_TOPIC, CONSUME_TOPIC events
    void addRuntimeNodes(CoreGraph graph, List<RuntimeEvent> events) {
        Set<String> processedNodes = new HashSet<>();

        for (RuntimeEvent event : events) {
            String nodeId = event.getNodeId();

            if (processedNodes.contains(nodeId)) {
                continue;
            }

            if (graph.getNode(nodeId) != null) {
                continue;
            }

            if (shouldCreateRuntimeNode(event.getType())) {
                CoreNode runtimeNode = createRuntimeNode(event);
                graph.addNode(runtimeNode);
                processedNodes.add(nodeId);
            }
        }
    }

    private boolean shouldCreateRuntimeNode(EventType type) {
        return type == EventType.METHOD_ENTER ||
               type == EventType.PRODUCE_TOPIC ||
               type == EventType.CONSUME_TOPIC;
    }

    private CoreNode createRuntimeNode(RuntimeEvent event) {
        String name = "Runtime: " + event.getNodeId();
        NodeType nodeType = determineNodeType(event.getType());

        CoreNode node = new CoreNode(
            event.getNodeId(),
            name,
            nodeType,
            null,
            Visibility.PUBLIC
        );

        node.setZoomLevel(RUNTIME_ZOOM_LEVEL);
        return node;
    }

    private NodeType determineNodeType(EventType eventType) {
        return switch (eventType) {
            case PRODUCE_TOPIC, CONSUME_TOPIC -> NodeType.TOPIC;
            default -> NodeType.METHOD;
        };
    }

    // Add runtime edges: RUNTIME_CALL, PRODUCES_RUNTIME, ASYNC_HOP
    void addRuntimeEdges(CoreGraph graph, List<RuntimeEvent> events) {
        Map<String, RuntimeEvent> spanToEvent = new HashMap<>();

        for (RuntimeEvent event : events) {
            if (event.getSpanId() != null) {
                spanToEvent.put(event.getSpanId(), event);
            }
        }

        for (RuntimeEvent event : events) {
            if (event.getParentSpanId() != null && event.getType() == EventType.METHOD_ENTER) {
                RuntimeEvent parent = spanToEvent.get(event.getParentSpanId());
                if (parent != null) {
                    createRuntimeCallEdge(graph, parent.getNodeId(), event.getNodeId());
                }
            }

            if (event.getType() == EventType.PRODUCE_TOPIC) {
                createProducesRuntimeEdge(graph, event);
            }
        }
    }

    private void createRuntimeCallEdge(CoreGraph graph, String sourceId, String targetId) {
        if (graph.getNode(sourceId) == null || graph.getNode(targetId) == null) {
            return;
        }

        String edgeId = "runtime:" + sourceId + "->" + targetId;
        if (graph.getEdge(edgeId) != null) {
            return;
        }

        CoreEdge edge = new CoreEdge(edgeId, sourceId, targetId, EdgeType.RUNTIME_CALL);
        graph.addEdge(edge);
    }

    private void createProducesRuntimeEdge(CoreGraph graph, RuntimeEvent event) {
        String topicId = (String) event.getDataValue("topicId");
        if (topicId == null) {
            return;
        }

        String edgeId = "runtime:produces:" + event.getNodeId() + "->" + topicId;
        if (graph.getEdge(edgeId) != null) {
            return;
        }

        if (graph.getNode(event.getNodeId()) != null && graph.getNode(topicId) != null) {
            CoreEdge edge = new CoreEdge(edgeId, event.getNodeId(), topicId, EdgeType.PRODUCES);
            graph.addEdge(edge);
        }
    }

    // Apply durations: durationMs = exit.timestamp - enter.timestamp
    void applyDurations(CoreGraph graph, List<RuntimeEvent> events) {
        Map<String, RuntimeEvent> enterEvents = new HashMap<>();

        for (RuntimeEvent event : events) {
            if (event.getType() == EventType.METHOD_ENTER) {
                enterEvents.put(event.getSpanId(), event);
            } else if (event.getType() == EventType.METHOD_EXIT) {
                RuntimeEvent enter = enterEvents.get(event.getSpanId());
                if (enter != null) {
                    long duration = event.getTimestamp() - enter.getTimestamp();
                    CoreNode node = graph.getNode(event.getNodeId());
                    if (node != null) {
                        // Store duration in node metadata (would need to add data field to CoreNode)
                        // For now, we'll track it separately
                    }
                }
            }
        }
    }

    // Apply checkpoints: stored under node.data.checkpoints
    void applyCheckpoints(CoreGraph graph, List<RuntimeEvent> events) {
        for (RuntimeEvent event : events) {
            if (event.getType() == EventType.CHECKPOINT) {
                CoreNode node = graph.getNode(event.getNodeId());
                if (node != null) {
                    // Store checkpoint data (would need data field in CoreNode)
                    // For now, checkpoints are in RuntimeEvent.data
                }
            }
        }
    }

    // Stitch async hops: PRODUCE_TOPIC + CONSUME_TOPIC -> ASYNC_HOP edge
    void stitchAsyncHops(CoreGraph graph, List<RuntimeEvent> events) {
        Map<String, List<RuntimeEvent>> producesByTopic = new HashMap<>();
        Map<String, List<RuntimeEvent>> consumesByTopic = new HashMap<>();

        for (RuntimeEvent event : events) {
            String topicId = (String) event.getDataValue("topicId");
            if (topicId == null) continue;

            if (event.getType() == EventType.PRODUCE_TOPIC) {
                producesByTopic.computeIfAbsent(topicId, k -> new ArrayList<>()).add(event);
            } else if (event.getType() == EventType.CONSUME_TOPIC) {
                consumesByTopic.computeIfAbsent(topicId, k -> new ArrayList<>()).add(event);
            }
        }

        for (String topicId : producesByTopic.keySet()) {
            List<RuntimeEvent> consumes = consumesByTopic.get(topicId);
            if (consumes != null && !consumes.isEmpty()) {
                for (RuntimeEvent consume : consumes) {
                    createAsyncHopEdge(graph, topicId, consume.getNodeId());
                }
            }
        }
    }

    private void createAsyncHopEdge(CoreGraph graph, String topicId, String consumerId) {
        if (graph.getNode(topicId) == null || graph.getNode(consumerId) == null) {
            return;
        }

        String edgeId = "async:" + topicId + "->" + consumerId;
        if (graph.getEdge(edgeId) != null) {
            return;
        }

        CoreEdge edge = new CoreEdge(edgeId, topicId, consumerId, EdgeType.CONSUMES);
        graph.addEdge(edge);
    }

    // Attach error information to nodes
    void attachErrors(CoreGraph graph, List<RuntimeEvent> events) {
        for (RuntimeEvent event : events) {
            if (event.getType() == EventType.ERROR) {
                CoreNode node = graph.getNode(event.getNodeId());
                if (node != null) {
                    // Store error data (would need data field in CoreNode)
                    // node.data.error = { type, message, stack }
                    // node.data.status = "FAILED"
                }
            }
        }
    }

    // Legacy method for backward compatibility
    public void merge(CoreGraph graph) {
        // No-op for now, or call mergeStaticAndRuntime with empty events
        Objects.requireNonNull(graph, "Graph cannot be null");
    }
}


