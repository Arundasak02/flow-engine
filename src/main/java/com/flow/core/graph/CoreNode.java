package com.flow.core.graph;

import java.util.Objects;

/**
 * Represents a node in the flow graph.
 *
 * A CoreNode can be at any zoom level:
 * - Level 1: Business (endpoints, topics)
 * - Level 2: Service/Class
 * - Level 3: Public methods
 * - Level 4: Private/internal methods
 * - Level 5: Runtime execution nodes
 */
public class CoreNode {

    private final String id;
    private final String name;
    private final String type; // e.g., "ENDPOINT", "SERVICE", "METHOD", "RUNTIME_CALL"
    private int zoomLevel; // 1-5
    private final String serviceId; // reference to parent service/class
    private final boolean isPublic;

    public CoreNode(String id, String name, String type, String serviceId, boolean isPublic) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        this.name = Objects.requireNonNull(name, "Node name cannot be null");
        this.type = Objects.requireNonNull(type, "Node type cannot be null");
        this.serviceId = serviceId;
        this.isPublic = isPublic;
        this.zoomLevel = -1; // unassigned until zoom engine processes it
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        if (zoomLevel < 1 || zoomLevel > 5) {
            throw new IllegalArgumentException("Zoom level must be between 1 and 5");
        }
        this.zoomLevel = zoomLevel;
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean isPublic() {
        return isPublic;
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
                ", type='" + type + '\'' +
                ", zoomLevel=" + zoomLevel +
                ", serviceId='" + serviceId + '\'' +
                ", isPublic=" + isPublic +
                '}';
    }
}

