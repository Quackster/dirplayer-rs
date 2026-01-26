package com.dirplayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Utility functions for the DirPlayer.
 * Port of Rust utils.rs
 */
public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static final String PATH_SEPARATOR = "/";

    public static void logI(String value) {
        logger.info(value);
    }

    public static void consoleWarn(String value) {
        logger.warn(value);
    }

    public static void consoleError(String value) {
        logger.error(value);
    }

    /**
     * Get the basename of a path without its extension.
     */
    public static String getBasenameNoExtension(String path) {
        String[] segments = path.split("/");
        String fileName = segments.length > 0 ? segments[segments.length - 1] : "";

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    /**
     * Get the base URL (directory) from a URL.
     */
    public static URL getBaseUrl(URL url) {
        try {
            String urlStr = url.toString();
            int lastSlash = urlStr.lastIndexOf('/');
            if (lastSlash >= 0) {
                return new URL(urlStr.substring(0, lastSlash + 1));
            }
            return url;
        } catch (MalformedURLException e) {
            return url;
        }
    }

    /**
     * Convert byte array to hex string.
     */
    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02x", bytes[i] & 0xFF));
        }
        return sb.toString();
    }

    /**
     * Number of ticks (60 ticks/second) from epoch until endtime.
     */
    private static long ticksSinceEpoch(LocalDateTime endTime) {
        long nanos = ChronoUnit.NANOS.between(LocalDateTime.of(1970, 1, 1, 0, 0), endTime);
        long tickDurationNanos = 1_000_000_000L / 60;  // 60 ticks per second
        return nanos / tickDurationNanos;
    }

    /**
     * Get elapsed ticks since start time.
     */
    public static int getElapsedTicks(LocalDateTime startTime) {
        long currentTicks = ticksSinceEpoch(LocalDateTime.now());
        long startTicks = ticksSinceEpoch(startTime);
        return (int) (currentTicks - startTicks);
    }

    /**
     * Clamp a value between min and max.
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamp a float value between min and max.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamp a double value between min and max.
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Linearly interpolate between two values.
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Check if a string represents a valid URL.
     */
    public static boolean isValidUrl(String urlString) {
        try {
            new URL(urlString);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Join path segments with the path separator.
     */
    public static String joinPath(String... segments) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0 && !result.toString().endsWith(PATH_SEPARATOR)) {
                result.append(PATH_SEPARATOR);
            }
            String segment = segments[i];
            if (segment.startsWith(PATH_SEPARATOR) && result.length() > 0) {
                segment = segment.substring(1);
            }
            result.append(segment);
        }
        return result.toString();
    }

    /**
     * Encode a string for use in a URL path.
     */
    public static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * Decode a URL-encoded string.
     */
    public static String urlDecode(String value) {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
}
