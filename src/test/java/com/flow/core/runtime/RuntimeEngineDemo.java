package com.flow.core.runtime;

import com.flow.core.graph.*;

import java.util.*;

/**
 * Demonstrates the Runtime Engine functionality.
 */
public class RuntimeEngineDemo {

    public static void main(String[] args) {
        System.out.println("=== Flow Runtime Engine Demo ===\n");

        RuntimeEngine engine = new RuntimeEngine();
        CoreGraph staticGraph = createStaticGraph();

        String traceId = "demo-trace-001";
        List<RuntimeEvent> events = createSampleEvents(traceId);

        System.out.println("1. Ingesting " + events.size() + " runtime events...");
        engine.acceptEvents(events);

        System.out.println("2. Processing trace: " + traceId);
        RuntimeFlow flow = engine.processTrace(traceId, staticGraph);

        System.out.println("\n3. Runtime Flow Generated:");
        System.out.println("   Trace ID: " + flow.getTraceId());
        System.out.println("   Total Steps: " + flow.getStepCount());
        System.out.println("   Total Duration: " + flow.getTotalDurationMs() + "ms");
        System.out.println("   Has Errors: " + flow.hasErrors());

        System.out.println("\n4. Execution Steps:");
        for (FlowStep step : flow.getSteps()) {
            System.out.println("   → " + step.getNodeId() +
                             " (timestamp: " + step.getTimestamp() +
                             (step.getDurationMs() != null ? ", duration: " + step.getDurationMs() + "ms" : "") +
                             ")");
        }

        System.out.println("\n5. Available Traces:");
        Set<String> traceIds = engine.getAllTraceIds();
        System.out.println("   " + traceIds);

        System.out.println("\n✓ Runtime Engine Demo Complete!");
    }

    private static CoreGraph createStaticGraph() {
        CoreGraph graph = new CoreGraph("1");

        CoreNode endpoint = new CoreNode(
            "endpoint:POST /api/orders",
            "POST /api/orders",
            NodeType.ENDPOINT,
            null,
            Visibility.PUBLIC
        );
        endpoint.setZoomLevel(1);

        CoreNode controller = new CoreNode(
            "OrderController#createOrder",
            "createOrder",
            NodeType.METHOD,
            "OrderController",
            Visibility.PUBLIC
        );
        controller.setZoomLevel(3);

        CoreNode service = new CoreNode(
            "OrderService#placeOrder",
            "placeOrder",
            NodeType.METHOD,
            "OrderService",
            Visibility.PUBLIC
        );
        service.setZoomLevel(3);

        CoreNode topic = new CoreNode(
            "topic:orders.v1",
            "orders.v1",
            NodeType.TOPIC,
            null,
            Visibility.PUBLIC
        );
        topic.setZoomLevel(1);

        graph.addNode(endpoint);
        graph.addNode(controller);
        graph.addNode(service);
        graph.addNode(topic);

        graph.addEdge(new CoreEdge("e1", endpoint.getId(), controller.getId(), EdgeType.HANDLES));
        graph.addEdge(new CoreEdge("e2", controller.getId(), service.getId(), EdgeType.CALL));
        graph.addEdge(new CoreEdge("e3", service.getId(), topic.getId(), EdgeType.PRODUCES));

        return graph;
    }

    private static List<RuntimeEvent> createSampleEvents(String traceId) {
        long baseTime = System.currentTimeMillis();
        List<RuntimeEvent> events = new ArrayList<>();

        events.add(new RuntimeEvent(
            traceId, baseTime, EventType.METHOD_ENTER,
            "endpoint:POST /api/orders", "span-1", null, Map.of()
        ));

        events.add(new RuntimeEvent(
            traceId, baseTime + 10, EventType.METHOD_ENTER,
            "OrderController#createOrder", "span-2", "span-1", Map.of()
        ));

        events.add(new RuntimeEvent(
            traceId, baseTime + 20, EventType.METHOD_ENTER,
            "OrderService#placeOrder", "span-3", "span-2",
            Map.of("args", "cart123")
        ));

        events.add(new RuntimeEvent(
            traceId, baseTime + 25, EventType.CHECKPOINT,
            "OrderService#placeOrder", "span-3", "span-2",
            Map.of("cart_total", 250, "discount", "APPLIED20")
        ));

        events.add(new RuntimeEvent(
            traceId, baseTime + 50, EventType.METHOD_EXIT,
            "OrderService#placeOrder", "span-3", "span-2",
            Map.of("durationMs", 30)
        ));

        events.add(new RuntimeEvent(
            traceId, baseTime + 60, EventType.PRODUCE_TOPIC,
            "OrderService#placeOrder", "span-3", "span-2",
            Map.of("topicId", "topic:orders.v1")
        ));

        events.add(new RuntimeEvent(
            traceId, baseTime + 70, EventType.METHOD_EXIT,
            "OrderController#createOrder", "span-2", "span-1",
            Map.of("durationMs", 60)
        ));

        events.add(new RuntimeEvent(
            traceId, baseTime + 80, EventType.METHOD_EXIT,
            "endpoint:POST /api/orders", "span-1", null,
            Map.of("durationMs", 80)
        ));

        return events;
    }
}

