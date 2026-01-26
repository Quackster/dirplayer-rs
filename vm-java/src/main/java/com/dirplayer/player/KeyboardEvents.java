package com.dirplayer.player;

import com.dirplayer.player.events.EventDispatcher;
import com.dirplayer.player.script.ScriptInstanceRef;
import com.dirplayer.SimpleLogger;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keyboard event handling for the Director player.
 * Handles key down/up events, editable field input, and keyboard focus.
 * Port of Rust keyboard_events.rs.
 */
public class KeyboardEvents {
    private static final SimpleLogger logger = SimpleLogger.getLogger(KeyboardEvents.class);

    private KeyboardEvents() {
        // Utility class - prevent instantiation
    }

    /**
     * Find the next focusable sprite (editable field) after the given sprite ID.
     * Used for Tab key navigation between editable text fields.
     * @param player The DirPlayer instance
     * @param after The sprite ID to search after
     * @return The sprite ID of the next focusable field, or -1 if none found
     */
    public static int getNextFocusSpriteId(DirPlayer player, int after) {
        int channelCount = player.movie.score.getChannelCount();
        for (int spriteId = after + 1; spriteId <= channelCount; spriteId++) {
            Sprite sprite = player.movie.score.getSprite((short) spriteId);
            if (sprite == null) {
                continue;
            }

            CastMemberRef memberRef = sprite.memberRef;
            if (memberRef == null || !memberRef.isValid()) {
                continue;
            }

            CastMember member = player.movie.castManager.findMemberByRef(memberRef);
            if (member == null) {
                continue;
            }

            // Check if this is an editable field
            // In Director, fields (MemberType.Button or MemberType.RTE) can be editable
            if (member.isField()) {
                // Check if editable via sprite's editableText property or member properties
                if (sprite.editableText) {
                    return spriteId;
                }
            }
        }
        return -1;
    }

    /**
     * Handle a key down event for the player.
     * Updates keyboard state, handles editable field input, and dispatches keyDown event.
     * @param player The DirPlayer instance
     * @param eventDispatcher The event dispatcher for sending events
     * @param key The key string (e.g., "a", "Backspace", "Tab")
     * @param code The JavaScript key code
     * @throws ScriptError if a script error occurs during event dispatch
     */
    public static void playerKeyDown(DirPlayer player, EventDispatcher eventDispatcher,
                                     String key, int code) throws ScriptError {
        if (!player.isPlaying) {
            return;
        }

        // Update keyboard state
        player.keyboardManager.keyDown(key, code);

        // Get script instances for keyboard focus sprite
        List<ScriptInstanceRef> instanceIds = null;

        if (player.keyboardFocusSprite != -1) {
            int spriteId = player.keyboardFocusSprite;
            Sprite sprite = player.movie.score.getSprite((short) spriteId);

            if (sprite != null) {
                // Build instance list from sprite's script instances
                instanceIds = new ArrayList<>();
                for (int id : sprite.scriptInstanceList) {
                    instanceIds.add(new ScriptInstanceRef(id));
                }

                // Handle editable field input
                CastMemberRef memberRef = sprite.memberRef;
                if (memberRef != null && memberRef.isValid()) {
                    CastMember member = player.movie.castManager.findMemberByRef(memberRef);
                    if (member != null && member.isField()) {
                        // Check if editable
                        if (sprite.editableText) {
                            handleFieldInput(player, member, key, spriteId);
                        }
                    }
                }
            }
        }

        // Dispatch keyDown event
        if (eventDispatcher != null) {
            eventDispatcher.dispatchTargetedEvent("keyDown", Collections.emptyList(), instanceIds);
        }
    }

    /**
     * Handle input to an editable field.
     * Processes backspace, tab, and regular character input.
     * @param player The DirPlayer instance
     * @param member The cast member being edited
     * @param key The key string
     * @param spriteId The sprite ID containing the field
     */
    private static void handleFieldInput(DirPlayer player, CastMember member,
                                         String key, int spriteId) {
        if ("Backspace".equals(key)) {
            // Remove last character
            if (member.text != null && !member.text.isEmpty()) {
                member.text = member.text.substring(0, member.text.length() - 1);
            }
        } else if ("Tab".equals(key)) {
            // Move focus to next editable field
            int nextFocusSpriteId = getNextFocusSpriteId(player, spriteId);
            player.keyboardFocusSprite = nextFocusSpriteId;
        } else if (key.length() == 1) {
            // Append single character
            if (member.text == null) {
                member.text = key;
            } else {
                member.text = member.text + key;
            }
        }
    }

    /**
     * Handle a key up event for the player.
     * Updates keyboard state and dispatches keyUp event.
     * @param player The DirPlayer instance
     * @param eventDispatcher The event dispatcher for sending events
     * @param key The key string
     * @param code The JavaScript key code
     * @throws ScriptError if a script error occurs during event dispatch
     */
    public static void playerKeyUp(DirPlayer player, EventDispatcher eventDispatcher,
                                   String key, int code) throws ScriptError {
        if (!player.isPlaying) {
            return;
        }

        // Update keyboard state
        player.keyboardManager.keyUp(key, code);

        // Get script instances for keyboard focus sprite
        List<ScriptInstanceRef> instanceIds = null;

        if (player.keyboardFocusSprite != -1) {
            int spriteId = player.keyboardFocusSprite;
            Sprite sprite = player.movie.score.getSprite((short) spriteId);

            if (sprite != null) {
                // Build instance list from sprite's script instances
                instanceIds = new ArrayList<>();
                for (int id : sprite.scriptInstanceList) {
                    instanceIds.add(new ScriptInstanceRef(id));
                }
            }
        }

        // Dispatch keyUp event
        if (eventDispatcher != null) {
            eventDispatcher.dispatchTargetedEvent("keyUp", Collections.emptyList(), instanceIds);
        }
    }

    /**
     * Set the keyboard focus to a specific sprite.
     * @param player The DirPlayer instance
     * @param spriteNum The sprite number to focus, or -1 to clear focus
     */
    public static void setKeyboardFocus(DirPlayer player, int spriteNum) {
        player.keyboardFocusSprite = spriteNum;
        logger.debug("Keyboard focus set to sprite: {}", spriteNum);
    }

    /**
     * Get the current keyboard focus sprite.
     * @param player The DirPlayer instance
     * @return The sprite number with keyboard focus, or -1 if no focus
     */
    public static int getKeyboardFocus(DirPlayer player) {
        return player.keyboardFocusSprite;
    }

    /**
     * Clear the keyboard focus.
     * @param player The DirPlayer instance
     */
    public static void clearKeyboardFocus(DirPlayer player) {
        player.keyboardFocusSprite = -1;
        logger.debug("Keyboard focus cleared");
    }
}
