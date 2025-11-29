package com.flow.core.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RuntimeEvent {

    private final String traceId;
    private final long timestamp;
    private final EventType type;
    private final String nodeId;
    private final String spanId;
    private final String parentSpanId;
    private final Map<String, Object> data;

    public RuntimeEvent(String traceId, long timestamp, EventType type, String nodeId,
                       String spanId, String parentSpanId, Map<String, Object> data) {
        this.traceId = Objects.requireNonNull(traceId, "traceId cannot be null");
        this.timestamp = timestamp;
        this.type = Objects.requireNonNull(type, "EventType cannot be null");
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId cannot be null");
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }

    public String getTraceId() {
        return traceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public EventType getType() {
        return type;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    public Object getDataValue(String key) {
        return data.get(key);
    }

    @Override
    public String toString() {
        return "RuntimeEvent{" +
                "traceId='" + traceId + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", nodeId='" + nodeId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", parentSpanId='" + parentSpanId + '\'' +
                ", data=" + data +
                '}';
    }
}

