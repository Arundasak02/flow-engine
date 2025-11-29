package com.flow.core.graph;

public enum NodeType {
    ENDPOINT,
    TOPIC,
    SERVICE,
    CLASS,
    METHOD,
    PRIVATE_METHOD,
    INTERFACE,
    FIELD,
    CONSTRUCTOR;

    public static NodeType fromString(String type) {
        try {
            return valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown node type: " + type);
        }
    }

    public boolean isPublicByDefault() {
        return this != PRIVATE_METHOD;
    }
}

