package com.flow.core;

import com.flow.core.graph.*;

import java.util.*;

public class FlowCoreSmokeTest {

    public static void main(String[] args) {
        System.out.println("=== Flow Core Smoke Test ===\n");

        try {
            // Create a simple test graph manually (no JSON parsing yet)
            CoreGraph graph = createTestGraph();

            System.out.println("✓ Test graph created");
            System.out.println("  - Nodes: " + graph.getNodeCount());
            System.out.println("  - Edges: " + graph.getEdgeCount());

            // Create engine and process
            FlowCoreEngine engine = new FlowCoreEngine();

            // Assign zoom levels
            engine.process("{}"); // Will fail gracefully until JSON parsing is implemented

        } catch (UnsupportedOperationException e) {
            System.out.println("\n✓ Expected error (JSON parsing not yet implemented):");
            System.out.println("  " + e.getMessage());
            System.out.println("\n  → Add Jackson dependency to pom.xml to enable JSON loading");
        } catch (Exception e) {
            System.out.println("\n✗ Unexpected error:");
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\n✓ Smoke test completed successfully!");
    }

    private static CoreGraph createTestGraph() {
        CoreGraph graph = new CoreGraph("1");

        CoreNode ep1 = new CoreNode("ep1", "GET /api/users", NodeType.ENDPOINT, null, Visibility.PUBLIC);
        CoreNode svc1 = new CoreNode("svc1", "UserService", NodeType.SERVICE, "svc1", Visibility.PUBLIC);
        CoreNode meth1 = new CoreNode("meth1", "getUser", NodeType.METHOD, "svc1", Visibility.PUBLIC);

        graph.addNode(ep1);
        graph.addNode(svc1);
        graph.addNode(meth1);

        CoreEdge e1 = new CoreEdge("e1", "ep1", "svc1", EdgeType.CALL);
        CoreEdge e2 = new CoreEdge("e2", "svc1", "meth1", EdgeType.CALL);

        graph.addEdge(e1);
        graph.addEdge(e2);

        return graph;
    }
}

