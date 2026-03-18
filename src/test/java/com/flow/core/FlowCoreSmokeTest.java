package com.flow.core;

import com.flow.core.graph.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlowCoreSmokeTest {

    @Test
    void engineProcessesMinimalGraphJson() {
        String json = """
                {
                  "version": "1",
                  "nodes": [
                    {"id":"ep1","type":"ENDPOINT","name":"GET /api/users","data":{}},
                    {"id":"svc1","type":"SERVICE","name":"UserService","data":{"className":"svc1"}},
                    {"id":"meth1","type":"METHOD","name":"getUser","data":{"className":"svc1"}}
                  ],
                  "edges": [
                    {"id":"e1","from":"ep1","to":"svc1","type":"CALL"},
                    {"id":"e2","from":"svc1","to":"meth1","type":"CALL"}
                  ]
                }
                """;

        FlowCoreEngine engine = new FlowCoreEngine();
        CoreGraph graph = engine.process(json);

        assertNotNull(graph);
        assertEquals(3, graph.getNodeCount());
        assertEquals(2, graph.getEdgeCount());
        assertEquals(1, graph.getNode("ep1").getZoomLevel());
    }
}

