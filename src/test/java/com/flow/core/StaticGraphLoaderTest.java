package com.flow.core;

import com.flow.core.graph.*;

import java.nio.file.Files;
import java.nio.file.Paths;

public class StaticGraphLoaderTest {

    public static void main(String[] args) {
        System.out.println("=== Static Graph Loader Test (flow.json) ===\n");

        try {
            String flowJson = loadFlowJson();

            FlowCoreEngine engine = new FlowCoreEngine();
            CoreGraph graph = engine.process(flowJson);

            System.out.println("✓ Graph loaded successfully");
            printGraphStats(graph);
            validateGraphStructure(graph);
            validateZoomLevels(graph);
            validateNodeTypes(graph);
            validateEdgeTypes(graph);
            testExports(engine, graph);

            System.out.println("\n✓✓✓ All tests passed! ✓✓✓");

        } catch (Exception e) {
            System.out.println("\n✗ Test failed:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String loadFlowJson() throws Exception {
        System.out.println("Loading flow.json...");
        String path = "flow.json";
        return Files.readString(Paths.get(path));
    }

    private static void printGraphStats(CoreGraph graph) {
        System.out.println("\nGraph Statistics:");
        System.out.println("  - Total Nodes: " + graph.getNodeCount());
        System.out.println("  - Total Edges: " + graph.getEdgeCount());
        System.out.println("  - Version: " + graph.getVersion());

        System.out.println("\nNodes by Type:");
        printNodeCountByType(graph, NodeType.ENDPOINT);
        printNodeCountByType(graph, NodeType.TOPIC);
        printNodeCountByType(graph, NodeType.SERVICE);
        printNodeCountByType(graph, NodeType.CLASS);
        printNodeCountByType(graph, NodeType.METHOD);
        printNodeCountByType(graph, NodeType.PRIVATE_METHOD);

        System.out.println("\nNodes by Zoom Level:");
        for (int level = 1; level <= 5; level++) {
            int count = graph.getNodesByZoomLevel(level).size();
            System.out.println("  - Level " + level + ": " + count + " nodes");
        }
    }

    private static void printNodeCountByType(CoreGraph graph, NodeType type) {
        long count = graph.getAllNodes().stream()
                .filter(n -> type == n.getType())
                .count();
        System.out.println("  - " + type + ": " + count);
    }

    private static void validateGraphStructure(CoreGraph graph) {
        System.out.println("\n✓ Validating graph structure...");

        if (graph.getNodeCount() != 20) {
            throw new AssertionError("Expected 20 nodes, found " + graph.getNodeCount());
        }

        // Note: StaticGraphLoader only adds edges where both nodes exist
        // One edge in JSON references non-existent nodes and is skipped
        if (graph.getEdgeCount() != 26) {
            throw new AssertionError("Expected 26 edges, found " + graph.getEdgeCount());
        }

        System.out.println("  ✓ Node and edge counts correct");
    }

    private static void validateZoomLevels(CoreGraph graph) {
        System.out.println("\n✓ Validating zoom level assignments...");

        for (CoreNode node : graph.getAllNodes()) {
            int zoom = node.getZoomLevel();

            if (zoom < 1 || zoom > 5) {
                throw new AssertionError("Invalid zoom level " + zoom + " for node " + node.getId());
            }

            switch (node.getType()) {
                case ENDPOINT:
                case TOPIC:
                    if (zoom != 1) {
                        throw new AssertionError("ENDPOINT/TOPIC should be zoom level 1, got " + zoom);
                    }
                    break;
                case SERVICE:
                case CLASS:
                    if (zoom != 2) {
                        throw new AssertionError("SERVICE/CLASS should be zoom level 2, got " + zoom);
                    }
                    break;
                case METHOD:
                    if (node.isPublic() && zoom != 3) {
                        throw new AssertionError("Public METHOD should be zoom level 3, got " + zoom);
                    }
                    break;
                case PRIVATE_METHOD:
                    if (zoom != 4) {
                        throw new AssertionError("PRIVATE_METHOD should be zoom level 4, got " + zoom);
                    }
                    break;
            }
        }

        System.out.println("  ✓ All zoom levels correctly assigned");
    }

    private static void validateNodeTypes(CoreGraph graph) {
        System.out.println("\n✓ Validating specific nodes...");

        CoreNode endpoint = graph.getNode("endpoint:POST /api/orders/{id}");
        if (endpoint == null) {
            throw new AssertionError("POST endpoint not found");
        }
        if (endpoint.getType() != NodeType.ENDPOINT) {
            throw new AssertionError("Endpoint has wrong type: " + endpoint.getType());
        }
        System.out.println("  ✓ Endpoint node validated");

        CoreNode topic = graph.getNode("topic:orders.v1");
        if (topic == null) {
            throw new AssertionError("Kafka topic not found");
        }
        if (topic.getType() != NodeType.TOPIC) {
            throw new AssertionError("Topic has wrong type: " + topic.getType());
        }
        System.out.println("  ✓ Topic node validated");

        CoreNode service = graph.getNode("service:order");
        if (service == null) {
            throw new AssertionError("Service node not found");
        }
        if (service.getType() != NodeType.SERVICE) {
            throw new AssertionError("Service has wrong type: " + service.getType());
        }
        System.out.println("  ✓ Service node validated");

        CoreNode orderService = graph.getNode("com.greens.order.core.OrderService");
        if (orderService == null) {
            throw new AssertionError("OrderService class not found");
        }
        if (orderService.getType() != NodeType.CLASS) {
            throw new AssertionError("Class has wrong type: " + orderService.getType());
        }
        System.out.println("  ✓ Class node validated");

        CoreNode publicMethod = graph.getNode("com.greens.order.core.OrderService#placeOrder(String):String");
        if (publicMethod == null) {
            throw new AssertionError("placeOrder method not found");
        }
        if (!publicMethod.isPublic()) {
            throw new AssertionError("placeOrder should be public");
        }
        if (publicMethod.getZoomLevel() != 3) {
            throw new AssertionError("Public method should be zoom level 3, got " + publicMethod.getZoomLevel());
        }
        System.out.println("  ✓ Public method validated");

        CoreNode privateMethod = graph.getNode("com.greens.order.core.OrderService#validateCart(String):void");
        if (privateMethod == null) {
            throw new AssertionError("validateCart method not found");
        }
        if (privateMethod.isPublic()) {
            throw new AssertionError("validateCart should be private");
        }
        if (privateMethod.getZoomLevel() != 4) {
            throw new AssertionError("Private method should be zoom level 4, got " + privateMethod.getZoomLevel());
        }
        System.out.println("  ✓ Private method validated");
    }

    private static void validateEdgeTypes(CoreGraph graph) {
        System.out.println("\n✓ Validating edge relationships...");

        CoreEdge endpointToController = graph.getEdge("e-endpoint-1");
        if (endpointToController == null) {
            throw new AssertionError("Endpoint->Controller edge not found");
        }
        if (endpointToController.getType() != EdgeType.HANDLES) {
            throw new AssertionError("Endpoint edge should be HANDLES type");
        }
        System.out.println("  ✓ HANDLES relationship validated");

        CoreEdge methodCall = graph.getEdge("e-call-6");
        if (methodCall == null) {
            throw new AssertionError("Method call edge not found");
        }
        if (methodCall.getType() != EdgeType.CALL) {
            throw new AssertionError("Method call edge should be CALL type");
        }
        System.out.println("  ✓ CALL relationship validated");

        CoreEdge produces = graph.getEdge("e-produces-1");
        if (produces == null) {
            throw new AssertionError("Produces edge not found");
        }
        if (produces.getType() != EdgeType.PRODUCES) {
            throw new AssertionError("Kafka produces edge should be PRODUCES type");
        }
        System.out.println("  ✓ PRODUCES relationship validated");

        CoreEdge consumes = graph.getEdge("e-consumes-2");
        if (consumes == null) {
            throw new AssertionError("Consumes edge not found");
        }
        if (consumes.getType() != EdgeType.CONSUMES) {
            throw new AssertionError("Kafka consumes edge should be CONSUMES type");
        }
        System.out.println("  ✓ CONSUMES relationship validated");

        CoreEdge belongsTo = graph.getEdge("e-class-service-0");
        if (belongsTo == null) {
            throw new AssertionError("BelongsTo edge not found");
        }
        if (belongsTo.getType() != EdgeType.BELONGS_TO) {
            throw new AssertionError("Class->Service edge should be BELONGS_TO type");
        }
        System.out.println("  ✓ BELONGS_TO relationship validated");

        CoreEdge defines = graph.getEdge("e-method-class-16");
        if (defines == null) {
            throw new AssertionError("Defines edge not found");
        }
        if (defines.getType() != EdgeType.DEFINES) {
            throw new AssertionError("Method->Class edge should be DEFINES type");
        }
        System.out.println("  ✓ DEFINES relationship validated");
    }

    private static void testExports(FlowCoreEngine engine, CoreGraph graph) {
        System.out.println("\n✓ Testing exports...");

        String neo4j = engine.exportToNeo4j(graph);
        if (neo4j == null || neo4j.isEmpty()) {
            throw new AssertionError("Neo4j export failed");
        }
        if (!neo4j.contains("CREATE")) {
            throw new AssertionError("Neo4j export should contain CREATE statements");
        }
        System.out.println("  ✓ Neo4j export: " + neo4j.split("\n").length + " statements");
    }
}

