package com.dirplayer.player;

import com.dirplayer.director.enums.ScriptType;
import com.dirplayer.director.lingo.datum.Datum;
import com.dirplayer.player.cast.CastMemberData;
import com.dirplayer.player.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cast library containing cast members.
 * Port of Rust CastLib struct.
 */
public class CastLib {
    private static final Logger logger = LoggerFactory.getLogger(CastLib.class);

    public String name;
    public String fileName;
    public int number;
    public boolean isExternal;
    public CastLibState state;
    public Map<Integer, CastMember> members;
    public Map<Integer, Script> scripts;
    public int preloadMode;
    public boolean capitalX;
    public int dirVersion;

    /**
     * Cast library state enum.
     */
    public enum CastLibState {
        None,
        Loading,
        Loaded
    }

    public CastLib() {
        this.name = "";
        this.fileName = "";
        this.number = 0;
        this.isExternal = false;
        this.state = CastLibState.None;
        this.members = new HashMap<>();
        this.scripts = new HashMap<>();
        this.preloadMode = 0;
        this.capitalX = false;
        this.dirVersion = 0;
    }

    public CastLib(int number, String name) {
        this();
        this.number = number;
        this.name = name;
    }

    /**
     * Get the maximum member ID in this cast.
     */
    public int maxMemberId() {
        return members.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /**
     * Find the first free member ID.
     */
    public int firstFreeMemberId() {
        int maxMember = 5000;
        for (int i = 1; i < maxMember; i++) {
            if (!members.containsKey(i)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Remove a member from this cast.
     */
    public void removeMember(int memberNumber) {
        members.remove(memberNumber);
        scripts.remove(memberNumber);
        logger.debug("Removed member {} from cast {}", memberNumber, number);
    }

    /**
     * Find a member by number.
     */
    public CastMember findMemberByNumber(int memberNumber) {
        return members.get(memberNumber);
    }

    /**
     * Find a member by name (case-insensitive).
     */
    public CastMember findMemberByName(String name) {
        for (CastMember member : members.values()) {
            if (member.name != null && member.name.equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }

    /**
     * Clear all members and scripts.
     */
    public void clear() {
        if (state != CastLibState.Loaded) {
            return;
        }
        members.clear();
        scripts.clear();
        state = CastLibState.None;
    }

    /**
     * Set property on cast lib.
     */
    public void setProperty(String prop, Datum value) throws ScriptError {
        switch (prop) {
            case "preloadMode":
                preloadMode = value.intValue();
                break;
            case "name":
                name = value.stringValue();
                break;
            case "fileName":
                fileName = value.stringValue();
                break;
            default:
                throw new ScriptError("Cannot set castLib property " + prop);
        }
    }

    /**
     * Get property from cast lib.
     */
    public Datum getProperty(String prop) throws ScriptError {
        switch (prop) {
            case "preloadMode":
                return Datum.ofInt(preloadMode);
            case "fileName":
                return Datum.ofString(fileName);
            case "number":
                return Datum.ofInt(number);
            case "name":
                return Datum.ofString(name);
            case "number of castMembers":
            case "number of members":
                return Datum.ofInt(members.size());
            default:
                throw new ScriptError("Cannot get castLib property " + prop);
        }
    }

    /**
     * Insert a member into this cast.
     */
    public void insertMember(int memberNumber, CastMember member) {
        members.put(memberNumber, member);
        logger.debug("Inserted member {} into cast {}", memberNumber, number);
    }

    /**
     * Get script for a member.
     */
    public Script getScriptForMember(int memberNumber) {
        return scripts.get(memberNumber);
    }

    /**
     * Get all movie scripts from this cast.
     */
    public List<Script> getMovieScripts() {
        List<Script> result = new ArrayList<>();
        for (Script script : scripts.values()) {
            if (script.getScriptType() == ScriptType.Movie) {
                result.add(script);
            }
        }
        return result;
    }

    // Legacy compatibility methods
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
