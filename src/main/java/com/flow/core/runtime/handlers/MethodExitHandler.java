package com.flow.core.runtime.handlers;

import com.flow.core.graph.CoreGraph;
import com.flow.core.runtime.*;

/**
 * Handles METHOD_EXIT events for duration calculation.
 * Note: Actual duration calculation requires correlation with METHOD_ENTER,
 * which is handled by the DurationStage in MergeEngine.
 */
public class MethodExitHandler implements RuntimeEventHandler {

    @Override
    public void handle(RuntimeEvent event, CoreGraph graph) {
        // Duration calculation is handled by DurationStage
        // This handler is a placeholder for future per-event processing
    }

    @Override
    public boolean canHandle(EventType eventType) {
        return eventType == EventType.METHOD_EXIT;
    }
}

