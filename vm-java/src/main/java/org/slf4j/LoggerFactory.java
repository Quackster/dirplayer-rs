package org.slf4j;

/**
 * Simple logger factory shim for compilation without SLF4J dependency.
 */
public class LoggerFactory {
    public static Logger getLogger(Class<?> clazz) {
        return new SimpleLogger(clazz.getSimpleName());
    }

    public static Logger getLogger(String name) {
        return new SimpleLogger(name);
    }

    private static class SimpleLogger implements Logger {
        private final String name;

        SimpleLogger(String name) {
            this.name = name;
        }

        private void log(String level, String msg) {
            System.out.println("[" + level + "] " + name + ": " + msg);
        }

        private String format(String format, Object... args) {
            String result = format;
            for (Object arg : args) {
                int idx = result.indexOf("{}");
                if (idx >= 0) {
                    result = result.substring(0, idx) + String.valueOf(arg) + result.substring(idx + 2);
                }
            }
            return result;
        }

        @Override public void debug(String msg) { log("DEBUG", msg); }
        @Override public void debug(String format, Object arg) { log("DEBUG", format(format, arg)); }
        @Override public void debug(String format, Object arg1, Object arg2) { log("DEBUG", format(format, arg1, arg2)); }
        @Override public void debug(String format, Object... arguments) { log("DEBUG", format(format, arguments)); }

        @Override public void info(String msg) { log("INFO", msg); }
        @Override public void info(String format, Object arg) { log("INFO", format(format, arg)); }
        @Override public void info(String format, Object arg1, Object arg2) { log("INFO", format(format, arg1, arg2)); }
        @Override public void info(String format, Object... arguments) { log("INFO", format(format, arguments)); }

        @Override public void warn(String msg) { log("WARN", msg); }
        @Override public void warn(String format, Object arg) { log("WARN", format(format, arg)); }
        @Override public void warn(String format, Object arg1, Object arg2) { log("WARN", format(format, arg1, arg2)); }
        @Override public void warn(String format, Object... arguments) { log("WARN", format(format, arguments)); }

        @Override public void error(String msg) { log("ERROR", msg); }
        @Override public void error(String format, Object arg) { log("ERROR", format(format, arg)); }
        @Override public void error(String format, Object arg1, Object arg2) { log("ERROR", format(format, arg1, arg2)); }
        @Override public void error(String format, Object... arguments) { log("ERROR", format(format, arguments)); }
        @Override public void error(String msg, Throwable t) { log("ERROR", msg + ": " + t.getMessage()); }

        @Override public boolean isDebugEnabled() { return true; }
        @Override public boolean isInfoEnabled() { return true; }
        @Override public boolean isWarnEnabled() { return true; }
        @Override public boolean isErrorEnabled() { return true; }
    }
}
