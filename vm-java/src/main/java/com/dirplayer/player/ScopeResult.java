package com.dirplayer.player;

/**
 * Result of executing a script handler.
 * Contains the return value and whether the event was "passed" (propagated).
 */
public class ScopeResult {
    public final int returnValue;
    public final boolean passed;

    public ScopeResult(int returnValue, boolean passed) {
        this.returnValue = returnValue;
        this.passed = passed;
    }
}
