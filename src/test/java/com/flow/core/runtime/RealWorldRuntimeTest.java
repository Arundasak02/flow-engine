package com.flow.core.runtime;

import com.flow.core.FlowCoreEngine;
import com.flow.core.export.Neo4jExporter;
import com.flow.core.graph.*;
import com.flow.core.ingest.MergeEngine;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Real-world integration test simulating an e-commerce order flow.
 *
 * Scenario: POST /api/orders → OrderController → OrderService → Kafka → OrderConsumer
 *
 * Features tested:
 * - Static graph loading from flow.json
 * - Runtime event ingestion
 * - Method enter/exit with durations
 * - Checkpoints (cart validation, payment)
 * - Async hops (Kafka topic)
 * - Distributed tracing across services
 * - Error handling
 */
public class RealWorldRuntimeTest {

    public static void main(String[] args) {
        System.out.println("════════════════════════════════════════════════");
        System.out.println("  REAL-WORLD RUNTIME ENGINE TEST");
        System.out.println("  E-Commerce Order Flow Simulation");
        System.out.println("════════════════════════════════════════════════\n");

        try {
            // 1. Load static graph from flow.json
            System.out.println("1. Loading static graph from flow.json...");
            CoreGraph staticGraph = loadStaticGraph();
            System.out.println("   ✓ Loaded " + staticGraph.getNodeCount() + " nodes, " +
                             staticGraph.getEdgeCount() + " edges\n");

            // 2. Initialize Runtime Engine
            System.out.println("2. Initializing Runtime Engine...");
            RuntimeEngine runtimeEngine = new RuntimeEngine();
            System.out.println("   ✓ Runtime Engine ready\n");

            // 3. Simulate successful order flow
            System.out.println("3. Simulating SUCCESSFUL order flow...");
            String traceId1 = "trace-order-001";
            List<RuntimeEvent> successEvents = createSuccessfulOrderEvents(traceId1);
            runtimeEngine.acceptEvents(successEvents);
            System.out.println("   ✓ Ingested " + successEvents.size() + " events\n");

            // 4. Process successful trace
            System.out.println("4. Processing successful trace...");
            RuntimeFlow successFlow = runtimeEngine.processTrace(traceId1, staticGraph);
            printFlowSummary(successFlow);
            printDetailedSteps(successFlow);

            // 5. Simulate failed order flow (payment declined)
            System.out.println("\n5. Simulating FAILED order flow (payment declined)...");
            String traceId2 = "trace-order-002";
            List<RuntimeEvent> failedEvents = createFailedOrderEvents(traceId2);
            runtimeEngine.acceptEvents(failedEvents);
            System.out.println("   ✓ Ingested " + failedEvents.size() + " events\n");

            // 6. Process failed trace
            System.out.println("6. Processing failed trace...");
            RuntimeFlow failedFlow = runtimeEngine.processTrace(traceId2, staticGraph);
            printFlowSummary(failedFlow);
            printDetailedSteps(failedFlow);

            // 7. Simulate async consumer processing
            System.out.println("\n7. Simulating async Kafka consumer...");
            String traceId3 = "trace-order-003";
            List<RuntimeEvent> asyncEvents = createAsyncConsumerEvents(traceId3);
            runtimeEngine.acceptEvents(asyncEvents);
            System.out.println("   ✓ Ingested " + asyncEvents.size() + " events\n");

            // 8. Process async trace
            System.out.println("8. Processing async trace...");
            RuntimeFlow asyncFlow = runtimeEngine.processTrace(traceId3, staticGraph);
            printFlowSummary(asyncFlow);
            printDetailedSteps(asyncFlow);

            // 9. Show all active traces
            System.out.println("\n9. Active traces in memory:");
            Set<String> allTraces = runtimeEngine.getAllTraceIds();
            allTraces.forEach(tid -> System.out.println("   - " + tid));

            // 10. Export enriched graph to Neo4j
            System.out.println("\n10. Exporting enriched graph to Neo4j format...");
            CoreGraph enrichedGraph = createEnrichedGraph(staticGraph, successEvents, runtimeEngine);
            String neo4jExport = exportToNeo4j(enrichedGraph);
            System.out.println("   ✓ Generated " + countLines(neo4jExport) + " Cypher statements");
            System.out.println("\n   Sample Neo4j output:");
            printSampleLines(neo4jExport, 5);

            // 11. Save Neo4j export to file
            System.out.println("\n11. Saving Neo4j export to file...");
            Files.writeString(Paths.get("target/runtime-graph.cypher"), neo4jExport);
            System.out.println("   ✓ Saved to target/runtime-graph.cypher");

            // 12. Cleanup
            System.out.println("\n12. Cleaning up...");
            runtimeEngine.expireOldTraces();
            System.out.println("   ✓ Old traces expired\n");

            System.out.println("════════════════════════════════════════════════");
            System.out.println("  ✓✓✓ ALL TESTS PASSED ✓✓✓");
            System.out.println("  Runtime graph exported to Neo4j!");
            System.out.println("════════════════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("\n✗ Test failed:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static CoreGraph loadStaticGraph() throws Exception {
        String flowJson = Files.readString(Paths.get("flow.json"));
        FlowCoreEngine engine = new FlowCoreEngine();
        return engine.process(flowJson);
    }

    /**
     * Successful order flow:
     * POST /api/orders → OrderController → OrderService → PaymentService → Kafka
     */
    private static List<RuntimeEvent> createSuccessfulOrderEvents(String traceId) {
        long t0 = System.currentTimeMillis();
        List<RuntimeEvent> events = new ArrayList<>();

        // Endpoint entry
        events.add(new RuntimeEvent(
            traceId, t0, EventType.METHOD_ENTER,
            "endpoint:POST /api/orders/{id}",
            "span-1", null,
            Map.of("method", "POST", "path", "/api/orders/123")
        ));

        // Controller method
        events.add(new RuntimeEvent(
            traceId, t0 + 5, EventType.METHOD_ENTER,
            "com.greens.order.api.OrderController#createOrder(String):String",
            "span-2", "span-1",
            Map.of("args", "[orderId=123]")
        ));

        // Service method
        events.add(new RuntimeEvent(
            traceId, t0 + 10, EventType.METHOD_ENTER,
            "com.greens.order.core.OrderService#placeOrder(String):String",
            "span-3", "span-2",
            Map.of("args", "[orderId=123]")
        ));

        // Checkpoint: Cart validation
        events.add(new RuntimeEvent(
            traceId, t0 + 15, EventType.CHECKPOINT,
            "com.greens.order.core.OrderService#placeOrder(String):String",
            "span-3", "span-2",
            Map.of(
                "checkpoint", "cart_validated",
                "cart_total", 250.50,
                "item_count", 3,
                "customer_id", "CUST-9876"
            )
        ));

        // Private method: validateCart
        events.add(new RuntimeEvent(
            traceId, t0 + 20, EventType.METHOD_ENTER,
            "com.greens.order.core.OrderService#validateCart(String):void",
            "span-4", "span-3",
            Map.of("cartId", "cart-123")
        ));

        events.add(new RuntimeEvent(
            traceId, t0 + 28, EventType.METHOD_EXIT,
            "com.greens.order.core.OrderService#validateCart(String):void",
            "span-4", "span-3",
            Map.of("durationMs", 8)
        ));

        // Checkpoint: Payment processing
        events.add(new RuntimeEvent(
            traceId, t0 + 30, EventType.CHECKPOINT,
            "com.greens.order.core.OrderService#placeOrder(String):String",
            "span-3", "span-2",
            Map.of(
                "checkpoint", "payment_processing",
                "payment_method", "VISA_4532",
                "amount", 250.50
            )
        ));

        // Payment service call
        events.add(new RuntimeEvent(
            traceId, t0 + 35, EventType.METHOD_ENTER,
            "com.greens.payment.PaymentService#processPayment(String,double):boolean",
            "span-5", "span-3",
            Map.of("orderId", "123", "amount", 250.50)
        ));

        events.add(new RuntimeEvent(
            traceId, t0 + 85, EventType.METHOD_EXIT,
            "com.greens.payment.PaymentService#processPayment(String,double):boolean",
            "span-5", "span-3",
            Map.of("durationMs", 50, "success", true)
        ));

        // Checkpoint: Payment success
        events.add(new RuntimeEvent(
            traceId, t0 + 90, EventType.CHECKPOINT,
            "com.greens.order.core.OrderService#placeOrder(String):String",
            "span-3", "span-2",
            Map.of(
                "checkpoint", "payment_success",
                "transaction_id", "TXN-789456",
                "status", "CONFIRMED"
            )
        ));

        // Produce to Kafka
        events.add(new RuntimeEvent(
            traceId, t0 + 95, EventType.PRODUCE_TOPIC,
            "com.greens.order.core.OrderService#placeOrder(String):String",
            "span-3", "span-2",
            Map.of(
                "topicId", "topic:orders.v1",
                "messageKey", "order-123",
                "partition", 2
            )
        ));

        // Service method exit
        events.add(new RuntimeEvent(
            traceId, t0 + 100, EventType.METHOD_EXIT,
            "com.greens.order.core.OrderService#placeOrder(String):String",
            "span-3", "span-2",
            Map.of("durationMs", 90, "result", "order-123")
        ));

        // Controller exit
        events.add(new RuntimeEvent(
            traceId, t0 + 105, EventType.METHOD_EXIT,
            "com.greens.order.api.OrderController#createOrder(String):String",
            "span-2", "span-1",
            Map.of("durationMs", 100)
        ));

        // Endpoint exit
        events.add(new RuntimeEvent(
            traceId, t0 + 110, EventType.METHOD_EXIT,
            "endpoint:POST /api/orders/{id}",
            "span-1", null,
            Map.of("durationMs", 110, "status", 200)
        ));

        return events;
    }

    /**
     * Failed order flow: Payment declined
     */
    private static List<RuntimeEvent> createFailedOrderEvents(String traceId) {
        long t0 = System.currentTimeMillis();
        List<RuntimeEvent> events = new ArrayList<>();

        // Endpoint entry
        events.add(new RuntimeEvent(
            traceId, t0, EventType.METHOD_ENTER,
            "endpoint:POST /api/orders/{id}",
            "span-1", null,
            Map.of("method", "POST", "path", "/api/orders/456")
        ));

        // Controller
        events.add(new RuntimeEvent(
            traceId, t0 + 5, EventType.METHOD_ENTER,
            "com.greens.order.api.OrderController#createOrder(String):String",
            "span-2", "span-1",
            Map.of("args", "[orderId=456]")
        ));

        // Service
        events.add(new RuntimeEvent(
            traceId, t0 + 10, EventType.METHOD_ENTER,
            "com.greens.order.core.OrderService#placeOrder(String):String",
            "span-3", "span-2",
            Map.of("args", "[orderId=456]")
        ));

        // Cart validation checkpoint
        events.add(new RuntimeEvent(
            traceId, t0 + 15, EventType.CHECKPOINT,
            "com.greens.order.core.OrderService#placeOrder(String):String",
            "span-3", "span-2",
            Map.of(
                "checkpoint", "cart_validated",
                "cart_total", 1500.00,
                "item_count", 5
            )
        ));

        // Payment service call
        events.add(new RuntimeEvent(
            traceId, t0 + 30, EventType.METHOD_ENTER,
            "com.greens.payment.PaymentService#processPayment(String,double):boolean",
            "span-4", "span-3",
            Map.of("orderId", "456", "amount", 1500.00)
        ));

        // Payment declined - ERROR EVENT
        events.add(new RuntimeEvent(
            traceId, t0 + 80, EventType.ERROR,
            "com.greens.payment.PaymentService#processPayment(String,double):boolean",
            "span-4", "span-3",
            Map.of(
                "error_type", "PaymentDeclinedException",
                "error_message", "Insufficient funds",
                "error_code", "PAYMENT_DECLINED",
                "card_last4", "4532"
            )
        ));

        // Payment method exit with error
        events.add(new RuntimeEvent(
            traceId, t0 + 85, EventType.METHOD_EXIT,
            "com.greens.payment.PaymentService#processPayment(String,double):boolean",
            "span-4", "span-3",
            Map.of("durationMs", 55, "success", false)
        ));

        // Service propagates error
        events.add(new RuntimeEvent(
            traceId, t0 + 90, EventType.ERROR,
            "com.greens.order.core.OrderService#placeOrder(String):String",
            "span-3", "span-2",
            Map.of(
                "error_type", "OrderCreationException",
                "error_message", "Payment declined: Insufficient funds",
                "status", "FAILED"
            )
        ));

        // Service exit
        Map<String, Object> serviceExitData = new HashMap<>();
        serviceExitData.put("durationMs", 85);
        serviceExitData.put("result", null);
        events.add(new RuntimeEvent(
            traceId, t0 + 95, EventType.METHOD_EXIT,
            "com.greens.order.core.OrderService#placeOrder(String):String",
            "span-3", "span-2",
            serviceExitData
        ));

        // Controller exit
        events.add(new RuntimeEvent(
            traceId, t0 + 100, EventType.METHOD_EXIT,
            "com.greens.order.api.OrderController#createOrder(String):String",
            "span-2", "span-1",
            Map.of("durationMs", 95)
        ));

        // Endpoint exit with 400
        events.add(new RuntimeEvent(
            traceId, t0 + 105, EventType.METHOD_EXIT,
            "endpoint:POST /api/orders/{id}",
            "span-1", null,
            Map.of("durationMs", 105, "status", 400)
        ));

        return events;
    }

    /**
     * Async consumer processing: Kafka message → Consumer → Email service
     */
    private static List<RuntimeEvent> createAsyncConsumerEvents(String traceId) {
        long t0 = System.currentTimeMillis();
        List<RuntimeEvent> events = new ArrayList<>();

        // Kafka consume event
        events.add(new RuntimeEvent(
            traceId, t0, EventType.CONSUME_TOPIC,
            "topic:orders.v1",
            "span-1", null,
            Map.of(
                "topicId", "topic:orders.v1",
                "partition", 2,
                "offset", 12345,
                "messageKey", "order-123"
            )
        ));

        // Consumer method entry
        events.add(new RuntimeEvent(
            traceId, t0 + 5, EventType.METHOD_ENTER,
            "com.greens.notification.OrderConsumer#onOrderCreated(String):void",
            "span-2", "span-1",
            Map.of("orderId", "order-123")
        ));

        // Checkpoint: Processing order notification
        events.add(new RuntimeEvent(
            traceId, t0 + 10, EventType.CHECKPOINT,
            "com.greens.notification.OrderConsumer#onOrderCreated(String):void",
            "span-2", "span-1",
            Map.of(
                "checkpoint", "notification_processing",
                "order_id", "order-123",
                "customer_email", "customer@example.com"
            )
        ));

        // Email service call
        events.add(new RuntimeEvent(
            traceId, t0 + 15, EventType.METHOD_ENTER,
            "com.greens.notification.EmailService#sendOrderConfirmation(String):void",
            "span-3", "span-2",
            Map.of("orderId", "order-123")
        ));

        events.add(new RuntimeEvent(
            traceId, t0 + 65, EventType.METHOD_EXIT,
            "com.greens.notification.EmailService#sendOrderConfirmation(String):void",
            "span-3", "span-2",
            Map.of("durationMs", 50, "email_sent", true)
        ));

        // Checkpoint: Email sent
        events.add(new RuntimeEvent(
            traceId, t0 + 70, EventType.CHECKPOINT,
            "com.greens.notification.OrderConsumer#onOrderCreated(String):void",
            "span-2", "span-1",
            Map.of(
                "checkpoint", "email_sent",
                "status", "SUCCESS"
            )
        ));

        // Consumer exit
        events.add(new RuntimeEvent(
            traceId, t0 + 75, EventType.METHOD_EXIT,
            "com.greens.notification.OrderConsumer#onOrderCreated(String):void",
            "span-2", "span-1",
            Map.of("durationMs", 70)
        ));

        return events;
    }

    private static void printFlowSummary(RuntimeFlow flow) {
        System.out.println("   ┌─────────────────────────────────────────┐");
        System.out.println("   │ Flow Summary                            │");
        System.out.println("   ├─────────────────────────────────────────┤");
        System.out.println("   │ Trace ID:       " + flow.getTraceId());
        System.out.println("   │ Total Steps:    " + flow.getStepCount());
        System.out.println("   │ Total Duration: " + flow.getTotalDurationMs() + " ms");
        System.out.println("   │ Has Errors:     " + (flow.hasErrors() ? "YES ❌" : "NO ✓"));
        System.out.println("   └─────────────────────────────────────────┘");
    }

    private static void printDetailedSteps(RuntimeFlow flow) {
        System.out.println("\n   Execution Timeline:");
        System.out.println("   ─────────────────────────────────────────────────────");

        int stepNum = 1;
        for (FlowStep step : flow.getSteps()) {
            String duration = step.getDurationMs() != null ?
                String.format(" [%dms]", step.getDurationMs()) : "";

            String error = step.hasError() ? " ❌ ERROR" : "";

            String checkpoints = "";
            if (!step.getCheckpoints().isEmpty()) {
                checkpoints = " 📍 " + step.getCheckpoints().size() + " checkpoint(s)";
            }

            System.out.printf("   %2d. %s%s%s%s%n",
                stepNum++,
                shortenNodeId(step.getNodeId()),
                duration,
                checkpoints,
                error
            );

            // Show checkpoint details
            if (!step.getCheckpoints().isEmpty()) {
                step.getCheckpoints().forEach((key, value) ->
                    System.out.println("       └─ " + key + ": " + value)
                );
            }

            // Show error details
            if (step.hasError()) {
                Map<String, Object> errorData = step.getError();
                System.out.println("       └─ Error: " + errorData.get("error_message"));
            }
        }
        System.out.println("   ─────────────────────────────────────────────────────");
    }

    private static String shortenNodeId(String nodeId) {
        if (nodeId.startsWith("com.greens.")) {
            return nodeId.substring("com.greens.".length());
        }
        return nodeId;
    }

    /**
     * Create enriched graph by merging static graph with runtime events
     */
    private static CoreGraph createEnrichedGraph(CoreGraph staticGraph,
                                                  List<RuntimeEvent> events,
                                                  RuntimeEngine engine) {
        // Create a copy of static graph for enrichment
        CoreGraph enrichedGraph = copyGraph(staticGraph);

        // Merge runtime events into the graph
        MergeEngine mergeEngine = engine.getMergeEngine();
        mergeEngine.mergeStaticAndRuntime(enrichedGraph, events);

        return enrichedGraph;
    }

    private static CoreGraph copyGraph(CoreGraph original) {
        CoreGraph copy = new CoreGraph(original.getVersion());

        // Copy all nodes
        for (CoreNode node : original.getAllNodes()) {
            CoreNode nodeCopy = new CoreNode(
                node.getId(),
                node.getName(),
                node.getType(),
                node.getServiceId(),
                node.getVisibility()
            );
            nodeCopy.setZoomLevel(node.getZoomLevel());
            copy.addNode(nodeCopy);
        }

        // Copy all edges
        for (CoreEdge edge : original.getAllEdges()) {
            CoreEdge edgeCopy = new CoreEdge(
                edge.getId(),
                edge.getSourceId(),
                edge.getTargetId(),
                edge.getType()
            );
            edgeCopy.setExecutionCount(edge.getExecutionCount());
            copy.addEdge(edgeCopy);
        }

        return copy;
    }

    /**
     * Export graph to Neo4j Cypher format
     */
    private static String exportToNeo4j(CoreGraph graph) {
        Neo4jExporter exporter = new Neo4jExporter();
        return exporter.export(graph);
    }


    /**
     * Count lines in a string
     */
    private static int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) text.lines().count();
    }

    /**
     * Print first N lines of text with line numbers
     */
    private static void printSampleLines(String text, int maxLines) {
        text.lines()
            .limit(maxLines)
            .forEach(line -> System.out.println("   " + line));

        long totalLines = text.lines().count();
        if (totalLines > maxLines) {
            System.out.println("   ... (" + (totalLines - maxLines) + " more lines)");
        }
    }
}

