package com.flow.core.graph;

import java.util.*;

public class CoreNode {

    private final String id;
    private final String name;
    private final NodeType type;
    private int zoomLevel;
    private final String serviceId;
    private final Visibility visibility;
    private final Map<String, Object> metadata;

    public CoreNode(String id, String name, NodeType type, String serviceId, Visibility visibility) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        this.name = Objects.requireNonNull(name, "Node name cannot be null");
        this.type = Objects.requireNonNull(type, "Node type cannot be null");
        this.serviceId = serviceId;
        this.visibility = Objects.requireNonNull(visibility, "Visibility cannot be null");
        this.zoomLevel = -1;
        this.metadata = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public NodeType getType() {
        return type;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        validateZoomLevel(zoomLevel);
        this.zoomLevel = zoomLevel;
    }

    private void validateZoomLevel(int zoomLevel) {
        if (zoomLevel < 1 || zoomLevel > 5) {
            throw new IllegalArgumentException("Zoom level must be between 1 and 5");
        }
    }

    public String getServiceId() {
        return serviceId;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public boolean isPublic() {
        return visibility.isPublic();
    }

    /**
     * Get metadata value by key.
     * Used for storing runtime data like durations, checkpoints, errors.
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Set metadata value.
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Get all metadata as unmodifiable map.
     */
    public Map<String, Object> getAllMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Check if metadata key exists.
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoreNode)) return false;
        CoreNode coreNode = (CoreNode) o;
        return Objects.equals(id, coreNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CoreNode{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", zoomLevel=" + zoomLevel +
                ", serviceId='" + serviceId + '\'' +
                ", visibility=" + visibility +
                '}';
    }
}

