package com.dirplayer.player.timeout;

/**
 * Timeout object for Lingo timeout scripting.
 */
public class Timeout {
    public String name;
    public int period;
    public int targetRef;
    public String handler;
    public boolean persistent;
    public boolean active;
    public long lastTrigger;

    public Timeout(String name, int period) {
        this.name = name;
        this.period = period;
        this.targetRef = 0;
        this.handler = "";
        this.persistent = false;
        this.active = true;
        this.lastTrigger = System.currentTimeMillis();
    }

    public boolean shouldTrigger() {
        if (!active) return false;
        long now = System.currentTimeMillis();
        return (now - lastTrigger) >= period;
    }

    public void trigger() {
        lastTrigger = System.currentTimeMillis();
        if (!persistent) {
            active = false;
        }
    }

    public void forget() {
        active = false;
    }
}
