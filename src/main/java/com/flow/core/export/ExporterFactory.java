package com.flow.core.export;

import java.util.*;

/**
 * Factory for creating graph exporters.
 *
 * Supports:
 * - Factory-based selection by format name
 * - Registration of custom exporters
 * - Easy extension for new formats
 */
public class ExporterFactory {

    public enum Format {
        NEO4J,
        GRAPHVIZ,
        CSV,
        JSON
    }

    private static final Map<Format, Class<? extends GraphExporter>> EXPORTERS = new HashMap<>();

    static {
        EXPORTERS.put(Format.NEO4J, Neo4jExporter.class);
        // Future exporters:
        // EXPORTERS.put(Format.GRAPHVIZ, GraphVizExporter.class);
        // EXPORTERS.put(Format.CSV, CSVExporter.class);
        // EXPORTERS.put(Format.JSON, JSONExporter.class);
    }

    /**
     * Get an exporter instance for the specified format.
     */
    public static GraphExporter getExporter(Format format) {
        Class<? extends GraphExporter> exporterClass = EXPORTERS.get(format);
        if (exporterClass == null) {
            throw new IllegalArgumentException("No exporter registered for format: " + format);
        }

        try {
            return exporterClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate exporter: " + exporterClass.getName(), e);
        }
    }

    /**
     * Register a custom exporter.
     */
    public static void registerExporter(Format format, Class<? extends GraphExporter> exporterClass) {
        EXPORTERS.put(format, exporterClass);
    }

    /**
     * Get all supported formats.
     */
    public static Set<Format> getSupportedFormats() {
        return EXPORTERS.keySet();
    }
}

