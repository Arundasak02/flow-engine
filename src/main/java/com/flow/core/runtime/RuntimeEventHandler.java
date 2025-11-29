package com.flow.core.runtime;

import com.flow.core.graph.CoreGraph;

/**
 * Strategy interface for handling different types of runtime events.
 *
 * This allows modular processing of:
 * - Method calls (enter/exit)
 * - Topic produce/consume
 * - Checkpoints
 * - Errors
 * - Future: DB calls, HTTP spans, gRPC spans, Redis operations
 *
 * Benefits:
 * - Easy to add new event types
 * - Each handler is independently testable
 * - Supports multiple runtime plugins (Python, Node.js, .NET)
 */
public interface RuntimeEventHandler {

    /**
     * Handle a runtime event and modify the graph context.
     *
     * @param event the runtime event to process
     * @param graph the graph being enriched
     */
    void handle(RuntimeEvent event, CoreGraph graph);

    /**
     * Check if this handler can process the given event type.
     *
     * @param eventType the event type to check
     * @return true if this handler supports the event type
     */
    boolean canHandle(EventType eventType);
}

