package com.flow.core.ingest;

import com.flow.core.graph.*;
import com.flow.core.runtime.EventType;
import com.flow.core.runtime.RuntimeEvent;

import java.util.*;

/**
 * Merges static graph with runtime execution data using a pipeline approach.
 *
 * Each merge stage is independent and can be tested/extended separately:
 * 1. RuntimeNodeStage - Adds runtime-discovered nodes
 * 2. RuntimeEdgeStage - Creates runtime call edges
 * 3. DurationStage - Calculates method durations
 * 4. CheckpointStage - Applies developer checkpoints
 * 5. AsyncHopStage - Stitches async message flows
 * 6. ErrorStage - Attaches error information
 *
 * RULES:
 * - Runtime nodes ALWAYS have zoom = 5
 * - Static nodes are NEVER overwritten
 * - Each stage is idempotent
 */
public class MergeEngine {

    private static final int RUNTIME_ZOOM_LEVEL = 5;

    private final RuntimeNodeStage runtimeNodeStage;
    private final RuntimeEdgeStage runtimeEdgeStage;
    private final DurationStage durationStage;
    private final CheckpointStage checkpointStage;
    private final AsyncHopStage asyncHopStage;
    private final ErrorStage errorStage;

    public MergeEngine() {
        this.runtimeNodeStage = new RuntimeNodeStage();
        this.runtimeEdgeStage = new RuntimeEdgeStage();
        this.durationStage = new DurationStage();
        this.checkpointStage = new CheckpointStage();
        this.asyncHopStage = new AsyncHopStage();
        this.errorStage = new ErrorStage();
    }

    /**
     * Main merge orchestration - runs all stages in sequence.
     */
    public CoreGraph mergeStaticAndRuntime(CoreGraph staticGraph, List<RuntimeEvent> events) {
        Objects.requireNonNull(staticGraph, "Static graph cannot be null");
        Objects.requireNonNull(events, "Runtime events cannot be null");

        MergeContext context = new MergeContext(staticGraph, events);

        runtimeNodeStage.execute(context);
        runtimeEdgeStage.execute(context);
        durationStage.execute(context);
        checkpointStage.execute(context);
        asyncHopStage.execute(context);
        errorStage.execute(context);

        return context.getGraph();
    }

    // Legacy method for backward compatibility
    public void merge(CoreGraph graph) {
        Objects.requireNonNull(graph, "Graph cannot be null");
    }

    // ============================================================
    // MERGE CONTEXT - Shared state across all stages
    // ============================================================

    private static class MergeContext {
        private final CoreGraph graph;
        private final List<RuntimeEvent> events;
        private final Map<String, RuntimeEvent> spanToEvent;

        MergeContext(CoreGraph graph, List<RuntimeEvent> events) {
            this.graph = graph;
            this.events = events;
            this.spanToEvent = buildSpanIndex(events);
        }

        private Map<String, RuntimeEvent> buildSpanIndex(List<RuntimeEvent> events) {
            Map<String, RuntimeEvent> index = new HashMap<>();
            for (RuntimeEvent event : events) {
                if (event.getSpanId() != null) {
                    index.put(event.getSpanId(), event);
                }
            }
            return index;
        }

        CoreGraph getGraph() {
            return graph;
        }

        List<RuntimeEvent> getEvents() {
            return events;
        }

        RuntimeEvent getEventBySpan(String spanId) {
            return spanToEvent.get(spanId);
        }
    }

    // ============================================================
    // STAGE 1: Runtime Node Creation
    // ============================================================

    private static class RuntimeNodeStage {
        void execute(MergeContext context) {
            Set<String> processedNodes = new HashSet<>();

            for (RuntimeEvent event : context.getEvents()) {
                if (shouldProcessEvent(event, context.getGraph(), processedNodes)) {
                    addRuntimeNode(event, context.getGraph());
                    processedNodes.add(event.getNodeId());
                }
            }
        }

        private boolean shouldProcessEvent(RuntimeEvent event, CoreGraph graph, Set<String> processed) {
            return !processed.contains(event.getNodeId()) &&
                   graph.getNode(event.getNodeId()) == null &&
                   isNodeCreationEvent(event.getType());
        }

        private boolean isNodeCreationEvent(EventType type) {
            return type == EventType.METHOD_ENTER ||
                   type == EventType.PRODUCE_TOPIC ||
                   type == EventType.CONSUME_TOPIC;
        }

        private void addRuntimeNode(RuntimeEvent event, CoreGraph graph) {
            NodeType nodeType = determineNodeType(event.getType());
            String name = "Runtime: " + event.getNodeId();

            CoreNode node = new CoreNode(
                event.getNodeId(),
                name,
                nodeType,
                null,
                Visibility.PUBLIC
            );
            node.setZoomLevel(RUNTIME_ZOOM_LEVEL);
            graph.addNode(node);
        }

        private NodeType determineNodeType(EventType eventType) {
            return switch (eventType) {
                case PRODUCE_TOPIC, CONSUME_TOPIC -> NodeType.TOPIC;
                default -> NodeType.METHOD;
            };
        }
    }

    // ============================================================
    // STAGE 2: Runtime Edge Creation
    // ============================================================

    private static class RuntimeEdgeStage {
        void execute(MergeContext context) {
            for (RuntimeEvent event : context.getEvents()) {
                processEvent(event, context);
            }
        }

        private void processEvent(RuntimeEvent event, MergeContext context) {
            if (event.getType() == EventType.METHOD_ENTER && event.getParentSpanId() != null) {
                createMethodCallEdge(event, context);
            } else if (event.getType() == EventType.PRODUCE_TOPIC) {
                createProducesEdge(event, context.getGraph());
            }
        }

        private void createMethodCallEdge(RuntimeEvent event, MergeContext context) {
            RuntimeEvent parent = context.getEventBySpan(event.getParentSpanId());
            if (parent != null) {
                addEdgeIfValid(
                    context.getGraph(),
                    "runtime:" + parent.getNodeId() + "->" + event.getNodeId(),
                    parent.getNodeId(),
                    event.getNodeId(),
                    EdgeType.RUNTIME_CALL
                );
            }
        }

        private void createProducesEdge(RuntimeEvent event, CoreGraph graph) {
            String topicId = (String) event.getDataValue("topicId");
            if (topicId != null) {
                addEdgeIfValid(
                    graph,
                    "runtime:produces:" + event.getNodeId() + "->" + topicId,
                    event.getNodeId(),
                    topicId,
                    EdgeType.PRODUCES
                );
            }
        }

        private void addEdgeIfValid(CoreGraph graph, String edgeId, String sourceId,
                                    String targetId, EdgeType type) {
            if (graph.getNode(sourceId) != null &&
                graph.getNode(targetId) != null &&
                graph.getEdge(edgeId) == null) {
                graph.addEdge(new CoreEdge(edgeId, sourceId, targetId, type));
            }
        }
    }

    // ============================================================
    // STAGE 3: Duration Calculation
    // ============================================================

    private static class DurationStage {
        void execute(MergeContext context) {
            Map<String, RuntimeEvent> enterEvents = collectEnterEvents(context.getEvents());

            for (RuntimeEvent event : context.getEvents()) {
                if (event.getType() == EventType.METHOD_EXIT) {
                    processDuration(event, enterEvents, context.getGraph());
                }
            }
        }

        private Map<String, RuntimeEvent> collectEnterEvents(List<RuntimeEvent> events) {
            Map<String, RuntimeEvent> enterEvents = new HashMap<>();
            for (RuntimeEvent event : events) {
                if (event.getType() == EventType.METHOD_ENTER && event.getSpanId() != null) {
                    enterEvents.put(event.getSpanId(), event);
                }
            }
            return enterEvents;
        }

        private void processDuration(RuntimeEvent exitEvent, Map<String, RuntimeEvent> enterEvents,
                                     CoreGraph graph) {
            RuntimeEvent enterEvent = enterEvents.get(exitEvent.getSpanId());
            if (enterEvent != null) {
                long duration = exitEvent.getTimestamp() - enterEvent.getTimestamp();
                CoreNode node = graph.getNode(exitEvent.getNodeId());
                if (node != null) {
                    node.setMetadata("durationMs", duration);
                }
            }
        }
    }

    // ============================================================
    // STAGE 4: Checkpoint Application
    // ============================================================

    private static class CheckpointStage {
        void execute(MergeContext context) {
            for (RuntimeEvent event : context.getEvents()) {
                if (event.getType() == EventType.CHECKPOINT) {
                    applyCheckpoint(event, context.getGraph());
                }
            }
        }

        private void applyCheckpoint(RuntimeEvent event, CoreGraph graph) {
            CoreNode node = graph.getNode(event.getNodeId());
            if (node != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> checkpoints = (Map<String, Object>) node.getMetadata("checkpoints");
                if (checkpoints == null) {
                    checkpoints = new HashMap<>();
                    node.setMetadata("checkpoints", checkpoints);
                }
                checkpoints.putAll(event.getData());
            }
        }
    }

    // ============================================================
    // STAGE 5: Async Hop Stitching
    // ============================================================

    private static class AsyncHopStage {
        void execute(MergeContext context) {
            AsyncHopIndex index = buildAsyncHopIndex(context.getEvents());
            stitchAsyncHops(index, context.getGraph());
        }

        private AsyncHopIndex buildAsyncHopIndex(List<RuntimeEvent> events) {
            AsyncHopIndex index = new AsyncHopIndex();
            for (RuntimeEvent event : events) {
                String topicId = (String) event.getDataValue("topicId");
                if (topicId != null) {
                    if (event.getType() == EventType.PRODUCE_TOPIC) {
                        index.addProduce(topicId, event);
                    } else if (event.getType() == EventType.CONSUME_TOPIC) {
                        index.addConsume(topicId, event);
                    }
                }
            }
            return index;
        }

        private void stitchAsyncHops(AsyncHopIndex index, CoreGraph graph) {
            for (String topicId : index.getTopics()) {
                List<RuntimeEvent> consumes = index.getConsumes(topicId);
                if (consumes != null) {
                    for (RuntimeEvent consume : consumes) {
                        createAsyncHopEdge(graph, topicId, consume.getNodeId());
                    }
                }
            }
        }

        private void createAsyncHopEdge(CoreGraph graph, String topicId, String consumerId) {
            String edgeId = "async:" + topicId + "->" + consumerId;
            if (graph.getNode(topicId) != null &&
                graph.getNode(consumerId) != null &&
                graph.getEdge(edgeId) == null) {
                graph.addEdge(new CoreEdge(edgeId, topicId, consumerId, EdgeType.CONSUMES));
            }
        }
    }

    private static class AsyncHopIndex {
        private final Map<String, List<RuntimeEvent>> producesByTopic = new HashMap<>();
        private final Map<String, List<RuntimeEvent>> consumesByTopic = new HashMap<>();

        void addProduce(String topicId, RuntimeEvent event) {
            producesByTopic.computeIfAbsent(topicId, k -> new ArrayList<>()).add(event);
        }

        void addConsume(String topicId, RuntimeEvent event) {
            consumesByTopic.computeIfAbsent(topicId, k -> new ArrayList<>()).add(event);
        }

        Set<String> getTopics() {
            return producesByTopic.keySet();
        }

        List<RuntimeEvent> getConsumes(String topicId) {
            return consumesByTopic.get(topicId);
        }
    }

    // ============================================================
    // STAGE 6: Error Attachment
    // ============================================================

    private static class ErrorStage {
        void execute(MergeContext context) {
            for (RuntimeEvent event : context.getEvents()) {
                if (event.getType() == EventType.ERROR) {
                    attachError(event, context.getGraph());
                }
            }
        }

        private void attachError(RuntimeEvent event, CoreGraph graph) {
            CoreNode node = graph.getNode(event.getNodeId());
            if (node != null) {
                node.setMetadata("error", event.getData());
                node.setMetadata("status", "FAILED");
            }
        }
    }
}


