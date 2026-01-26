package com.dirplayer.player;

/**
 * Reference to a cursor.
 * Port of Rust CursorRef struct.
 */
public class CursorRef {
    public int cursorId;
    public CastMemberRef memberRef;
    public boolean isSystemCursor;

    public CursorRef() {
        this.cursorId = 0;
        this.isSystemCursor = true;
    }

    public CursorRef(int cursorId) {
        this.cursorId = cursorId;
        this.isSystemCursor = true;
    }

    public static CursorRef system(int cursorId) {
        return new CursorRef(cursorId);
    }

    public static CursorRef member(CastMemberRef ref) {
        CursorRef c = new CursorRef();
        c.memberRef = ref;
        c.isSystemCursor = false;
        return c;
    }

    public CursorRef copy() {
        CursorRef c = new CursorRef();
        c.cursorId = cursorId;
        c.memberRef = memberRef != null ? memberRef.copy() : null;
        c.isSystemCursor = isSystemCursor;
        return c;
    }

    @Override
    public String toString() {
        if (isSystemCursor) {
            return "cursor(" + cursorId + ")";
        }
        return "cursor(" + memberRef + ")";
    }
}
