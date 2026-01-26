package com.dirplayer.player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Profiling support for measuring execution times.
 * Port of Rust profiling.rs.
 */
public class Profiling {

    /**
     * A profiling token representing a timed operation.
     */
    public static class ProfilingToken {
        private final String name;
        private final Instant startTime;
        private Instant endTime;

        public ProfilingToken(String name) {
            this.name = name;
            this.startTime = Instant.now();
            this.endTime = null;
        }

        /**
         * Get the elapsed duration if the token has been ended.
         *
         * @return The elapsed duration, or null if not ended
         */
        public Duration elapsed() {
            if (endTime == null) {
                return null;
            }
            return Duration.between(startTime, endTime);
        }

        public String getName() {
            return name;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public Instant getEndTime() {
            return endTime;
        }

        void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }
    }

    /**
     * The player profiler - tracks execution times by name.
     */
    public static class PlayerProfiler {
        private final Map<Integer, ProfilingToken> tokens;
        private final Map<String, Duration> totalTimeByName;
        private final AtomicInteger tokenIdCounter;

        public PlayerProfiler() {
            this.tokens = new HashMap<>();
            this.totalTimeByName = new HashMap<>();
            this.tokenIdCounter = new AtomicInteger(0);
        }

        /**
         * Start a profiling session with the given name.
         *
         * @param name The name of the operation being profiled
         * @return The token ID to use when ending the session
         */
        public int start(String name) {
            int id = tokenIdCounter.getAndIncrement();
            tokens.put(id, new ProfilingToken(name));
            return id;
        }

        /**
         * End a profiling session.
         *
         * @param id The token ID from start()
         */
        public void end(int id) {
            ProfilingToken token = tokens.remove(id);
            if (token == null) {
                return;
            }

            token.setEndTime(Instant.now());
            Duration elapsed = token.elapsed();
            if (elapsed == null) {
                return;
            }

            String name = token.getName();
            Duration existing = totalTimeByName.getOrDefault(name, Duration.ZERO);
            totalTimeByName.put(name, existing.plus(elapsed));

            System.out.println(name + " took " + formatDuration(elapsed));
        }

        /**
         * Generate a profiling report.
         *
         * @return A formatted string with profiling results
         */
        public String report() {
            StringBuilder result = new StringBuilder();

            // Calculate total time
            Duration totalElapsed = totalTimeByName.values().stream()
                .reduce(Duration.ZERO, Duration::plus);
            long totalMs = totalElapsed.toMillis();

            if (totalMs == 0) {
                return "No profiling data collected.\n";
            }

            // Sort by elapsed time (ascending, to match Rust's reversed sort)
            List<Map.Entry<String, Duration>> sorted = new ArrayList<>(totalTimeByName.entrySet());
            sorted.sort(Comparator.comparing(Map.Entry::getValue));

            for (Map.Entry<String, Duration> entry : sorted) {
                String name = entry.getKey();
                Duration elapsed = entry.getValue();
                double elapsedPercent = (elapsed.toMillis() * 100.0) / totalMs;

                result.append(String.format("%s took %s (%.2f%%)%n",
                    name, formatDuration(elapsed), elapsedPercent));
            }

            result.append(String.format("Total: %s%n", formatDuration(totalElapsed)));

            return result.toString();
        }

        /**
         * Clear all profiling data.
         */
        public void clear() {
            tokens.clear();
            totalTimeByName.clear();
        }

        private static String formatDuration(Duration duration) {
            long millis = duration.toMillis();
            if (millis < 1000) {
                return millis + "ms";
            }
            long seconds = duration.getSeconds();
            long remainingMillis = millis % 1000;
            return String.format("%d.%03ds", seconds, remainingMillis);
        }
    }

    // Global profiler instance (thread-safe singleton)
    private static volatile PlayerProfiler globalProfiler;
    private static final Object lock = new Object();

    /**
     * Get the global profiler instance.
     *
     * @return The global profiler
     */
    private static PlayerProfiler profiler() {
        if (globalProfiler == null) {
            synchronized (lock) {
                if (globalProfiler == null) {
                    globalProfiler = new PlayerProfiler();
                }
            }
        }
        return globalProfiler;
    }

    /**
     * Start profiling an operation with the given name.
     *
     * @param name The name of the operation
     * @return The token ID to use when ending
     */
    public static int startProfiling(String name) {
        return profiler().start(name);
    }

    /**
     * End a profiling session.
     *
     * @param id The token ID from startProfiling()
     */
    public static void endProfiling(int id) {
        profiler().end(id);
    }

    /**
     * Get a formatted profiling report.
     *
     * @return The profiling report
     */
    public static String getProfilerReport() {
        return profiler().report();
    }

    /**
     * Clear all profiling data.
     */
    public static void clearProfiling() {
        profiler().clear();
    }

    /**
     * Reset the global profiler instance.
     * Useful for testing or starting fresh.
     */
    public static void resetProfiler() {
        synchronized (lock) {
            globalProfiler = new PlayerProfiler();
        }
    }
}
