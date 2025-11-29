# Flow Core Developer Guide

## Overview

This guide provides developers with understanding of the Flow Core architecture, how components work together, and how to extend or maintain the system.

## Project Structure

```
flow-engine/
├── pom.xml                          (Maven configuration)
├── README.md                        (User documentation)
├── CONTEXT.md                       (Architecture & constraints)
├── DEVELOPER_GUIDE.md               (This file)
│
└── src/
    ├── main/java/com/flow/core/
    │   ├── FlowCoreEngine.java      (Main orchestrator class)
    │   │
    │   ├── graph/                   (Data model)
    │   │   ├── CoreNode.java        (Vertex in graph)
    │   │   ├── CoreEdge.java        (Edge between vertices)
    │   │   ├── CoreGraph.java       (Graph container)
    │   │   └── GraphValidator.java  (Validation logic)
    │   │
    │   ├── ingest/                  (Input processing)
    │   │   ├── StaticGraphLoader.java  (JSON → CoreGraph)
    │   │   ├── RuntimeEventIngestor.java (Events → Graph)
    │   │   └── MergeEngine.java        (Combines static + runtime)
    │   │
    │   ├── zoom/                    (Zoom assignment)
    │   │   ├── ZoomEngine.java      (Assigns zoom levels)
    │   │   ├── ZoomPolicy.java      (Policy rules)
    │   │   └── ZoomLevel.java       (Constants)
    │   │
    │   ├── flow/                    (Flow extraction)
    │   │   ├── FlowExtractor.java   (Traversal algorithm)
    │   │   ├── FlowModel.java       (Flow representation)
    │   │   └── FlowStep.java        (Step in flow)
    │   │
    │   └── export/                  (Output generation)
    │       ├── Neo4jExporter.java   (Cypher output)
    │       └── GEFExporter.java     (GEF 1.1 output)
    │
    └── test/java/com/flow/core/
        ├── FlowCoreSmokeTest.java
        ├── ingest/
        │   └── StaticGraphLoaderTest.java
        └── zoom/
            └── ZoomEngineTest.java
```

## How It Works

### 1. Graph Loading (StaticGraphLoader)

**Input**: flow.json (unified format with nodes + edges arrays)  
**Output**: CoreGraph with all nodes and edges

```java
public CoreGraph load(String jsonString)
```

**Format Structure**:
```json
{
  "graphId": "project-id",
  "nodes": [
    {
      "id": "node-id",
      "type": "METHOD|ENDPOINT|TOPIC|CLASS|SERVICE|PRIVATE_METHOD",
      "name": "Display Name",
      "data": { metadata }
    }
  ],
  "edges": [
    {
      "id": "edge-id",
      "from": "source-node-id",
      "to": "target-node-id",
      "type": "CALL|HANDLES|PRODUCES|CONSUMES|BELONGS_TO|DEFINES"
    }
  ]
}
```

**Parsing Logic**:
1. Iterate `nodes` array, create CoreNode for each
2. Extract type, name, and visibility from `data` field
3. Iterate `edges` array, create CoreEdge for each
4. Both nodes in edge must exist before edge is added

**Key Details**:
- Nodes start with zoom=-1 (unassigned)
- Visibility extracted from data.visibility field
- PRIVATE_METHOD type nodes automatically treated as private
- Edge creation is gated on source+target node existence

### 2. Zoom Assignment (ZoomEngine + ZoomPolicy)

**Input**: CoreGraph with unassigned zoom levels  
**Output**: CoreGraph with zoom 1-5 assigned to all nodes

```java
ZoomPolicy policy = new ZoomPolicy();  // Loads default mappings
ZoomEngine engine = new ZoomEngine(policy);
engine.assignZoomLevels(graph);
```

**Default Mappings**:
- Level 1: ENDPOINT, TOPIC, BUSINESS_OPERATION
- Level 2: SERVICE, CLASS, COMPONENT
- Level 3: METHOD, FUNCTION
- Level 4: PRIVATE_METHOD, INTERNAL_FUNCTION
- Level 5: RUNTIME_CALL, STACK_FRAME

For METHOD nodes, visibility further refines:
- `isPublic=true` → zoom 3 (PUBLIC)
- `isPublic=false` → zoom 4 (PRIVATE)

### 3. Validation (GraphValidator)

**Input**: CoreGraph with zoom levels  
**Output**: Validated or exception thrown

```java
GraphValidator validator = new GraphValidator();
validator.validate(graph);  // Throws if validation fails
```

**Checks**:
- All nodes have zoom 1-5 (not -1)
- No duplicate node IDs
- No duplicate edge IDs
- All edge references exist
- Optional: No self-loops (strict mode)

### 4. Flow Extraction (FlowExtractor)

**Input**: Validated CoreGraph  
**Output**: List of FlowModel (one per endpoint)

```java
FlowExtractor extractor = new FlowExtractor();
List<FlowModel> flows = extractor.extractFlows(graph);
```

**Algorithm**: BFS from each ENDPOINT node
- Traverses outgoing edges
- Stops at MAX_DEPTH (100)
- Creates a FlowModel with ordered steps

### 5. Runtime Ingestion (RuntimeEventIngestor)

**Input**: CoreGraph + runtime events  
**Output**: Graph enriched with runtime data

```java
List<Map<String, Object>> events = loadRuntimeEvents();
RuntimeEventIngestor ingestor = new RuntimeEventIngestor();
ingestor.ingest(events, graph);
```

Maps runtime events to existing nodes, adds execution metadata.

### 6. Merge (MergeEngine)

**Input**: Static CoreGraph + ingested runtime data  
**Output**: Unified graph

```java
mergeEngine.merge(graph);
```

Combines call traces with static structure, marks runtime nodes.

### 7. Export

**Neo4j Cypher**:
```java
Neo4jExporter exporter = new Neo4jExporter();
String cypher = exporter.export(graph);
```

Generates CREATE statements for nodes and relationships.

**GEF JSON**:
```java
GEFExporter exporter = new GEFExporter();
String gef = exporter.export(graph);
```

Exports back to GEF 1.1 format with zoom levels included.

## Core Data Structures

### CoreNode
```java
public class CoreNode {
    String id;          // Unique identifier
    String name;        // Display name
    String type;        // ENDPOINT, SERVICE, METHOD, etc.
    int zoomLevel;      // 1-5 (or -1 if unassigned)
    String serviceId;   // Parent service reference
    boolean isPublic;   // Visibility
}
```

### CoreEdge
```java
public class CoreEdge {
    String id;          // Unique identifier
    String sourceId;    // Reference to source node
    String targetId;    // Reference to target node
    String type;        // CALLS, HANDLES, PRODUCES, CONSUMES
}
```

### CoreGraph
```java
public class CoreGraph {
    String version;     // Schema version (e.g., "gef:1.1")
    
    void addNode(CoreNode node)
    void addEdge(CoreEdge edge)
    
    CoreNode getNode(String id)
    List<CoreNode> getAllNodes()
    List<CoreEdge> getAllEdges()
    
    List<CoreNode> getNodesByZoomLevel(int level)
    List<CoreEdge> getOutgoingEdges(String nodeId)
    List<CoreEdge> getIncomingEdges(String nodeId)
}
```

## Extension Points

### Adding a Custom Zoom Policy

```java
public class MyZoomPolicy extends ZoomPolicy {
    public MyZoomPolicy() {
        super();
        // Override default mappings
        setZoomLevel("MY_NODE_TYPE", 2);
    }
}

// Usage
ZoomPolicy policy = new MyZoomPolicy();
ZoomEngine engine = new ZoomEngine(policy);
```

### Adding a Custom Exporter

```java
public class GraphMLExporter {
    public String export(CoreGraph graph) {
        // Generate GraphML format
        return graphmlOutput;
    }
}

// Integrate into FlowCoreEngine
public class FlowCoreEngine {
    private final GraphMLExporter exporter = new GraphMLExporter();
    
    public String exportToGraphML(CoreGraph graph) {
        return exporter.export(graph);
    }
}
```

### Custom Graph Validation

```java
GraphValidator validator = new GraphValidator();
validator.setStrictMode(true);  // Enable self-loop detection
validator.validate(graph);

// Or extend with custom rules
public class CustomValidator extends GraphValidator {
    public void validateCustomRules(CoreGraph graph) {
        // Your validation logic
    }
}
```

## Common Patterns

### Filtering Nodes by Type
```java
List<CoreNode> methods = graph.getAllNodes().stream()
    .filter(n -> "METHOD".equals(n.getType()))
    .collect(Collectors.toList());
```

### Finding All Paths Between Two Nodes
Use BFS or DFS on CoreGraph's edge structure. FlowExtractor already implements BFS traversal - reuse that pattern.

### Calculating Node Centrality
```java
Map<String, Integer> inDegree = new HashMap<>();
for (CoreEdge edge : graph.getAllEdges()) {
    inDegree.merge(edge.getTargetId(), 1, Integer::sum);
}
```

### Exporting Subgraph
```java
// Filter nodes
Set<String> nodeIds = graph.getAllNodes().stream()
    .filter(n -> n.getZoomLevel() >= 2)  // Only SERVICE and above
    .map(CoreNode::getId)
    .collect(Collectors.toSet());

// Create new graph with filtered nodes/edges
CoreGraph subgraph = new CoreGraph(graph.getVersion());
// Copy filtered nodes and edges
```

## Known Constraints

### Thread Safety
- CoreGraph uses ConcurrentHashMap for thread-safe reads
- Adding nodes/edges is NOT thread-safe during construction
- Once fully constructed, safe for concurrent reads

### Performance
- Optimized for graphs with 1K-100K nodes
- BFS traversal limited to MAX_DEPTH=100 to prevent infinite loops
- No graph compression or caching by default

### Zoom Levels
- Must be assigned BEFORE validation (this was a critical bug fix)
- Default policy handles standard node types
- Custom types require explicit policy registration

## Troubleshooting

### Error: "Invalid zoom level -1"
**Cause**: Zoom assignment was skipped or failed  
**Fix**: Ensure ZoomEngine.assignZoomLevels() is called before validation

### Error: "No zoom level configured for node type: XYZ"
**Cause**: Custom node type not in ZoomPolicy  
**Fix**: Register the type:
```java
policy.setZoomLevel("XYZ", 2);
```

### Error: "Edge references non-existent node"
**Cause**: Edge refers to node that doesn't exist  
**Fix**: Add nodes before edges, or ensure StaticGraphLoader parsed all nodes

### Error: "Jackson ClassNotFoundException"
**Cause**: Jackson not on classpath (likely marked optional in pom.xml)  
**Fix**: Remove `<optional>true</optional>` from Jackson dependency

## Testing Strategy

Flow Core uses integration tests with real JSON to verify end-to-end pipeline:

1. **StaticGraphLoaderTest** - Loads payment-service graph, validates complete pipeline
2. **ZoomEngineTest** - Tests zoom level assignment
3. **FlowCoreSmokeTest** - Basic smoke test of full engine

To run tests:
```bash
mvn test
```

Or run integration test directly:
```bash
mvn dependency:copy-dependencies -DincludeScope=compile
java -cp "target/classes:target/test-classes:target/dependency/*" \
  com.flow.core.ingest.StaticGraphLoaderTest
```

## Code Quality

**Principles**:
- Single responsibility - each class has one job
- Immutable where possible - use final fields and records
- No side effects - pure functions preferred
- Clear naming - method/variable names describe intent

**Conventions**:
- 4-space indentation
- JavaDoc on all public APIs
- No empty catch blocks
- Null checks with Objects.requireNonNull()

## Building & Deployment

```bash
# Build
mvn clean package

# Run tests
mvn test

# Generate JAR
mvn clean package -DskipTests
```

Output JAR includes:
- All Flow Core classes
- Jackson library (bundled)
- Executable without external dependencies

## Integration with Flow Platform

**Upstream** (Input):
- Flow Adapter generates flow.json
- Runtime Plugin provides event streams

**Downstream** (Consumption):
- Flow Core Service reads CoreGraph
- Flow UI receives exported Cypher/GEF

Flow Core sits cleanly in the middle - pure data transformation.

## What's NOT Here

- REST APIs
- Database operations
- Spring Boot configuration
- HTTP client/server code
- Source code parsing
- UI/visualization logic

Flow Core is intentionally a data processing library, not a service.

