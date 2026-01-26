package com.dirplayer.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Keyboard key code mappings for Director player.
 * Maps JavaScript key codes to Shockwave/Director key codes.
 * Port of Rust keyboard_map.rs.
 */
public final class KeyboardMap {

    private static final Map<Integer, Integer> JS_TO_SW_MAP;
    private static final Map<Character, Integer> DIRECTOR_SPECIAL_CHAR_TO_KEYCODE_MAP;
    private static final Map<Character, Integer> CHAR_TO_KEYCODE_MAP;

    static {
        // JavaScript key code to Shockwave key code mapping
        Map<Integer, Integer> jsToSw = new HashMap<>();
        jsToSw.put(8, 51);     // backspace
        jsToSw.put(13, 36);    // enter
        jsToSw.put(32, 49);    // space
        jsToSw.put(65, 0);     // a
        jsToSw.put(66, 11);    // b
        jsToSw.put(67, 8);     // c
        jsToSw.put(68, 2);     // d
        jsToSw.put(69, 14);    // e
        jsToSw.put(70, 3);     // f
        jsToSw.put(71, 5);     // g
        jsToSw.put(72, 4);     // h
        jsToSw.put(73, 34);    // i
        jsToSw.put(74, 38);    // j
        jsToSw.put(75, 40);    // k
        jsToSw.put(76, 37);    // l
        jsToSw.put(77, 46);    // m
        jsToSw.put(78, 45);    // n
        jsToSw.put(79, 31);    // o
        jsToSw.put(80, 35);    // p
        jsToSw.put(81, 12);    // q
        jsToSw.put(82, 15);    // r
        jsToSw.put(83, 1);     // s
        jsToSw.put(84, 17);    // t
        jsToSw.put(85, 32);    // u
        jsToSw.put(86, 9);     // v
        jsToSw.put(87, 13);    // w
        jsToSw.put(88, 7);     // x
        jsToSw.put(89, 16);    // y
        jsToSw.put(90, 6);     // z
        jsToSw.put(48, 29);    // 0
        jsToSw.put(49, 18);    // 1
        jsToSw.put(50, 19);    // 2
        jsToSw.put(51, 20);    // 3
        jsToSw.put(52, 21);    // 4
        jsToSw.put(53, 23);    // 5
        jsToSw.put(54, 22);    // 6
        jsToSw.put(55, 26);    // 7
        jsToSw.put(56, 28);    // 8
        jsToSw.put(57, 25);    // 9
        jsToSw.put(37, 123);   // left
        jsToSw.put(38, 126);   // up
        jsToSw.put(39, 124);   // right
        jsToSw.put(40, 125);   // down
        jsToSw.put(16, 56);    // shift
        jsToSw.put(17, 55);    // ctrl
        jsToSw.put(18, 58);    // alt
        jsToSw.put(27, 53);    // esc
        jsToSw.put(112, 122);  // f1
        jsToSw.put(113, 120);  // f2
        jsToSw.put(114, 99);   // f3
        jsToSw.put(115, 118);  // f4
        jsToSw.put(116, 96);   // f5
        jsToSw.put(117, 97);   // f6
        jsToSw.put(118, 98);   // f7
        jsToSw.put(119, 100);  // f8
        jsToSw.put(120, 101);  // f9
        jsToSw.put(121, 109);  // f10
        jsToSw.put(122, 111);  // f11
        jsToSw.put(123, 110);  // f12
        jsToSw.put(192, 50);   // `
        jsToSw.put(189, 27);   // -
        jsToSw.put(187, 24);   // =
        jsToSw.put(219, 33);   // [
        jsToSw.put(221, 30);   // ]
        jsToSw.put(186, 41);   // ;
        jsToSw.put(222, 39);   // '
        jsToSw.put(220, 42);   // \
        jsToSw.put(188, 43);   // ,
        jsToSw.put(190, 47);   // .
        jsToSw.put(191, 44);   // /
        jsToSw.put(9, 48);     // tab
        jsToSw.put(20, 57);    // caps lock
        jsToSw.put(97, 83);    // numpad 1
        jsToSw.put(98, 84);    // numpad 2
        jsToSw.put(99, 85);    // numpad 3
        jsToSw.put(100, 86);   // numpad 4
        jsToSw.put(101, 87);   // numpad 5
        jsToSw.put(102, 88);   // numpad 6
        jsToSw.put(103, 89);   // numpad 7
        jsToSw.put(104, 91);   // numpad 8
        jsToSw.put(105, 92);   // numpad 9
        JS_TO_SW_MAP = Collections.unmodifiableMap(jsToSw);

        // Director special character codes for special keys.
        // In Lingo, numToChar(28-31) return control characters that represent arrow keys.
        Map<Character, Integer> specialCharToKeycode = new HashMap<>();
        // Director arrow key character codes (used with numToChar)
        specialCharToKeycode.put('\u001C', 123);  // ASCII 28 = Left Arrow -> SW keycode 123
        specialCharToKeycode.put('\u001D', 124);  // ASCII 29 = Right Arrow -> SW keycode 124
        specialCharToKeycode.put('\u001E', 126);  // ASCII 30 = Up Arrow -> SW keycode 126
        specialCharToKeycode.put('\u001F', 125);  // ASCII 31 = Down Arrow -> SW keycode 125
        // Enter/Return
        specialCharToKeycode.put('\r', 36);       // ASCII 13 = Return -> SW keycode 36
        specialCharToKeycode.put('\n', 36);       // Newline also maps to Return
        // Tab and other control characters
        specialCharToKeycode.put('\t', 48);       // ASCII 9 = Tab -> SW keycode 48
        specialCharToKeycode.put('\u0008', 51);   // ASCII 8 = Backspace -> SW keycode 51
        specialCharToKeycode.put('\u001B', 53);   // ASCII 27 = Escape -> SW keycode 53
        specialCharToKeycode.put(' ', 49);        // Space -> SW keycode 49
        DIRECTOR_SPECIAL_CHAR_TO_KEYCODE_MAP = Collections.unmodifiableMap(specialCharToKeycode);

        // Character to Shockwave key code mapping
        Map<Character, Integer> charToKeycode = new HashMap<>();
        charToKeycode.put('a', 0);
        charToKeycode.put('b', 11);
        charToKeycode.put('c', 8);
        charToKeycode.put('d', 2);
        charToKeycode.put('e', 14);
        charToKeycode.put('f', 3);
        charToKeycode.put('g', 5);
        charToKeycode.put('h', 4);
        charToKeycode.put('i', 34);
        charToKeycode.put('j', 38);
        charToKeycode.put('k', 40);
        charToKeycode.put('l', 37);
        charToKeycode.put('m', 46);
        charToKeycode.put('n', 45);
        charToKeycode.put('o', 31);
        charToKeycode.put('p', 35);
        charToKeycode.put('q', 12);
        charToKeycode.put('r', 15);
        charToKeycode.put('s', 1);
        charToKeycode.put('t', 17);
        charToKeycode.put('u', 32);
        charToKeycode.put('v', 9);
        charToKeycode.put('w', 13);
        charToKeycode.put('x', 7);
        charToKeycode.put('y', 16);
        charToKeycode.put('z', 6);
        charToKeycode.put('0', 29);
        charToKeycode.put('1', 18);
        charToKeycode.put('2', 19);
        charToKeycode.put('3', 20);
        charToKeycode.put('4', 21);
        charToKeycode.put('5', 23);
        charToKeycode.put('6', 22);
        charToKeycode.put('7', 26);
        charToKeycode.put('8', 28);
        charToKeycode.put('9', 25);
        CHAR_TO_KEYCODE_MAP = Collections.unmodifiableMap(charToKeycode);
    }

    private KeyboardMap() {
        // Utility class - prevent instantiation
    }

    /**
     * Get the JavaScript to Shockwave key code mapping.
     * @return Unmodifiable map from JS key codes to SW key codes
     */
    public static Map<Integer, Integer> getKeyboardKeyMapJsToSw() {
        return JS_TO_SW_MAP;
    }

    /**
     * Get the Director special character to key code mapping.
     * In Lingo, numToChar(28-31) return control characters that represent arrow keys.
     * @return Unmodifiable map from special characters to SW key codes
     */
    public static Map<Character, Integer> getDirectorSpecialCharToKeycodeMap() {
        return DIRECTOR_SPECIAL_CHAR_TO_KEYCODE_MAP;
    }

    /**
     * Get the character to Shockwave key code mapping.
     * @return Unmodifiable map from characters to SW key codes
     */
    public static Map<Character, Integer> getCharToKeycodeMap() {
        return CHAR_TO_KEYCODE_MAP;
    }

    /**
     * Map a JavaScript key code to a Shockwave key code.
     * @param jsKeyCode The JavaScript key code
     * @return The mapped Shockwave key code, or the original code if no mapping exists
     */
    public static int mapJsToSw(int jsKeyCode) {
        Integer mapped = JS_TO_SW_MAP.get(jsKeyCode);
        return mapped != null ? mapped : jsKeyCode;
    }

    /**
     * Get the Shockwave key code for a character.
     * @param c The character
     * @return The Shockwave key code, or null if no mapping exists
     */
    public static Integer getKeycodeForChar(char c) {
        // First check special characters
        Integer special = DIRECTOR_SPECIAL_CHAR_TO_KEYCODE_MAP.get(c);
        if (special != null) {
            return special;
        }
        // Then check regular characters (lowercase)
        return CHAR_TO_KEYCODE_MAP.get(Character.toLowerCase(c));
    }
}
