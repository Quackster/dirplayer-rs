package com.dirplayer.player;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for debugger breakpoints.
 * Port of Rust BreakpointManager struct.
 */
public class BreakpointManager {
    public List<Breakpoint> breakpoints;

    public BreakpointManager() {
        this.breakpoints = new ArrayList<>();
    }

    public void addBreakpoint(String scriptName, String handlerName, int bytecodeIndex) {
        Breakpoint bp = new Breakpoint(scriptName, handlerName, bytecodeIndex);
        if (!breakpoints.contains(bp)) {
            breakpoints.add(bp);
        }
    }

    public void removeBreakpoint(String scriptName, String handlerName, int bytecodeIndex) {
        breakpoints.removeIf(bp ->
            bp.scriptName.equals(scriptName) &&
            bp.handlerName.equals(handlerName) &&
            bp.bytecodeIndex == bytecodeIndex
        );
    }

    public void toggleBreakpoint(String scriptName, String handlerName, int bytecodeIndex) {
        boolean removed = breakpoints.removeIf(bp ->
            bp.scriptName.equals(scriptName) &&
            bp.handlerName.equals(handlerName) &&
            bp.bytecodeIndex == bytecodeIndex
        );
        if (!removed) {
            addBreakpoint(scriptName, handlerName, bytecodeIndex);
        }
    }

    public boolean hasBreakpoint(String scriptName, String handlerName, int bytecodeIndex) {
        return breakpoints.stream().anyMatch(bp ->
            bp.scriptName.equals(scriptName) &&
            bp.handlerName.equals(handlerName) &&
            bp.bytecodeIndex == bytecodeIndex
        );
    }

    /**
     * Find a breakpoint at the given bytecode location.
     */
    public Breakpoint findBreakpointForBytecode(String scriptName, String handlerName, int bytecodeIndex) {
        return breakpoints.stream()
            .filter(bp ->
                bp.scriptName.equals(scriptName) &&
                bp.handlerName.equals(handlerName) &&
                bp.bytecodeIndex == bytecodeIndex)
            .findFirst()
            .orElse(null);
    }

    public void clear() {
        breakpoints.clear();
    }

    public static class Breakpoint {
        public String scriptName;
        public String handlerName;
        public int bytecodeIndex;
        public boolean enabled;

        public Breakpoint(String scriptName, String handlerName, int bytecodeIndex) {
            this.scriptName = scriptName;
            this.handlerName = handlerName;
            this.bytecodeIndex = bytecodeIndex;
            this.enabled = true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Breakpoint that = (Breakpoint) o;
            return bytecodeIndex == that.bytecodeIndex &&
                   scriptName.equals(that.scriptName) &&
                   handlerName.equals(that.handlerName);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(scriptName, handlerName, bytecodeIndex);
        }
    }
}
