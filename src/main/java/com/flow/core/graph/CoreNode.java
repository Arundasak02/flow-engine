package com.flow.core.graph;

import java.util.Objects;

public class CoreNode {

    private final String id;
    private final String name;
    private final NodeType type;
    private int zoomLevel;
    private final String serviceId;
    private final Visibility visibility;

    public CoreNode(String id, String name, NodeType type, String serviceId, Visibility visibility) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        this.name = Objects.requireNonNull(name, "Node name cannot be null");
        this.type = Objects.requireNonNull(type, "Node type cannot be null");
        this.serviceId = serviceId;
        this.visibility = Objects.requireNonNull(visibility, "Visibility cannot be null");
        this.zoomLevel = -1;
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

