package com.dirplayer.player;

import java.util.HashMap;
import java.util.Map;

/**
 * A cast library containing cast members.
 * Port of Rust CastLib struct.
 */
public class CastLib {
    public int id;
    public String name;
    public Map<Integer, CastMember> members;
    public boolean isExternal;
    public String externalPath;

    public CastLib() {
        this.id = 0;
        this.name = "";
        this.members = new HashMap<>();
        this.isExternal = false;
        this.externalPath = "";
    }

    public CastLib(int id, String name) {
        this.id = id;
        this.name = name;
        this.members = new HashMap<>();
        this.isExternal = false;
        this.externalPath = "";
    }

    public CastMember getMember(int memberNum) {
        return members.get(memberNum);
    }

    public void addMember(int memberNum, CastMember member) {
        members.put(memberNum, member);
    }

    public int getMemberCount() {
        return members.size();
    }
}
