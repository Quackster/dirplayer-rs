package com.dirplayer;

/**
 * Simple logger implementation for TeaVM compatibility.
 * Replaces SLF4J with basic console logging.
 */
public class SimpleLogger {
    private final String name;

    public SimpleLogger(String name) {
        this.name = name;
    }

    public static SimpleLogger getLogger(Class<?> clazz) {
        return new SimpleLogger(clazz.getSimpleName());
    }

    public boolean isDebugEnabled() {
        return false; // Debug logging disabled for production
    }

    public void debug(String msg, Object... args) {
        // No-op for production to reduce console noise
    }

    public void info(String msg, Object... args) {
        System.out.println("[INFO] " + name + ": " + format(msg, args));
    }

    public void warn(String msg, Object... args) {
        System.out.println("[WARN] " + name + ": " + format(msg, args));
    }

    public void error(String msg, Object... args) {
        System.err.println("[ERROR] " + name + ": " + format(msg, args));
    }

    public void error(String msg, Throwable throwable) {
        System.err.println("[ERROR] " + name + ": " + msg);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }

    private String format(String msg, Object... args) {
        if (args == null || args.length == 0) {
            return msg;
        }

        // Simple {} replacement
        String result = msg;
        for (Object arg : args) {
            result = result.replaceFirst("\\{\\}", String.valueOf(arg));
        }
        return result;
    }
}
