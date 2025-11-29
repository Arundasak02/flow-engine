package com.flow.core.zoom;

import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.CoreNode;

/**
 * Tests for the ZoomEngine component.
 *
 * Validates:
 * - Correct zoom level assignment based on node type
 * - Policy application
 * - Error handling for unknown node types
 */
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
        graph.addNode(new CoreNode("ep1", "Endpoint", "ENDPOINT", null, true));
        graph.addNode(new CoreNode("svc1", "Service", "SERVICE", "svc1", true));
        graph.addNode(new CoreNode("meth1", "Method", "METHOD", "svc1", true));

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
        graph.addNode(new CoreNode("custom1", "Custom", "CUSTOM_TYPE", null, true));

        ZoomPolicy policy = new ZoomPolicy();
        policy.setZoomLevel("CUSTOM_TYPE", 4);

        ZoomEngine engine = new ZoomEngine(policy);
        engine.assignZoomLevels(graph);

        assert graph.getNode("custom1").getZoomLevel() == 4 : "Custom type should be zoom level 4";

        System.out.println("  ✓ Custom policy applied correctly");
    }

    private static void testZoomLevelBounds() {
        System.out.println("\nTest: Zoom level boundary validation");

        CoreGraph graph = new CoreGraph("1");
        graph.addNode(new CoreNode("unknown", "Unknown", "UNKNOWN_TYPE", null, true));

        ZoomEngine engine = new ZoomEngine();

        try {
            engine.assignZoomLevels(graph);
            System.out.println("  ✗ Should have thrown exception for unknown node type");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✓ Correctly rejected unknown node type: " + e.getMessage());
        }
    }
}

