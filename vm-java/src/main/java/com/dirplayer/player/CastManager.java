package com.dirplayer.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager for cast libraries and cast members.
 * Port of Rust CastManager struct.
 */
public class CastManager {
    public List<CastLib> castLibs;
    public Map<Integer, CastMember> loadedMembers;
    public Map<String, Integer> memberNameMap;

    public CastManager() {
        this.castLibs = new ArrayList<>();
        this.loadedMembers = new HashMap<>();
        this.memberNameMap = new HashMap<>();
    }

    public static CastManager empty() {
        return new CastManager();
    }

    public CastLib getCastLib(int index) {
        if (index >= 0 && index < castLibs.size()) {
            return castLibs.get(index);
        }
        return null;
    }

    public CastMember getMember(CastMemberRef ref) {
        int key = (ref.castLib << 16) | ref.castMember;
        return loadedMembers.get(key);
    }

    public CastMember getMember(int castLib, int castMember) {
        int key = (castLib << 16) | castMember;
        return loadedMembers.get(key);
    }

    public void addMember(CastMemberRef ref, CastMember member) {
        int key = (ref.castLib << 16) | ref.castMember;
        loadedMembers.put(key, member);

        if (member.name != null && !member.name.isEmpty()) {
            memberNameMap.put(member.name.toLowerCase(), key);
        }
    }

    public CastMemberRef findMemberByName(String name) {
        Integer key = memberNameMap.get(name.toLowerCase());
        if (key != null) {
            int castLib = (key >> 16) & 0xFFFF;
            int castMember = key & 0xFFFF;
            return new CastMemberRef(castLib, castMember);
        }
        return null;
    }

    public int getCastLibCount() {
        return castLibs.size();
    }
}
