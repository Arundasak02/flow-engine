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
 * FlowCoreEngine is the main orchestration class for the Flow Core library.
 *
 * It coordinates the full pipeline:
 * 1. Load static graph (flow.json)
 * 2. Assign zoom levels
 * 3. Validate graph structure
 * 4. Extract flows per endpoint
 * 5. Load runtime events (optional)
 * 6. Merge static + runtime data
 * 7. Export to Neo4j or GEF format
 *
 * Pure Java, no Spring, no HTTP, no frameworks.
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
     * Main entry point: process static graph JSON and optional runtime events.
     *
     * @param staticGraphJson the flow.json content as a string
     * @param runtimeEvents optional list of runtime event data
     * @return an enriched CoreGraph ready for export
     * @throws IllegalArgumentException if graph validation fails
     */
    public CoreGraph process(String staticGraphJson, List<Map<String, Object>> runtimeEvents) {
        // 1. Load static graph
        CoreGraph graph = staticGraphLoader.load(staticGraphJson);

        // 2. Assign zoom levels
        zoomEngine.assignZoomLevels(graph);

        // 3. Validate structure (after zoom assignment)
        graphValidator.validate(graph);

        // 4. Extract flows per endpoint
        flowExtractor.extractFlows(graph);

        // 5. Merge runtime events if provided
        if (runtimeEvents != null && !runtimeEvents.isEmpty()) {
            runtimeEventIngestor.ingest(runtimeEvents, graph);
            mergeEngine.merge(graph);
        }

        return graph;
    }

    /**
     * Process static graph only (no runtime events).
     *
     * @param staticGraphJson the flow.json content as a string
     * @return an enriched CoreGraph ready for export
     */
    public CoreGraph process(String staticGraphJson) {
        return process(staticGraphJson, null);
    }

    /**
     * Export the enriched graph to Neo4j Cypher format.
     *
     * @param graph the enriched CoreGraph
     * @return Cypher query string
     */
    public String exportToNeo4j(CoreGraph graph) {
        return neo4jExporter.export(graph);
    }

    /**
     * Export the enriched graph to GEF format.
     *
     * @param graph the enriched CoreGraph
     * @return GEF JSON string
     */
    public String exportToGEF(CoreGraph graph) {
        return gefExporter.export(graph);
    }
}

