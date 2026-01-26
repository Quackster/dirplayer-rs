package com.dirplayer.player;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference to a cursor.
 * Port of Rust CursorRef struct.
 */
public class CursorRef {
    public int cursorId;
    public CastMemberRef memberRef;
    public boolean isSystemCursor;
    public List<Integer> memberList;  // For member-based cursors with multiple parts (cursor + mask)

    public CursorRef() {
        this.cursorId = 0;
        this.isSystemCursor = true;
        this.memberList = null;
    }

    public CursorRef(int cursorId) {
        this.cursorId = cursorId;
        this.isSystemCursor = true;
        this.memberList = null;
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

    public static CursorRef memberList(List<Integer> slotNumbers) {
        CursorRef c = new CursorRef();
        c.isSystemCursor = false;
        c.memberList = new ArrayList<>(slotNumbers);
        return c;
    }

    /**
     * Check if this is a member-based cursor.
     */
    public boolean isMember() {
        return !isSystemCursor && (memberRef != null || (memberList != null && !memberList.isEmpty()));
    }

    /**
     * Get the member list for member-based cursors.
     * Returns slot numbers of the cursor bitmap(s).
     */
    public List<Integer> getMemberList() {
        if (memberList != null) {
            return memberList;
        }
        // Convert single memberRef to list
        if (memberRef != null && memberRef.isValid()) {
            List<Integer> list = new ArrayList<>();
            list.add(CastManager.getCastSlotNumber(memberRef.getCastLib(), memberRef.getCastMember()));
            return list;
        }
        return null;
    }

    public CursorRef copy() {
        CursorRef c = new CursorRef();
        c.cursorId = cursorId;
        c.memberRef = memberRef != null ? memberRef.copy() : null;
        c.isSystemCursor = isSystemCursor;
        c.memberList = memberList != null ? new ArrayList<>(memberList) : null;
        return c;
    }

    @Override
    public String toString() {
        if (isSystemCursor) {
            return "cursor(" + cursorId + ")";
        }
        if (memberList != null) {
            return "cursor(members=" + memberList + ")";
        }
        return "cursor(" + memberRef + ")";
    }
}
