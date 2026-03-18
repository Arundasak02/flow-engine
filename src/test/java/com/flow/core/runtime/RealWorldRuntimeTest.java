package com.flow.core.runtime;

import com.flow.core.graph.CoreEdge;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;
import com.flow.core.graph.EdgeType;
import com.flow.core.graph.NodeType;
import com.flow.core.graph.Visibility;
import com.flow.core.ingest.MergeEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit-based integration-style test for runtime merge behaviour.
 * The prior main()-based demo was renamed out of the surefire pattern so builds don't depend on external files.
 */
class RealWorldRuntimeTest {

    @Test
    void mergePipelineAcceptsRealisticOrderFlowEvents() {
        CoreGraph graph = createStaticGraph();
        List<RuntimeEvent> events = createSuccessfulOrderEvents("trace-order-001");

        MergeEngine mergeEngine = new MergeEngine();
        CoreGraph merged = mergeEngine.mergeStaticAndRuntime(graph, events);

        assertNotNull(merged);
        // Basic sanity: durations/checkpoints/errors may attach metadata; at minimum graph remains valid.
        assertTrue(merged.getNodeCount() >= graph.getNodeCount());
        assertTrue(merged.getEdgeCount() >= graph.getEdgeCount());
    }

    private static CoreGraph createStaticGraph() {
        CoreGraph graph = new CoreGraph("1");

        CoreNode endpoint = new CoreNode("endpoint:POST /api/orders/{id}", "POST /api/orders/{id}",
                NodeType.ENDPOINT, null, Visibility.PUBLIC);
        endpoint.setZoomLevel(1);
        graph.addNode(endpoint);

        CoreNode orderServiceClass = new CoreNode("com.greens.order.core.OrderService", "OrderService",
                NodeType.CLASS, "service:order", Visibility.PUBLIC);
        orderServiceClass.setZoomLevel(2);
        graph.addNode(orderServiceClass);

        CoreNode placeOrder = new CoreNode("com.greens.order.core.OrderService#placeOrder(String):String",
                "placeOrder", NodeType.METHOD, "com.greens.order.core.OrderService", Visibility.PUBLIC);
        placeOrder.setZoomLevel(3);
        graph.addNode(placeOrder);

        CoreNode validateCart = new CoreNode("com.greens.order.core.OrderService#validateCart(String):void",
                "validateCart", NodeType.PRIVATE_METHOD, "com.greens.order.core.OrderService", Visibility.PRIVATE);
        validateCart.setZoomLevel(4);
        graph.addNode(validateCart);

        CoreNode topic = new CoreNode("topic:orders.v1", "orders.v1",
                NodeType.TOPIC, null, Visibility.PUBLIC);
        topic.setZoomLevel(1);
        graph.addNode(topic);

        graph.addEdge(new CoreEdge("e1", endpoint.getId(), placeOrder.getId(), EdgeType.CALL));
        graph.addEdge(new CoreEdge("e2", placeOrder.getId(), topic.getId(), EdgeType.PRODUCES));

        return graph;
    }

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
}

