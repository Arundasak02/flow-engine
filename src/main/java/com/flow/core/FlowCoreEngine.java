package com.flow.core;

import com.flow.core.export.GEFExporter;
import com.flow.core.export.Neo4jExporter;
import com.flow.core.flow.FlowExtractor;
import com.flow.core.graph.CoreGraph;
import com.flow.core.graph.GraphValidator;
import com.flow.core.ingest.MergeEngine;
import com.flow.core.ingest.RuntimeEventIngestor;
import com.flow.core.ingest.StaticGraphLoader;
import com.flow.core.zoom.ZoomEngine;

import java.util.List;
import java.util.Map;

/**
 * Main orchestration engine for the Flow Core pipeline.
 *
 * Pipeline stages:
 * 1. Load static graph (JSON) → 2. Assign zoom levels → 3. Validate structure →
 * 4. Extract flows → 5. Merge runtime events (optional) → 6. Export
 */
public class FlowCoreEngine {

    private final StaticGraphLoader staticGraphLoader;
    private final RuntimeEventIngestor runtimeEventIngestor;
    private final GraphValidator graphValidator;
    private final ZoomEngine zoomEngine;
    private final FlowExtractor flowExtractor;
    private final MergeEngine mergeEngine;
    private final Neo4jExporter neo4jExporter;
    private final GEFExporter gefExporter;

    public FlowCoreEngine() {
        this.staticGraphLoader = new StaticGraphLoader();
        this.runtimeEventIngestor = new RuntimeEventIngestor();
        this.graphValidator = new GraphValidator();
        this.zoomEngine = new ZoomEngine();
        this.flowExtractor = new FlowExtractor();
        this.mergeEngine = new MergeEngine();
        this.neo4jExporter = new Neo4jExporter();
        this.gefExporter = new GEFExporter();
    }

    /**
     * Processes static graph JSON with optional runtime events.
     * Returns enriched graph ready for export.
     */
    public CoreGraph process(String staticGraphJson, List<Map<String, Object>> runtimeEvents) {
        CoreGraph graph = loadAndValidateStaticGraph(staticGraphJson);

        if (shouldMergeRuntimeEvents(runtimeEvents)) {
            mergeRuntimeData(graph, runtimeEvents);
        }

        return graph;
    }

    // Pipeline stages: load → zoom → validate → extract flows
    private CoreGraph loadAndValidateStaticGraph(String staticGraphJson) {
        CoreGraph graph = staticGraphLoader.load(staticGraphJson);
        zoomEngine.assignZoomLevels(graph);
        graphValidator.validate(graph);
        flowExtractor.extractFlows(graph);
        return graph;
    }

    private boolean  shouldMergeRuntimeEvents(List<Map<String, Object>> runtimeEvents) {
        return runtimeEvents != null && !runtimeEvents.isEmpty();
    }

    private void mergeRuntimeData(CoreGraph graph, List<Map<String, Object>> runtimeEvents) {
        runtimeEventIngestor.ingest(runtimeEvents, graph);
        mergeEngine.merge(graph);
    }

    public CoreGraph process(String staticGraphJson) {
        return process(staticGraphJson, null);
    }

    public String exportToNeo4j(CoreGraph graph) {
        return neo4jExporter.export(graph);
    }

    public String exportToGEF(CoreGraph graph) {
        return gefExporter.export(graph);
    }
}

