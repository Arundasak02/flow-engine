package com.flow.core.graph;

public enum Visibility {
    PUBLIC,
    PRIVATE,
    PROTECTED,
    PACKAGE_PRIVATE;

    public static Visibility fromString(String visibility) {
        if (visibility == null) {
            return PUBLIC;
        }
        try {
            return valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PUBLIC;
        }
    }

    public boolean isPublic() {
        return this == PUBLIC;
    }
}

