package com.dirplayer.player.script;

import java.util.Objects;

/**
 * Reference to a script instance in the allocator.
 * Port of Rust ScriptInstanceRef struct.
 */
public class ScriptInstanceRef {
    private final int instanceId;

    public ScriptInstanceRef(int instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Get the unique instance ID.
     */
    public int id() {
        return instanceId;
    }

    /**
     * Create a copy of this reference.
     */
    public ScriptInstanceRef copy() {
        return new ScriptInstanceRef(instanceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptInstanceRef that = (ScriptInstanceRef) o;
        return instanceId == that.instanceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId);
    }

    @Override
    public String toString() {
        return "ScriptInstanceRef(" + instanceId + ")";
    }
}
