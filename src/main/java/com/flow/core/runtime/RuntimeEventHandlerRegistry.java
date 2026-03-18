package com.flow.core.runtime;

import com.flow.core.runtime.handlers.*;

import java.util.*;

/**
 * Registry for runtime event handlers.
 * Maps event types to their corresponding handlers using Strategy Pattern.
 *
 * Benefits:
 * - Modular event processing
 * - Easy to add new event types
 * - Supports custom handler registration
 * - Clean separation of concerns
 */
public class RuntimeEventHandlerRegistry {

    private final Map<EventType, RuntimeEventHandler> handlers;

    public RuntimeEventHandlerRegistry() {
        this.handlers = new EnumMap<>(EventType.class);
        registerDefaultHandlers();
    }

    private void registerDefaultHandlers() {
        handlers.put(EventType.METHOD_ENTER, new MethodEnterHandler());
        handlers.put(EventType.METHOD_EXIT, new MethodExitHandler());
        handlers.put(EventType.PRODUCE_TOPIC, new ProduceTopicHandler());
        handlers.put(EventType.CONSUME_TOPIC, new ConsumeTopicHandler());
        handlers.put(EventType.CHECKPOINT, new CheckpointHandler());
        handlers.put(EventType.ERROR, new ErrorHandler());
    }

    /**
     * Get the handler for a specific event type.
     */
    public RuntimeEventHandler getHandler(EventType eventType) {
        RuntimeEventHandler handler = handlers.get(eventType);
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for event type: " + eventType);
        }
        return handler;
    }

    /**
     * Register a custom handler for an event type.
     * Allows overriding default handlers or adding new ones.
     */
    public void registerHandler(EventType eventType, RuntimeEventHandler handler) {
        handlers.put(eventType, handler);
    }

    /**
     * Check if a handler exists for the event type.
     */
    public boolean hasHandler(EventType eventType) {
        return handlers.containsKey(eventType);
    }

    /**
     * Get all registered event types.
     */
    public Set<EventType> getSupportedEventTypes() {
        return handlers.keySet();
    }
}

