package com.flow.core.ingest;

import com.flow.core.graph.*;
import com.flow.core.runtime.EventType;
import com.flow.core.runtime.RuntimeEvent;
import com.flow.core.runtime.RuntimeTraceBuffer;

import java.util.*;

/**
 * Ingests runtime execution events into the RuntimeTraceBuffer.
 *
 * Accepts RuntimeEvent objects and buffers them by traceId.
 * Does NOT modify the graph directly - that's MergeEngine's job.
 */
public class RuntimeEventIngestor {

    private final RuntimeTraceBuffer buffer;

    public RuntimeEventIngestor() {
        this.buffer = new RuntimeTraceBuffer();
    }

    public RuntimeEventIngestor(RuntimeTraceBuffer buffer) {
        this.buffer = Objects.requireNonNull(buffer, "RuntimeTraceBuffer cannot be null");
    }

    public void ingest(RuntimeEvent event) {
        Objects.requireNonNull(event, "RuntimeEvent cannot be null");
        buffer.addEvent(event);
    }

    public void ingest(List<RuntimeEvent> events) {
        Objects.requireNonNull(events, "Events list cannot be null");
        buffer.addEvents(events);
    }

    public List<RuntimeEvent> getEventsByTrace(String traceId) {
        return buffer.getEventsByTrace(traceId);
    }

    public Set<String> getAllTraceIds() {
        return buffer.getAllTraceIds();
    }

    public void clearTrace(String traceId) {
        buffer.clearTrace(traceId);
    }

    public void expireOldTraces() {
        buffer.expireOldTraces();
    }

    public RuntimeTraceBuffer getBuffer() {
        return buffer;
    }

    // Legacy method for backward compatibility with Map<String, Object> format
    @Deprecated
    public void ingest(List<Map<String, Object>> eventMaps, CoreGraph graph) {
        Objects.requireNonNull(eventMaps, "Events list cannot be null");

        List<RuntimeEvent> events = new ArrayList<>();
        for (Map<String, Object> eventMap : eventMaps) {
            RuntimeEvent event = convertMapToEvent(eventMap);
            if (event != null) {
                events.add(event);
            }
        }

        ingest(events);
    }

    @SuppressWarnings("unchecked")
    private RuntimeEvent convertMapToEvent(Map<String, Object> eventMap) {
        try {
            String traceId = (String) eventMap.get("traceId");
            Long timestamp = getLongValue(eventMap.get("timestamp"));
            String typeStr = (String) eventMap.get("type");
            String nodeId = (String) eventMap.get("nodeId");
            String spanId = (String) eventMap.get("spanId");
            String parentSpanId = (String) eventMap.get("parentSpanId");
            Map<String, Object> data = (Map<String, Object>) eventMap.get("data");

            if (traceId == null || timestamp == null || typeStr == null || nodeId == null) {
                return null;
            }

            EventType type = EventType.valueOf(typeStr.toUpperCase());

            return new RuntimeEvent(traceId, timestamp, type, nodeId, spanId, parentSpanId, data);
        } catch (Exception e) {
            return null;
        }
    }

    private Long getLongValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

