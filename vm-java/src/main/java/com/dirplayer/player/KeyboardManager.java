package com.dirplayer.player;

import java.util.HashSet;
import java.util.Set;

/**
 * Manager for keyboard input state.
 * Port of Rust KeyboardManager struct.
 */
public class KeyboardManager {
    public Set<Integer> pressedKeys;
    public String lastKey;
    public int lastKeyCode;
    public boolean shiftDown;
    public boolean controlDown;
    public boolean altDown;
    public boolean commandDown;

    public KeyboardManager() {
        this.pressedKeys = new HashSet<>();
        this.lastKey = "";
        this.lastKeyCode = 0;
        this.shiftDown = false;
        this.controlDown = false;
        this.altDown = false;
        this.commandDown = false;
    }

    public void keyDown(String key, int code) {
        pressedKeys.add(code);
        lastKey = key;
        lastKeyCode = code;

        // Update modifier states
        updateModifiers(code, true);
    }

    public void keyUp(String key, int code) {
        pressedKeys.remove(code);
        updateModifiers(code, false);
    }

    private void updateModifiers(int code, boolean pressed) {
        switch (code) {
            case 16:  // Shift
                shiftDown = pressed;
                break;
            case 17:  // Control
                controlDown = pressed;
                break;
            case 18:  // Alt
                altDown = pressed;
                break;
            case 91:  // Meta/Command (left)
            case 93:  // Meta/Command (right)
                commandDown = pressed;
                break;
        }
    }

    public boolean isKeyDown(int code) {
        return pressedKeys.contains(code);
    }

    public boolean isKeyDown(String key) {
        // Convert key string to code and check
        int code = keyStringToCode(key);
        return isKeyDown(code);
    }

    public String getLastKey() {
        return lastKey;
    }

    public int getLastKeyCode() {
        return lastKeyCode;
    }

    public void reset() {
        pressedKeys.clear();
        lastKey = "";
        lastKeyCode = 0;
        shiftDown = false;
        controlDown = false;
        altDown = false;
        commandDown = false;
    }

    private int keyStringToCode(String key) {
        if (key == null || key.isEmpty()) return 0;

        // Handle special keys
        switch (key.toLowerCase()) {
            case "enter": return 13;
            case "return": return 13;
            case "tab": return 9;
            case "backspace": return 8;
            case "delete": return 46;
            case "escape": return 27;
            case "space": return 32;
            case "up": return 38;
            case "down": return 40;
            case "left": return 37;
            case "right": return 39;
            case "shift": return 16;
            case "control": return 17;
            case "alt": return 18;
            default:
                // Single character - return its char code
                if (key.length() == 1) {
                    return key.toUpperCase().charAt(0);
                }
                return 0;
        }
    }

    // Getters for modifier state
    public boolean isShiftDown() {
        return shiftDown;
    }

    public void setShiftDown(boolean shiftDown) {
        this.shiftDown = shiftDown;
    }

    public boolean isControlDown() {
        return controlDown;
    }

    public void setControlDown(boolean controlDown) {
        this.controlDown = controlDown;
    }

    public boolean isAltDown() {
        return altDown;
    }

    public void setAltDown(boolean altDown) {
        this.altDown = altDown;
    }

    public boolean isCommandDown() {
        return commandDown;
    }

    public void setCommandDown(boolean commandDown) {
        this.commandDown = commandDown;
    }

    public boolean is_alt_down() {
        return altDown;
    }

    public boolean is_control_down() {
        return controlDown;
    }

    public boolean is_command_down() {
        return commandDown;
    }
}
