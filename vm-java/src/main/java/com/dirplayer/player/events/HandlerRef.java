package com.dirplayer.player.events;

import com.dirplayer.player.CastMemberRef;

import java.util.Objects;

/**
 * Reference to a handler in a script.
 * Port of Rust handler reference tuple (CastMemberRef, String).
 */
public class HandlerRef {
    public final CastMemberRef scriptRef;
    public final String handlerName;

    public HandlerRef(CastMemberRef scriptRef, String handlerName) {
        this.scriptRef = scriptRef;
        this.handlerName = handlerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HandlerRef that = (HandlerRef) o;
        return Objects.equals(scriptRef, that.scriptRef) && Objects.equals(handlerName, that.handlerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scriptRef, handlerName);
    }

    @Override
    public String toString() {
        return scriptRef + "." + handlerName;
    }
}
