package com.flow.core.runtime;

import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts runtime flows from events and graph.
 * Produces ordered execution paths for UI animation.
 */
public class RuntimeFlowExtractor {

    public RuntimeFlow extractByTraceId(CoreGraph graph, List<RuntimeEvent> events) {
        Objects.requireNonNull(graph, "Graph cannot be null");
        Objects.requireNonNull(events, "Events cannot be null");

        if (events.isEmpty()) {
            return new RuntimeFlow("unknown", Collections.emptyList());
        }

        String traceId = events.get(0).getTraceId();
        List<FlowStep> steps = generateOrderedSteps(graph, events);

        return new RuntimeFlow(traceId, steps);
    }

    private List<FlowStep> generateOrderedSteps(CoreGraph graph, List<RuntimeEvent> events) {
        List<RuntimeEvent> sortedEvents = sortEventsByTimestamp(events);
        Map<String, RuntimeEvent> enterEvents = collectEnterEvents(sortedEvents);
        Map<String, Map<String, Object>> checkpointsByNode = collectCheckpoints(sortedEvents);
        Map<String, Map<String, Object>> errorsByNode = collectErrors(sortedEvents);

        List<FlowStep> steps = new ArrayList<>();

        for (RuntimeEvent event : sortedEvents) {
            if (shouldCreateStep(event)) {
                FlowStep step = createStep(event, graph, enterEvents, checkpointsByNode, errorsByNode);
                steps.add(step);
            }
        }

        return steps;
    }

    private List<RuntimeEvent> sortEventsByTimestamp(List<RuntimeEvent> events) {
        return events.stream()
                .sorted(Comparator.comparingLong(RuntimeEvent::getTimestamp))
                .collect(Collectors.toList());
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

    private Map<String, Map<String, Object>> collectCheckpoints(List<RuntimeEvent> events) {
        Map<String, Map<String, Object>> checkpointsByNode = new HashMap<>();
        for (RuntimeEvent event : events) {
            if (event.getType() == EventType.CHECKPOINT) {
                checkpointsByNode.computeIfAbsent(event.getNodeId(), k -> new HashMap<>())
                        .putAll(event.getData());
            }
        }
        return checkpointsByNode;
    }

    private Map<String, Map<String, Object>> collectErrors(List<RuntimeEvent> events) {
        Map<String, Map<String, Object>> errorsByNode = new HashMap<>();
        for (RuntimeEvent event : events) {
            if (event.getType() == EventType.ERROR) {
                errorsByNode.put(event.getNodeId(), event.getData());
            }
        }
        return errorsByNode;
    }

    private boolean shouldCreateStep(RuntimeEvent event) {
        return event.getType() == EventType.METHOD_ENTER ||
               event.getType() == EventType.PRODUCE_TOPIC ||
               event.getType() == EventType.CONSUME_TOPIC;
    }

    private FlowStep createStep(RuntimeEvent event, CoreGraph graph,
                                Map<String, RuntimeEvent> enterEvents,
                                Map<String, Map<String, Object>> checkpointsByNode,
                                Map<String, Map<String, Object>> errorsByNode) {

        FlowStep.Builder builder = FlowStep.builder(event.getNodeId(), event.getTimestamp())
                .spanId(event.getSpanId())
                .parentSpanId(event.getParentSpanId());

        Long duration = calculateDuration(event, enterEvents);
        if (duration != null) {
            builder.durationMs(duration);
        }

        Map<String, Object> checkpoints = checkpointsByNode.get(event.getNodeId());
        if (checkpoints != null && !checkpoints.isEmpty()) {
            builder.checkpoints(checkpoints);
        }

        Map<String, Object> error = errorsByNode.get(event.getNodeId());
        if (error != null) {
            builder.error(error);
        }

        return builder.build();
    }

    private Long calculateDuration(RuntimeEvent event, Map<String, RuntimeEvent> enterEvents) {
        if (event.getType() == EventType.METHOD_EXIT && event.getSpanId() != null) {
            RuntimeEvent enter = enterEvents.get(event.getSpanId());
            if (enter != null) {
                return event.getTimestamp() - enter.getTimestamp();
            }
        }

        Object durationObj = event.getDataValue("durationMs");
        if (durationObj instanceof Number) {
            return ((Number) durationObj).longValue();
        }

        return null;
    }
}

