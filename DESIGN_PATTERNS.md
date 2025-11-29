# Design Patterns Implementation Guide

**Date:** November 29, 2025  
**Status:** ✅ Implemented

---

## Overview

This document describes the design patterns and interfaces implemented in the Flow Core Engine to ensure maintainability, extensibility, and clean architecture.

---

## ✅ 1. Strategy Pattern - Graph Exporters

### Problem
Multiple export formats needed (Neo4j, GraphViz, CSV, JSON, Protobuf) with potential for more in the future.

### Solution: GraphExporter Interface

```java
public interface GraphExporter {
    String export(CoreGraph graph);
    default String getFormat();
}
```

### Implementations
- **Neo4jExporter** - Exports to Cypher CREATE statements
- **Future:** GraphVizExporter, CSVExporter, JSONExporter, ProtobufExporter

### Benefits
✅ **Factory-based selection** - `ExporterFactory.getExporter(Format.NEO4J)`  
✅ **Easy plugins** - Register custom exporters  
✅ **Clean testability** - Mock exporters for testing  
✅ **Dependency injection** - Ready for Spring/CDI integration  

### Usage Example

```java
// Direct usage
GraphExporter exporter = new Neo4jExporter();
String cypher = exporter.export(graph);

// Factory usage
GraphExporter exporter = ExporterFactory.getExporter(Format.NEO4J);
String cypher = exporter.export(graph);

// Via FlowCoreEngine
FlowCoreEngine engine = new FlowCoreEngine();
String cypher = engine.exportToNeo4j(graph);
String output = engine.export(graph, ExporterFactory.Format.NEO4J);
```

### Extending

```java
// Create custom exporter
public class GraphVizExporter implements GraphExporter {
    @Override
    public String export(CoreGraph graph) {
        // Generate DOT format
        return "digraph { ... }";
    }
}

// Register in factory
ExporterFactory.registerExporter(Format.GRAPHVIZ, GraphVizExporter.class);
```

---

## ✅ 2. Strategy Pattern - Runtime Event Handlers

### Problem
Different runtime event types require different processing logic. Future expansion needed for:
- DB calls (SQL, MongoDB, Redis)
- HTTP/gRPC spans
- Custom instrumentation from Python, Node.js, .NET plugins

### Solution: RuntimeEventHandler Interface

```java
public interface RuntimeEventHandler {
    void handle(RuntimeEvent event, CoreGraph graph);
    boolean canHandle(EventType eventType);
}
```

### Implementations

| Handler | Event Type | Responsibility |
|---------|-----------|----------------|
| **MethodEnterHandler** | METHOD_ENTER | Creates runtime method nodes |
| **MethodExitHandler** | METHOD_EXIT | Placeholder for per-event processing |
| **ProduceTopicHandler** | PRODUCE_TOPIC | Creates topic nodes and PRODUCES edges |
| **ConsumeTopicHandler** | CONSUME_TOPIC | Creates consumer nodes |
| **CheckpointHandler** | CHECKPOINT | Attaches checkpoint data to nodes |
| **ErrorHandler** | ERROR | Attaches error information to nodes |

### Registry Pattern

```java
public class RuntimeEventHandlerRegistry {
    private final Map<EventType, RuntimeEventHandler> handlers;
    
    public RuntimeEventHandler getHandler(EventType eventType);
    public void registerHandler(EventType eventType, RuntimeEventHandler handler);
}
```

### Benefits
✅ **Modular processing** - Each handler is independent  
✅ **Easy extension** - Add new event types without modifying existing code  
✅ **Testable** - Test each handler in isolation  
✅ **Multi-language support** - Ready for Python, Node.js, .NET plugins  

### Usage Example

```java
// Registry manages all handlers
RuntimeEventHandlerRegistry registry = new RuntimeEventHandlerRegistry();

// Process events
for (RuntimeEvent event : events) {
    RuntimeEventHandler handler = registry.getHandler(event.getType());
    handler.handle(event, graph);
}

// Register custom handler
registry.registerHandler(EventType.DB_QUERY, new DBQueryHandler());
```

### Future Extensions

```java
// HTTP span handler
public class HttpSpanHandler implements RuntimeEventHandler {
    @Override
    public void handle(RuntimeEvent event, CoreGraph graph) {
        String url = (String) event.getDataValue("url");
        int statusCode = (int) event.getDataValue("statusCode");
        // Create HTTP call node and edges
    }
    
    @Override
    public boolean canHandle(EventType eventType) {
        return eventType == EventType.HTTP_CALL;
    }
}

// Redis operation handler
public class RedisSpanHandler implements RuntimeEventHandler {
    @Override
    public void handle(RuntimeEvent event, CoreGraph graph) {
        String command = (String) event.getDataValue("command");
        String key = (String) event.getDataValue("key");
        // Create Redis operation node
    }
    
    @Override
    public boolean canHandle(EventType eventType) {
        return eventType == EventType.REDIS_OPERATION;
    }
}
```

---

## ✅ 3. Factory Pattern - Exporter Creation

### Implementation

```java
public class ExporterFactory {
    public enum Format {
        NEO4J, GRAPHVIZ, CSV, JSON
    }
    
    private static final Map<Format, Class<? extends GraphExporter>> EXPORTERS;
    
    public static GraphExporter getExporter(Format format);
    public static void registerExporter(Format format, Class<? extends GraphExporter> exporterClass);
}
```

### Benefits
✅ **Centralized creation** - Single point for exporter instantiation  
✅ **Type-safe selection** - Enum-based format selection  
✅ **Easy registration** - Add new formats at runtime  
✅ **Lazy instantiation** - Exporters created only when needed  

### Usage in Flow Core Service

```java
@RestController
public class ExportController {
    
    @GetMapping("/api/graph/export")
    public String exportGraph(@RequestParam String format, @RequestBody CoreGraph graph) {
        ExporterFactory.Format fmt = ExporterFactory.Format.valueOf(format.toUpperCase());
        GraphExporter exporter = ExporterFactory.getExporter(fmt);
        return exporter.export(graph);
    }
}
```

---

## ✅ 4. Pipeline Pattern - MergeEngine Stages

### Problem
The original `mergeStaticAndRuntime()` method was a "God method" doing too much in one place.

### Solution: Independent Pipeline Stages

```java
public class MergeEngine {
    private final RuntimeNodeStage runtimeNodeStage;
    private final RuntimeEdgeStage runtimeEdgeStage;
    private final DurationStage durationStage;
    private final CheckpointStage checkpointStage;
    private final AsyncHopStage asyncHopStage;
    private final ErrorStage errorStage;
    
    public CoreGraph mergeStaticAndRuntime(CoreGraph staticGraph, List<RuntimeEvent> events) {
        MergeContext context = new MergeContext(staticGraph, events);
        
        runtimeNodeStage.execute(context);
        runtimeEdgeStage.execute(context);
        durationStage.execute(context);
        checkpointStage.execute(context);
        asyncHopStage.execute(context);
        errorStage.execute(context);
        
        return context.getGraph();
    }
}
```

### Stage Responsibilities

| Stage | Responsibility | Idempotent |
|-------|---------------|------------|
| **RuntimeNodeStage** | Create runtime-discovered nodes (zoom=5) | ✅ |
| **RuntimeEdgeStage** | Create RUNTIME_CALL and PRODUCES edges | ✅ |
| **DurationStage** | Calculate method execution durations | ✅ |
| **CheckpointStage** | Attach checkpoint data to nodes | ✅ |
| **AsyncHopStage** | Stitch async message flows (Kafka, etc.) | ✅ |
| **ErrorStage** | Attach error information to nodes | ✅ |

### MergeContext - Shared State

```java
private static class MergeContext {
    private final CoreGraph graph;
    private final List<RuntimeEvent> events;
    private final Map<String, RuntimeEvent> spanToEvent; // Pre-computed index
    
    CoreGraph getGraph();
    List<RuntimeEvent> getEvents();
    RuntimeEvent getEventBySpan(String spanId);
}
```

### Benefits
✅ **Single Responsibility** - Each stage does one thing  
✅ **Testable** - Test each stage independently  
✅ **Maintainable** - Easy to understand and modify  
✅ **Extensible** - Add new stages without affecting others  
✅ **Idempotent** - Each stage can run multiple times safely  

---

## ❌ Interfaces NOT Needed (Yet)

### CoreGraph
**Why not:** Simple POJO, no alternative implementations needed.  
**Decision:** Keep as concrete class. Add interface only if multiple implementations arise.

### StaticGraphLoader
**Why not:** Single, generic loader. No planned alternatives.  
**Decision:** Keep as concrete class. JSON is the standard format.

### FlowExtractor
**Why not:** One canonical flow extraction algorithm.  
**Decision:** Keep as concrete class unless alternative algorithms needed.

### MergeEngine
**Why not:** Only one implementation exists.  
**Decision:** Keep as concrete class. The Pipeline Pattern provides enough modularity.

---

## 🎯 Pattern Decisions

### ✅ **Metadata Support - IMPLEMENTED**

**Problem:** Need to store runtime data (durations, checkpoints, errors) on nodes.

**Solution:** Added `Map<String, Object> metadata` field to CoreNode.

```java
// Store duration
node.setMetadata("durationMs", 42L);

// Store checkpoints
Map<String, Object> checkpoints = new HashMap<>();
checkpoints.put("cart_total", 250.50);
node.setMetadata("checkpoints", checkpoints);

// Store error
node.setMetadata("error", errorData);
node.setMetadata("status", "FAILED");

// Retrieve metadata
Long duration = (Long) node.getMetadata("durationMs");
Map<String, Object> allMeta = node.getAllMetadata();
```

**Benefits:**
✅ Simple and flexible
✅ No need for Decorator Pattern complexity
✅ Works with existing Pipeline Pattern
✅ Easy to extend with new metadata types

### ❌ **Decorator Pattern - NOT NEEDED**

**Why not:**
- Metadata field in CoreNode is simpler and sufficient
- No need for immutable node decoration
- Decorator adds unnecessary complexity for current use case
- Pipeline Pattern already provides clean separation

**When to consider:**
- If nodes need to be truly immutable
- If you need behavior decoration (not just data)
- If you have 10+ types of decorators with complex interactions

### ❌ **Chain of Responsibility - NOT NEEDED**

**Why not:**
- Only 6 merge stages (manageable without chain)
- No conditional execution needed (all stages run)
- No dynamic pipeline ordering required
- Pipeline Pattern is cleaner for sequential processing

**When to consider:**
- When you have 15+ merge stages
- When stages need skip/short-circuit logic
- When pipeline order needs to be configurable at runtime
- When stages need to decide the next stage dynamically

---

## 🎯 Recommended Future Patterns

### A. Builder Pattern - CoreNode Construction

**Current:**
```java
CoreNode node = new CoreNode(id, name, type, serviceId, visibility);
node.setZoomLevel(zoom);
node.setMetadata("duration", 42);
```

**Future:**
```java
CoreNode node = CoreNode.builder()
    .id("node-123")
    .name("MyService")
    .type(NodeType.CLASS)
    .serviceId("svc-1")
    .visibility(Visibility.PUBLIC)
    .zoomLevel(2)
    .metadata("version", "1.0")
    .metadata("owner", "team-a")
    .build();
```

**Benefits:** Cleaner construction, optional fields, better readability

**Status:** Not urgent - current constructor pattern works fine

---

## 📊 Design Metrics

### Before Refactoring
- ❌ Monolithic MergeEngine method (200+ lines)
- ❌ Tight coupling to Neo4j exporter
- ❌ No event handler extensibility
- ❌ Hard to test individual merge steps
- ❌ No metadata storage for runtime data

### After Refactoring
- ✅ 6 independent merge stages (20-40 lines each)
- ✅ Interface-based exporters (Strategy Pattern)
- ✅ Modular event handlers (Strategy Pattern)
- ✅ Factory-based creation (Factory Pattern)
- ✅ Each component independently testable
- ✅ Metadata support for durations, checkpoints, errors

---

## 🚀 Extension Points

### Adding a New Export Format

```java
// 1. Implement interface
public class CSVExporter implements GraphExporter {
    @Override
    public String export(CoreGraph graph) {
        return "id,name,type\n" + /* CSV generation */;
    }
}

// 2. Register in factory
ExporterFactory.registerExporter(Format.CSV, CSVExporter.class);

// 3. Use it
String csv = engine.export(graph, ExporterFactory.Format.CSV);
```

### Adding a New Event Type

```java
// 1. Add to EventType enum
public enum EventType {
    // ...existing...
    DB_QUERY,
    HTTP_CALL,
    REDIS_OPERATION
}

// 2. Implement handler
public class DBQueryHandler implements RuntimeEventHandler {
    @Override
    public void handle(RuntimeEvent event, CoreGraph graph) {
        String query = (String) event.getDataValue("sql");
        long duration = (long) event.getDataValue("duration");
        // Process DB query event
    }
    
    @Override
    public boolean canHandle(EventType eventType) {
        return eventType == EventType.DB_QUERY;
    }
}

// 3. Register handler
registry.registerHandler(EventType.DB_QUERY, new DBQueryHandler());
```

### Adding a New Merge Stage

```java
// 1. Create stage class
private static class PerformanceAnalysisStage {
    void execute(MergeContext context) {
        // Analyze performance hotspots
        // Mark slow methods
        // Calculate critical path
    }
}

// 2. Add to MergeEngine
public class MergeEngine {
    private final PerformanceAnalysisStage performanceStage;
    
    public CoreGraph mergeStaticAndRuntime(...) {
        // ...existing stages...
        performanceStage.execute(context);
        return context.getGraph();
    }
}
```

---

## ✅ Compliance Checklist

- [x] **Strategy Pattern** - GraphExporter interface with multiple implementations
- [x] **Strategy Pattern** - RuntimeEventHandler interface with event-specific handlers
- [x] **Factory Pattern** - ExporterFactory for centralized exporter creation
- [x] **Registry Pattern** - RuntimeEventHandlerRegistry for handler management
- [x] **Pipeline Pattern** - MergeEngine with independent stages
- [x] **Single Responsibility** - Each stage/handler does one thing
- [x] **Open/Closed Principle** - Open for extension (new handlers), closed for modification
- [x] **Dependency Inversion** - Depend on abstractions (interfaces), not concrete classes
- [x] **Interface Segregation** - Small, focused interfaces
- [x] **Framework Agnostic** - Pure Java, no external dependencies

---

## 📚 References

- **Design Patterns:** Gang of Four (Strategy, Factory, Chain of Responsibility)
- **Clean Architecture:** Robert C. Martin (SOLID principles)
- **Effective Java:** Joshua Bloch (Builder pattern, item construction)

---

**End of Document**

