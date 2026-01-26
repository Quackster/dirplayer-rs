package com.dirplayer.player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for Lingo timeout objects.
 * Port of Rust TimeoutManager struct.
 */
public class TimeoutManager {
    public Map<String, Timeout> timeouts;

    public TimeoutManager() {
        this.timeouts = new HashMap<>();
    }

    public void add(String name, Timeout timeout) {
        timeouts.put(name, timeout);
    }

    public Timeout get(String name) {
        return timeouts.get(name);
    }

    public void remove(String name) {
        timeouts.remove(name);
    }

    public void clear() {
        timeouts.clear();
    }

    public static class Timeout {
        public String name;
        public int duration;
        public int callback;  // DatumRef
        public int target;    // DatumRef
        public boolean persistent;
        public long lastTriggerTime;
        public boolean isActive;

        public Timeout(String name, int duration) {
            this.name = name;
            this.duration = duration;
            this.persistent = false;
            this.lastTriggerTime = System.currentTimeMillis();
            this.isActive = true;
        }

        public boolean shouldTrigger() {
            if (!isActive) return false;
            long now = System.currentTimeMillis();
            return (now - lastTriggerTime) >= duration;
        }

        public void reset() {
            lastTriggerTime = System.currentTimeMillis();
        }
    }
}
