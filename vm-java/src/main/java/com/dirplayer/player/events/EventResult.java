package com.dirplayer.player.events;

/**
 * Result of event dispatch indicating whether the event was handled.
 * Port of Rust event result handling pattern.
 */
public class EventResult {
    public final boolean handled;
    public final boolean passed;
    public final int resultRef;

    private EventResult(boolean handled, boolean passed, int resultRef) {
        this.handled = handled;
        this.passed = passed;
        this.resultRef = resultRef;
    }

    /**
     * Create a result indicating the event was handled and not passed.
     */
    public static EventResult handled() {
        return new EventResult(true, false, 0);
    }

    /**
     * Create a result indicating the event was passed to the next handler.
     */
    public static EventResult passed() {
        return new EventResult(false, true, 0);
    }

    /**
     * Create a result indicating the event was not handled.
     */
    public static EventResult notHandled() {
        return new EventResult(false, false, 0);
    }

    /**
     * Create a void result (successful execution with no return value).
     */
    public static EventResult voidResult() {
        return new EventResult(true, false, 0);
    }

    /**
     * Create a result with a datum reference.
     */
    public static EventResult withResult(int datumRef) {
        return new EventResult(true, false, datumRef);
    }
}
