package com.flow.core.export;

import com.flow.core.graph.CoreGraph;

/**
 * Interface for graph export strategies.
 *
 * Implementations can export graphs to different formats:
 * - Neo4j Cypher
 * - GraphViz DOT
 * - CSV
 * - JSON (GEF, custom UI format)
 * - Protobuf
 * - NDJSON
 *
 * This allows:
 * - Factory-based exporter selection
 * - Easy plugin system
 * - Clean testability
 * - Dependency injection in Flow Core Service
 */
public interface GraphExporter {

    /**
     * Export the graph to a specific format.
     *
     * @param graph the graph to export
     * @return exported string representation
     */
    String export(CoreGraph graph);

    /**
     * Get the format name for this exporter.
     *
     * @return format identifier (e.g., "neo4j", "gef", "graphviz")
     */
    default String getFormat() {
        return this.getClass().getSimpleName().replace("Exporter", "").toLowerCase();
    }
}

