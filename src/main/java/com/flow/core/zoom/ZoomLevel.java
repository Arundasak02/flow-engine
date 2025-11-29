package com.flow.core.zoom;

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

    public static ZoomLevel fromLevel(int level) {
        for (ZoomLevel z : values()) {
            if (z.level == level) {
                return z;
            }
        }
        throw new IllegalArgumentException("Invalid zoom level: " + level);
    }
}

