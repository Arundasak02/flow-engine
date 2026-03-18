package com.flow.core.runtime;

import com.flow.core.graph.CoreGraph;
import com.flow.core.ingest.MergeEngine;
import com.flow.core.ingest.RuntimeEventIngestor;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Main orchestrator for runtime event processing.
 *
 * Workflow:
 * 1. Runtime plugin emits RuntimeEvents
 * 2. RuntimeEventIngestor buffers events by traceId
 * 3. MergeEngine merges static + runtime graphs
 * 4. RuntimeFlowExtractor generates ordered execution paths
 * 5. RuntimeFlow sent to UI for animation
 */
public class RuntimeEngine {

    private final RuntimeEventIngestor ingestor;
    private final MergeEngine mergeEngine;
    private final RuntimeFlowExtractor flowExtractor;

    public RuntimeEngine() {
        this.ingestor = new RuntimeEventIngestor();
        this.mergeEngine = new MergeEngine();
        this.flowExtractor = new RuntimeFlowExtractor();
    }

    public RuntimeEngine(RuntimeEventIngestor ingestor, MergeEngine mergeEngine,
                        RuntimeFlowExtractor flowExtractor) {
        this.ingestor = Objects.requireNonNull(ingestor, "RuntimeEventIngestor cannot be null");
        this.mergeEngine = Objects.requireNonNull(mergeEngine, "MergeEngine cannot be null");
        this.flowExtractor = Objects.requireNonNull(flowExtractor, "RuntimeFlowExtractor cannot be null");
    }

    /**
     * Accept a single runtime event.
     */
    public void acceptEvent(RuntimeEvent event) {
        ingestor.ingest(event);
    }

    /**
     * Accept multiple runtime events.
     */
    public void acceptEvents(List<RuntimeEvent> events) {
        ingestor.ingest(events);
    }

    /**
     * Process a trace: merge with static graph and extract flow.
     */
    public RuntimeFlow processTrace(String traceId, CoreGraph staticGraph) {
        Objects.requireNonNull(traceId, "traceId cannot be null");
        Objects.requireNonNull(staticGraph, "Static graph cannot be null");

        List<RuntimeEvent> events = ingestor.getEventsByTrace(traceId);
        if (events.isEmpty()) {
            return new RuntimeFlow(traceId, List.of());
        }

        CoreGraph enrichedGraph = mergeEngine.mergeStaticAndRuntime(staticGraph, events);
        RuntimeFlow flow = flowExtractor.extractByTraceId(enrichedGraph, events);

        return flow;
    }

    /**
     * Get all available trace IDs.
     */
    public Set<String> getAllTraceIds() {
        return ingestor.getAllTraceIds();
    }

    /**
     * Clear a specific trace from memory.
     */
    public void clearTrace(String traceId) {
        ingestor.clearTrace(traceId);
    }

    /**
     * Expire old traces to free memory.
     */
    public void expireOldTraces() {
        ingestor.expireOldTraces();
    }

    /**
     * Get the event ingestor (for advanced usage).
     */
    public RuntimeEventIngestor getIngestor() {
        return ingestor;
    }

    /**
     * Get the merge engine (for advanced usage).
     */
    public MergeEngine getMergeEngine() {
        return mergeEngine;
    }

    /**
     * Get the flow extractor (for advanced usage).
     */
    public RuntimeFlowExtractor getFlowExtractor() {
        return flowExtractor;
    }
}

