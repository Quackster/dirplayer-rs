package com.dirplayer.player;

/**
 * Exception thrown when a Lingo script error occurs.
 * Port of Rust ScriptError struct.
 */
public class ScriptError extends Exception {
    private final String message;
    private String scriptName;
    private String handlerName;
    private int bytecodeIndex;

    public ScriptError(String message) {
        super(message);
        this.message = message;
    }

    public ScriptError(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public ScriptError withLocation(String scriptName, String handlerName, int bytecodeIndex) {
        this.scriptName = scriptName;
        this.handlerName = handlerName;
        this.bytecodeIndex = bytecodeIndex;
        return this;
    }

    public String getScriptName() {
        return scriptName;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public int getBytecodeIndex() {
        return bytecodeIndex;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(message);
        if (scriptName != null) {
            sb.append(" at ");
            sb.append(scriptName);
            if (handlerName != null) {
                sb.append(".");
                sb.append(handlerName);
            }
            sb.append(" (bytecode index: ");
            sb.append(bytecodeIndex);
            sb.append(")");
        }
        return sb.toString();
    }
}
