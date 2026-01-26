package com.dirplayer.player;

import com.dirplayer.SimpleLogger;


import java.util.ArrayList;
import java.util.List;

/**
 * Manager for keyboard input state.
 * Tracks currently pressed keys and provides modifier key state.
 * Port of Rust KeyboardManager struct from keyboard.rs.
 */
public class KeyboardManager {
    private static final SimpleLogger logger = SimpleLogger.getLogger(KeyboardManager.class);

    /**
     * Represents a pressed keyboard key.
     */
    public static class KeyboardKey {
        public final String key;
        public final int code;

        public KeyboardKey(String key, int code) {
            this.key = key;
            this.code = code;
        }
    }

    /** List of currently pressed keys */
    private final List<KeyboardKey> downKeys;

    /** The last key string pressed */
    public String lastKey = "";

    /** The last key code pressed */
    public int lastKeyCode = 0;

    public KeyboardManager() {
        this.downKeys = new ArrayList<>();
    }

    /**
     * Handle a key down event.
     * Maps the code to Shockwave format and tracks the key if not already pressed.
     * @param key The key string (e.g., "a", "Shift", "Enter")
     * @param code The JavaScript key code
     */
    public void keyDown(String key, int code) {
        Integer codeMapped = KeyboardMap.getKeyboardKeyMapJsToSw().get(code);
        logger.debug("Key down: {} {} (mapped to: {})", key, code, codeMapped);
        int mappedCode = codeMapped != null ? codeMapped : code;

        // Check if this code is already in the downKeys list
        boolean alreadyDown = downKeys.stream().anyMatch(k -> k.code == mappedCode);
        if (!alreadyDown) {
            downKeys.add(new KeyboardKey(key, mappedCode));
        }
        // Update last key info
        this.lastKey = key;
        this.lastKeyCode = mappedCode;
    }

    /**
     * Handle a key up event.
     * Removes the key from the pressed keys list.
     * @param key The key string (unused but kept for API consistency)
     * @param code The JavaScript key code
     */
    public void keyUp(String key, int code) {
        // Map the code the same way as keyDown does
        Integer codeMapped = KeyboardMap.getKeyboardKeyMapJsToSw().get(code);
        int codeToRemove = codeMapped != null ? codeMapped : code;

        downKeys.removeIf(k -> k.code == codeToRemove);
    }

    /**
     * Check if a key with the given name is currently pressed.
     * @param key The key name to check (e.g., "Shift", "Control", "a")
     * @return true if the key is currently pressed
     */
    public boolean isKeyDown(String key) {
        return downKeys.stream().anyMatch(k -> k.key.equals(key));
    }

    /**
     * Check if a key with the given code is currently pressed.
     * @param keyCode The key code to check
     * @return true if the key is currently pressed
     */
    public boolean isKeyDown(int keyCode) {
        return downKeys.stream().anyMatch(k -> k.code == keyCode);
    }

    /**
     * Check if the Command/Meta key is currently pressed.
     * @return true if Command/Meta is pressed
     */
    public boolean isCommandDown() {
        return isKeyDown("Meta");
    }

    /**
     * Check if the Control key is currently pressed.
     * @return true if Control is pressed
     */
    public boolean isControlDown() {
        return isKeyDown("Control");
    }

    /**
     * Check if the Shift key is currently pressed.
     * @return true if Shift is pressed
     */
    public boolean isShiftDown() {
        return isKeyDown("Shift");
    }

    /**
     * Check if the Alt key is currently pressed.
     * @return true if Alt is pressed
     */
    public boolean isAltDown() {
        return isKeyDown("Alt");
    }

    /**
     * Get the key code of the most recently pressed key.
     * @return The Shockwave key code of the last pressed key, or 0 if no keys are pressed
     */
    public int keyCode() {
        if (downKeys.isEmpty()) {
            return 0;
        }
        return downKeys.get(downKeys.size() - 1).code;
    }

    /**
     * Get the key string of the most recently pressed key.
     * @return The key string of the last pressed key, or empty string if no keys are pressed
     */
    public String key() {
        if (downKeys.isEmpty()) {
            return "";
        }
        return downKeys.get(downKeys.size() - 1).key;
    }

    /**
     * Get the list of currently pressed keys.
     * @return List of currently pressed KeyboardKey objects
     */
    public List<KeyboardKey> getDownKeys() {
        return new ArrayList<>(downKeys);
    }

    /**
     * Reset the keyboard state, clearing all pressed keys.
     */
    public void reset() {
        downKeys.clear();
    }

    // Legacy compatibility methods

    /**
     * @deprecated Use isCommandDown() instead
     */
    @Deprecated
    public boolean is_command_down() {
        return isCommandDown();
    }

    /**
     * @deprecated Use isControlDown() instead
     */
    @Deprecated
    public boolean is_control_down() {
        return isControlDown();
    }

    /**
     * @deprecated Use isAltDown() instead
     */
    @Deprecated
    public boolean is_alt_down() {
        return isAltDown();
    }

    /**
     * Get the last key code (legacy getter).
     * @return The Shockwave key code of the last pressed key
     */
    public int getLastKeyCode() {
        return keyCode();
    }

    /**
     * Get the last key string (legacy getter).
     * @return The key string of the last pressed key
     */
    public String getLastKey() {
        return key();
    }

    /**
     * Set the shift modifier state directly.
     * Note: This is for external input handling; prefer using keyDown/keyUp.
     * @param shiftDown true if shift should be considered pressed
     */
    public void setShiftDown(boolean shiftDown) {
        if (shiftDown && !isKeyDown("Shift")) {
            downKeys.add(new KeyboardKey("Shift", KeyboardMap.mapJsToSw(16)));
        } else if (!shiftDown) {
            downKeys.removeIf(k -> k.key.equals("Shift"));
        }
    }

    /**
     * Set the control modifier state directly.
     * Note: This is for external input handling; prefer using keyDown/keyUp.
     * @param controlDown true if control should be considered pressed
     */
    public void setControlDown(boolean controlDown) {
        if (controlDown && !isKeyDown("Control")) {
            downKeys.add(new KeyboardKey("Control", KeyboardMap.mapJsToSw(17)));
        } else if (!controlDown) {
            downKeys.removeIf(k -> k.key.equals("Control"));
        }
    }

    /**
     * Set the alt modifier state directly.
     * Note: This is for external input handling; prefer using keyDown/keyUp.
     * @param altDown true if alt should be considered pressed
     */
    public void setAltDown(boolean altDown) {
        if (altDown && !isKeyDown("Alt")) {
            downKeys.add(new KeyboardKey("Alt", KeyboardMap.mapJsToSw(18)));
        } else if (!altDown) {
            downKeys.removeIf(k -> k.key.equals("Alt"));
        }
    }

    /**
     * Set the command modifier state directly.
     * Note: This is for external input handling; prefer using keyDown/keyUp.
     * @param commandDown true if command should be considered pressed
     */
    public void setCommandDown(boolean commandDown) {
        if (commandDown && !isKeyDown("Meta")) {
            downKeys.add(new KeyboardKey("Meta", KeyboardMap.mapJsToSw(91)));
        } else if (!commandDown) {
            downKeys.removeIf(k -> k.key.equals("Meta"));
        }
    }

    /**
     * Check if a key with the given code is currently pressed.
     * @param code The Shockwave key code to check
     * @return true if a key with that code is currently pressed
     */
    public boolean isKeyCodeDown(int code) {
        return downKeys.stream().anyMatch(k -> k.code == code);
    }
}
