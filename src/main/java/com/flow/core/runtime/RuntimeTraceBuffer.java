package com.flow.core.runtime;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Buffers runtime events in memory, grouped by traceId.
 * Automatically expires old traces.
 */
public class RuntimeTraceBuffer {

    private final Map<String, List<RuntimeEvent>> eventsByTrace;
    private final Map<String, Long> traceLastUpdated;
    private final long expirationTimeMs;

    public RuntimeTraceBuffer() {
        this(300000); // 5 minutes default
    }

    public RuntimeTraceBuffer(long expirationTimeMs) {
        this.eventsByTrace = new ConcurrentHashMap<>();
        this.traceLastUpdated = new ConcurrentHashMap<>();
        this.expirationTimeMs = expirationTimeMs;
    }

    public void addEvent(RuntimeEvent event) {
        Objects.requireNonNull(event, "RuntimeEvent cannot be null");

        String traceId = event.getTraceId();
        eventsByTrace.computeIfAbsent(traceId, k -> new ArrayList<>()).add(event);
        traceLastUpdated.put(traceId, System.currentTimeMillis());
    }

    public void addEvents(List<RuntimeEvent> events) {
        events.forEach(this::addEvent);
    }

    public List<RuntimeEvent> getEventsByTrace(String traceId) {
        List<RuntimeEvent> events = eventsByTrace.get(traceId);
        if (events == null) {
            return Collections.emptyList();
        }

        return events.stream()
                .sorted(Comparator.comparingLong(RuntimeEvent::getTimestamp))
                .collect(Collectors.toList());
    }

    public Set<String> getAllTraceIds() {
        return new HashSet<>(eventsByTrace.keySet());
    }

    public void clearTrace(String traceId) {
        eventsByTrace.remove(traceId);
        traceLastUpdated.remove(traceId);
    }

    public void expireOldTraces() {
        long now = System.currentTimeMillis();
        List<String> expiredTraces = new ArrayList<>();

        for (Map.Entry<String, Long> entry : traceLastUpdated.entrySet()) {
            if (now - entry.getValue() > expirationTimeMs) {
                expiredTraces.add(entry.getKey());
            }
        }

        expiredTraces.forEach(this::clearTrace);
    }

    public int getTraceCount() {
        return eventsByTrace.size();
    }

    public int getEventCount(String traceId) {
        List<RuntimeEvent> events = eventsByTrace.get(traceId);
        return events != null ? events.size() : 0;
    }

    public void clear() {
        eventsByTrace.clear();
        traceLastUpdated.clear();
    }
}

