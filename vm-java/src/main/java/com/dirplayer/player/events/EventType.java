package com.dirplayer.player.events;

/**
 * Director event types that can be dispatched to scripts.
 * These correspond to the event handlers in Lingo behavior scripts.
 */
public enum EventType {
    // ========== Mouse Events ==========
    /**
     * Called when the mouse button is pressed down.
     */
    MOUSE_DOWN("mouseDown"),

    /**
     * Called when the mouse button is released.
     */
    MOUSE_UP("mouseUp"),

    /**
     * Called when the mouse button is released over the same sprite it was pressed on.
     */
    MOUSE_UP_OUTSIDE("mouseUpOutside"),

    /**
     * Called when the mouse pointer enters a sprite's bounding box.
     */
    MOUSE_ENTER("mouseEnter"),

    /**
     * Called when the mouse pointer leaves a sprite's bounding box.
     */
    MOUSE_LEAVE("mouseLeave"),

    /**
     * Called while the mouse pointer is within a sprite's bounding box.
     */
    MOUSE_WITHIN("mouseWithin"),

    /**
     * Called when right mouse button is pressed.
     */
    RIGHT_MOUSE_DOWN("rightMouseDown"),

    /**
     * Called when right mouse button is released.
     */
    RIGHT_MOUSE_UP("rightMouseUp"),

    // ========== Keyboard Events ==========
    /**
     * Called when a key is pressed.
     */
    KEY_DOWN("keyDown"),

    /**
     * Called when a key is released.
     */
    KEY_UP("keyUp"),

    // ========== Frame Events ==========
    /**
     * Called before the frame is rendered, before exitFrame of the previous frame.
     */
    PREPARE_FRAME("prepareFrame"),

    /**
     * Called when entering a new frame.
     */
    ENTER_FRAME("enterFrame"),

    /**
     * Called when exiting a frame.
     */
    EXIT_FRAME("exitFrame"),

    /**
     * Called during frame stepping in the score.
     */
    STEP_FRAME("stepFrame"),

    // ========== Sprite Lifecycle Events ==========
    /**
     * Called when a sprite first appears on the stage.
     */
    BEGIN_SPRITE("beginSprite"),

    /**
     * Called when a sprite is about to leave the stage.
     */
    END_SPRITE("endSprite"),

    // ========== Movie Events ==========
    /**
     * Called before the movie starts playing.
     */
    PREPARE_MOVIE("prepareMovie"),

    /**
     * Called when the movie starts playing.
     */
    START_MOVIE("startMovie"),

    /**
     * Called when the movie stops playing.
     */
    STOP_MOVIE("stopMovie"),

    // ========== Idle Event ==========
    /**
     * Called during idle time when no other events are being processed.
     */
    IDLE("idle"),

    // ========== Timeout Events ==========
    /**
     * Called when a timeout is triggered.
     */
    TIMEOUT("timeout"),

    // ========== Streaming Events ==========
    /**
     * Called when streaming status changes.
     */
    STREAM_STATUS("streamStatus"),

    // ========== Behavior Events ==========
    /**
     * Called to get property description list for behavior parameters.
     */
    GET_PROPERTY_DESCRIPTION_LIST("getPropertyDescriptionList"),

    /**
     * Called to get behavior description.
     */
    GET_BEHAVIOR_DESCRIPTION("getBehaviorDescription"),

    /**
     * Called to run property dialog for behavior parameters.
     */
    RUN_PROPERTY_DIALOG("runPropertyDialog"),

    // ========== Cue Point Events ==========
    /**
     * Called when a cue point is passed in a sound or video.
     */
    CUE_POINT_PASSED("cuePassed"),

    // ========== Net Events ==========
    /**
     * Called when a download operation completes.
     */
    DOWNLOAD_COMPLETE("downloadComplete"),

    // ========== Custom Event ==========
    /**
     * Custom event with user-defined name.
     */
    CUSTOM("custom");

    private final String handlerName;

    EventType(String handlerName) {
        this.handlerName = handlerName;
    }

    /**
     * Get the Lingo handler name for this event.
     */
    public String getHandlerName() {
        return handlerName;
    }

    /**
     * Find an EventType by its handler name.
     * Returns CUSTOM if not found.
     */
    public static EventType fromHandlerName(String name) {
        for (EventType type : values()) {
            if (type.handlerName.equals(name)) {
                return type;
            }
        }
        return CUSTOM;
    }

    /**
     * Check if this is a mouse event.
     */
    public boolean isMouseEvent() {
        return this == MOUSE_DOWN || this == MOUSE_UP || this == MOUSE_UP_OUTSIDE ||
               this == MOUSE_ENTER || this == MOUSE_LEAVE || this == MOUSE_WITHIN ||
               this == RIGHT_MOUSE_DOWN || this == RIGHT_MOUSE_UP;
    }

    /**
     * Check if this is a keyboard event.
     */
    public boolean isKeyboardEvent() {
        return this == KEY_DOWN || this == KEY_UP;
    }

    /**
     * Check if this is a frame event.
     */
    public boolean isFrameEvent() {
        return this == PREPARE_FRAME || this == ENTER_FRAME ||
               this == EXIT_FRAME || this == STEP_FRAME;
    }

    /**
     * Check if this is a sprite lifecycle event.
     */
    public boolean isSpriteLifecycleEvent() {
        return this == BEGIN_SPRITE || this == END_SPRITE;
    }

    /**
     * Check if this is a movie lifecycle event.
     */
    public boolean isMovieLifecycleEvent() {
        return this == PREPARE_MOVIE || this == START_MOVIE || this == STOP_MOVIE;
    }

    /**
     * Check if this is a system event (dispatched to timeouts).
     */
    public boolean isSystemEvent() {
        return this == PREPARE_MOVIE || this == START_MOVIE || this == STOP_MOVIE ||
               this == PREPARE_FRAME || this == EXIT_FRAME;
    }
}
