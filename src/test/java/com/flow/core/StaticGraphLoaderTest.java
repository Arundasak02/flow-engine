package com.flow.core;

import com.flow.core.graph.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StaticGraphLoaderTest {

    @Test
    void loadsNodesEdgesAndAssignsZoomLevels() {
        String json = """
                {
                  "version": "1",
                  "nodes": [
                    {"id":"endpoint:POST /api/orders/{id}","type":"ENDPOINT","name":"POST /api/orders/{id}","data":{}},
                    {"id":"topic:orders.v1","type":"TOPIC","name":"orders.v1","data":{}},
                    {"id":"service:order","type":"SERVICE","name":"order","data":{}},
                    {"id":"com.greens.order.core.OrderService","type":"CLASS","name":"OrderService","data":{"className":"com.greens.order.core.OrderService"}},
                    {"id":"com.greens.order.core.OrderService#placeOrder(String):String","type":"METHOD","name":"placeOrder","data":{"className":"com.greens.order.core.OrderService","visibility":"PUBLIC"}},
                    {"id":"com.greens.order.core.OrderService#validateCart(String):void","type":"PRIVATE_METHOD","name":"validateCart","data":{"className":"com.greens.order.core.OrderService","visibility":"PRIVATE"}}
                  ],
                  "edges": [
                    {"id":"e-endpoint-1","from":"endpoint:POST /api/orders/{id}","to":"service:order","type":"HANDLES"},
                    {"id":"e-produces-1","from":"com.greens.order.core.OrderService#placeOrder(String):String","to":"topic:orders.v1","type":"PRODUCES"},
                    {"id":"e-consumes-2","from":"topic:orders.v1","to":"service:order","type":"CONSUMES"},
                    {"id":"e-class-service-0","from":"com.greens.order.core.OrderService","to":"service:order","type":"BELONGS_TO"},
                    {"id":"e-method-class-16","from":"com.greens.order.core.OrderService#placeOrder(String):String","to":"com.greens.order.core.OrderService","type":"DEFINES"}
                  ]
                }
                """;

        FlowCoreEngine engine = new FlowCoreEngine();
        CoreGraph graph = engine.process(json);

        assertEquals(6, graph.getNodeCount());
        assertEquals(5, graph.getEdgeCount());

        assertEquals(NodeType.ENDPOINT, graph.getNode("endpoint:POST /api/orders/{id}").getType());
        assertEquals(1, graph.getNode("endpoint:POST /api/orders/{id}").getZoomLevel());

        assertEquals(NodeType.SERVICE, graph.getNode("service:order").getType());
        assertEquals(2, graph.getNode("service:order").getZoomLevel());

        assertEquals(3, graph.getNode("com.greens.order.core.OrderService#placeOrder(String):String").getZoomLevel());
        assertEquals(4, graph.getNode("com.greens.order.core.OrderService#validateCart(String):void").getZoomLevel());

        String neo4j = engine.exportToNeo4j(graph);
        assertNotNull(neo4j);
        assertTrue(neo4j.contains("CREATE"));
    }
}

