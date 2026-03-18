package com.flow.core.zoom;

import com.flow.core.graph.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoomEngineTest {

    @Test
    void assignsZoomLevelsForStandardNodeTypes() {
        CoreGraph graph = new CoreGraph("1");
        graph.addNode(new CoreNode("ep1", "Endpoint", NodeType.ENDPOINT, null, Visibility.PUBLIC));
        graph.addNode(new CoreNode("svc1", "Service", NodeType.SERVICE, "svc1", Visibility.PUBLIC));
        graph.addNode(new CoreNode("meth1", "Method", NodeType.METHOD, "svc1", Visibility.PUBLIC));

        ZoomEngine engine = new ZoomEngine();
        engine.assignZoomLevels(graph);

        assertEquals(1, graph.getNode("ep1").getZoomLevel());
        assertEquals(2, graph.getNode("svc1").getZoomLevel());
        assertEquals(3, graph.getNode("meth1").getZoomLevel());
    }

    @Test
    void appliesCustomPolicy() {
        CoreGraph graph = new CoreGraph("1");
        graph.addNode(new CoreNode("custom1", "Custom", NodeType.INTERFACE, null, Visibility.PUBLIC));

        ZoomPolicy policy = new ZoomPolicy();
        policy.setZoomLevel(NodeType.INTERFACE, 4);

        ZoomEngine engine = new ZoomEngine(policy);
        engine.assignZoomLevels(graph);

        assertEquals(4, graph.getNode("custom1").getZoomLevel());
    }
}

