package com.flow.core.graph;

public enum EdgeType {
    CALL,
    HANDLES,
    PRODUCES,
    CONSUMES,
    BELONGS_TO,
    DEFINES,
    RUNTIME_CALL,
    DEPENDS_ON,
    FLOWS_TO;

    public static EdgeType fromString(String type) {
        try {
            return valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown edge type: " + type);
        }
    }
}

