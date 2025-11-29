package com.flow.core.zoom;

import com.flow.core.graph.*;

public class ZoomEngineTest {

    public static void main(String[] args) {
        System.out.println("=== ZoomEngine Tests ===\n");

        testBasicZoomAssignment();
        testCustomPolicy();
        testZoomLevelBounds();

        System.out.println("\n✓ All ZoomEngine tests passed!");
    }

    private static void testBasicZoomAssignment() {
        System.out.println("Test: Basic zoom level assignment");

        CoreGraph graph = new CoreGraph("1");
        graph.addNode(new CoreNode("ep1", "Endpoint", NodeType.ENDPOINT, null, Visibility.PUBLIC));
        graph.addNode(new CoreNode("svc1", "Service", NodeType.SERVICE, "svc1", Visibility.PUBLIC));
        graph.addNode(new CoreNode("meth1", "Method", NodeType.METHOD, "svc1", Visibility.PUBLIC));

        ZoomEngine engine = new ZoomEngine();
        engine.assignZoomLevels(graph);

        assert graph.getNode("ep1").getZoomLevel() == 1 : "Endpoint should be zoom level 1";
        assert graph.getNode("svc1").getZoomLevel() == 2 : "Service should be zoom level 2";
        assert graph.getNode("meth1").getZoomLevel() == 3 : "Method should be zoom level 3";

        System.out.println("  ✓ Nodes assigned correct zoom levels");
    }

    private static void testCustomPolicy() {
        System.out.println("\nTest: Custom zoom policy");

        CoreGraph graph = new CoreGraph("1");
        graph.addNode(new CoreNode("custom1", "Custom", NodeType.INTERFACE, null, Visibility.PUBLIC));

        ZoomPolicy policy = new ZoomPolicy();
        policy.setZoomLevel(NodeType.INTERFACE, 4);

        ZoomEngine engine = new ZoomEngine(policy);
        engine.assignZoomLevels(graph);

        assert graph.getNode("custom1").getZoomLevel() == 4 : "Custom type should be zoom level 4";

        System.out.println("  ✓ Custom policy applied correctly");
    }

    private static void testZoomLevelBounds() {
        System.out.println("\nTest: Zoom level boundary validation");

        CoreGraph graph = new CoreGraph("1");
        graph.addNode(new CoreNode("unknown", "Unknown", NodeType.FIELD, null, Visibility.PRIVATE));

        ZoomEngine engine = new ZoomEngine();

        try {
            engine.assignZoomLevels(graph);
            System.out.println("  ✓ FIELD type assigned correctly to level 4");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✗ Should have assigned FIELD type to level 4");
            throw e;
        }
    }
}

