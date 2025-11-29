# Flow Core Engine

**Flow Core** is a pure Java library that serves as the data processing engine for the Flow platform. It transforms raw static call graphs and runtime execution traces into a unified, zoomable graph model.

## Project Context

Flow Core sits at the heart of the Flow platform architecture:

- **Input**: Static call graphs (flow.json) from Flow Adapter + optional runtime events
- **Processing**: Validates, enriches, assigns zoom levels, extracts flows, merges data
- **Output**: Enriched CoreGraph ready for persistence or export (Neo4j Cypher, GEF JSON)

### What It Is
✅ Pure Java (JDK 17+)  
✅ No frameworks, no Spring Boot, no HTTP  
✅ Single-responsibility components  
✅ Modular and testable  
✅ Fast and lightweight  

### What It Is NOT
❌ REST API or web service  
❌ Database storage layer  
❌ Source code parser  
❌ Spring Boot application  
❌ Kafka/messaging consumer  
❌ UI or visualization logic  

## Architecture & How It Works

Flow Core processes graphs through an **ordered pipeline**:

```
┌─────────────────────────────────────────────────────────────────┐
│ FlowCoreEngine (Orchestrator)                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
    INGEST LAYER        PROCESSING LAYER     EXPORT LAYER
    
    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
    │ Static Graph │    │   Validate   │    │  Neo4j Cypher│
    │   Loader     │    │   Structure  │    │   Exporter   │
    └──────────────┘    └──────────────┘    └──────────────┘
                             │
                        ┌────┴────┐
                        ▼         ▼
                    ┌────────┬─────────┐
                    │ Zoom   │ Extract │
                    │Engine  │ Flows   │
                    └────────┴─────────┘
    
    ┌──────────────┐         │
    │ Runtime Event│         │       ┌──────────────┐
    │  Ingestor    │─────────┼──────▶│ Merge Engine │
    └──────────────┘         │       └──────────────┘
                        (optional)
```

### Pipeline Execution Order (CRITICAL)

1. **Load** → Parse flow.json into graph structure
2. **Zoom** → Assign zoom levels (1-5) to all nodes
3. **Validate** → Check structure integrity
4. **Extract** → Find flows from endpoints
5. **Ingest** (optional) → Load runtime events
6. **Merge** (optional) → Combine static + runtime
7. **Export** → Generate Neo4j or GEF output

**Important**: Zoom assignment happens **before** validation. Nodes start with zoom=-1 (unassigned) and must have valid levels (1-5) before the validator runs.

## Package Structure

```
com.flow.core/
│
├── FlowCoreEngine.java          (main orchestrator)
│
├── graph/                       (Graph data model)
│   ├── CoreNode.java           (node with type, visibility, zoom)
│   ├── CoreEdge.java           (edge between nodes)
│   ├── CoreGraph.java          (container: nodes + edges)
│   └── GraphValidator.java     (validates structure and zoom)
│
├── ingest/                      (Input processing)
│   ├── StaticGraphLoader.java  (parses flow.json into CoreGraph)
│   ├── RuntimeEventIngestor.java(loads runtime execution traces)
│   └── MergeEngine.java        (merges static + runtime data)
│
├── zoom/                        (Zoom level assignment)
│   ├── ZoomEngine.java         (assigns levels to nodes)
│   ├── ZoomPolicy.java         (type → zoom level mappings)
│   └── ZoomLevel.java          (zoom level constants)
│
├── flow/                        (Flow extraction)
│   ├── FlowExtractor.java      (BFS traversal from endpoints)
│   ├── FlowModel.java          (represents a complete flow)
│   └── FlowStep.java           (single node in a flow)
│
└── export/                      (Output generation)
    ├── Neo4jExporter.java      (generates Cypher queries)
    └── GEFExporter.java        (generates GEF 1.1 JSON)
```

## Zoom Levels (Business → Runtime)

| Level | Name | Purpose | Examples |
|-------|------|---------|----------|
| 1 | BUSINESS | Entry points, topics | REST endpoints, Kafka topics |
| 2 | SERVICE | Logical groupings | Classes, services, components |
| 3 | PUBLIC | Exported interface | Public methods, APIs |
| 4 | PRIVATE | Internal implementation | Private methods, helpers |
| 5 | RUNTIME | Execution traces | Stack frames, runtime calls |

Zoom levels enable progressive disclosure - users can zoom in/out to see different levels of detail.

## Implementation Status

### ✅ Implemented
- **CoreGraph** - Immutable graph data structure with nodes and edges
- **StaticGraphLoader** - Parses GEF 1.1 JSON format
- **ZoomEngine & ZoomPolicy** - Assigns zoom levels based on node type
- **GraphValidator** - Validates structure, zoom levels, references
- **FlowExtractor** - BFS traversal to extract flows per endpoint
- **Neo4jExporter** - Generates Cypher queries
- **GEFExporter** - Exports enriched graph back to GEF JSON
- **RuntimeEventIngestor** - Loads runtime execution traces
- **MergeEngine** - Merges static call graph with runtime data

### 🔄 Ready for Extension
- Custom zoom policies (implement `ZoomPolicy` interface)
- Additional exporters (implement `Exporter` pattern)
- Graph transformation filters
- Performance metrics aggregation
- Advanced traversal algorithms

### ❓ Future Enhancements
- [ ] Graph compression/summarization
- [ ] Cycle detection and handling
- [ ] Path finding (shortest path, all paths)
- [ ] Community detection
- [ ] GraphML format support
- [ ] Batch processing for large graphs

## Key Design Decisions

### Why Pure Java?
- No external runtime dependencies
- Fast startup and execution
- Suitable for embedded use in various platforms
- Easy to integrate into different ecosystems

### Why Separate Packages?
- **graph/** - Core data structure, used everywhere
- **ingest/** - Input handling, isolated from processing
- **zoom/** - Policy-based level assignment, reusable
- **flow/** - Graph traversal logic, independent algorithm
- **export/** - Output generation, pluggable exporters

### Why Immutable Where Possible?
- Thread-safe by default
- Easier to reason about behavior
- Prevents accidental state mutations
- Core graph is append-only after loading

### Why Validate After Zoom?
Nodes must have zoom levels (1-5) assigned before validation runs, otherwise validator fails on uninitialized zoom=-1 state.

## Input/Output Formats

### flow.json (Unified Format)

**Structure**: Single `nodes` array + single `edges` array

```json
{
  "graphId": "project-name",
  "nodes": [
    {
      "id": "unique-node-id",
      "type": "METHOD|ENDPOINT|TOPIC|CLASS|SERVICE|PRIVATE_METHOD",
      "name": "Display Name",
      "data": {
        "visibility": "public|private",
        "className": "...",
        "packageName": "...",
        "moduleName": "...",
        "signature": "...",
        "httpMethod": "...",
        "path": "..."
      }
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

**Node Types**:
- `METHOD` - Public method, zoom level 3
- `PRIVATE_METHOD` - Private method, zoom level 4
- `CLASS` - Class/component, zoom level 2
- `SERVICE` - Service module, zoom level 2
- `ENDPOINT` - HTTP REST endpoint, zoom level 1
- `TOPIC` - Kafka topic, zoom level 1

**Edge Types**:
- `CALL` - Method to method call
- `HANDLES` - Endpoint to handler method
- `PRODUCES` - Message producer
- `CONSUMES` - Message consumer
- `BELONGS_TO` - Class/method belongs to service
- `DEFINES` - Method defined in class

### Runtime Events
```json
{
  "timestamp": 1699123456789,
  "sourceId": "com.service.Class#method():void",
  "targetId": "com.other.Class#method():void",
  "executionCount": 42,
  "duration": 150
}
```

### Cypher Output (Neo4j)
```cypher
CREATE (n:Node {id: "...", type: "METHOD", zoom: 3})
CREATE (m:Node {id: "...", type: "SERVICE", zoom: 2})
CREATE (n)-[:CALLS]->(m)
```

## Core Concepts

### CoreNode
Represents any vertex in the graph: endpoint, service, method, runtime call.
- **id**: Unique identifier
- **type**: "ENDPOINT", "SERVICE", "METHOD", "TOPIC", "RUNTIME_CALL", etc.
- **zoom**: Level 1-5 (assigned by ZoomEngine)
- **isPublic**: Visibility flag (affects private/public method distinction)

### CoreEdge
Represents a relationship between two nodes.
- **type**: "CALLS", "HANDLES", "PRODUCES", "CONSUMES"
- **sourceId**, **targetId**: References to nodes

### CoreGraph
Container holding all nodes and edges.
- Thread-safe reads via ConcurrentHashMap
- No automatic persistence (caller handles storage)
- Supports efficient lookups by ID and type

## Integration Points

**Upstream**: Flow Adapter
- Produces flow.json in GEF 1.1 format
- Flow Core consumes this JSON

**Downstream**: Flow Core Service
- Uses CoreGraph from Flow Core
- Queries graph, applies business logic
- Handles API endpoints and persistence

**Consumers**: Flow UI
- Receives exported Cypher queries or GEF JSON
- Renders visualizations at different zoom levels

## Dependencies

**Compile:**
- Jackson 2.16.0 (JSON parsing)

**Test:**
- JUnit 5 (testing framework)

No other external dependencies. Core logic is pure Java.

## Building

```bash
mvn clean package
```

Produces JAR with all code and Jackson library included.

