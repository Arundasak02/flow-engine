package com.flow.core.zoom;

/**
 * Defines the zoom levels in the Flow system.
 *
 * Level 1: BUSINESS - endpoints, topics, business operations
 * Level 2: SERVICE - services, classes
 * Level 3: PUBLIC - public methods, exported functions
 * Level 4: PRIVATE - private/internal methods, helpers
 * Level 5: RUNTIME - actual runtime execution nodes, stack traces
 */
public enum ZoomLevel {
    BUSINESS(1, "Business"),
    SERVICE(2, "Service"),
    PUBLIC(3, "Public"),
    PRIVATE(4, "Private"),
    RUNTIME(5, "Runtime");

    private final int level;
    private final String displayName;

    ZoomLevel(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get ZoomLevel by numeric level.
     *
     * @param level the zoom level (1-5)
     * @return the ZoomLevel enum value
     * @throws IllegalArgumentException if level is invalid
     */
    public static ZoomLevel fromLevel(int level) {
        for (ZoomLevel z : values()) {
            if (z.level == level) {
                return z;
            }
        }
        throw new IllegalArgumentException("Invalid zoom level: " + level);
    }
}

