package com.dirplayer.player;

import com.dirplayer.director.ScriptType;
import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.bitmap.PaletteMap;
import com.dirplayer.player.cast.CastMemberType;
import com.dirplayer.player.cast.PaletteMember;
import com.dirplayer.player.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for cast libraries and cast members.
 * Port of Rust CastManager struct.
 */
public class CastManager {
    private static final Logger logger = LoggerFactory.getLogger(CastManager.class);

    public List<CastLib> casts;
    private List<Script> movieScriptCache;
    private PaletteMap paletteCache;

    /**
     * Preload reason enum.
     */
    public enum CastPreloadReason {
        MovieLoaded,
        AfterFrameOne
    }

    public CastManager() {
        this.casts = new ArrayList<>();
        this.movieScriptCache = null;
        this.paletteCache = null;
    }

    public static CastManager empty() {
        return new CastManager();
    }

    /**
     * Get cast by number (1-indexed).
     */
    public CastLib getCast(int number) throws ScriptError {
        CastLib cast = getCastOrNull(number);
        if (cast == null) {
            throw new ScriptError("Cast not found: " + number);
        }
        return cast;
    }

    /**
     * Get cast by number or null (1-indexed).
     */
    public CastLib getCastOrNull(int number) {
        int index = number - 1;
        if (index >= 0 && index < casts.size()) {
            return casts.get(index);
        }
        return null;
    }

    /**
     * Get mutable cast by number (1-indexed).
     */
    public CastLib getCastMut(int number) {
        return casts.get(number - 1);
    }

    /**
     * Get cast by name (case-insensitive).
     */
    public CastLib getCastByName(String name) {
        String target = name.toLowerCase();
        for (CastLib cast : casts) {
            if (cast.name.toLowerCase().equals(target)) {
                return cast;
            }
        }
        return null;
    }

    /**
     * Find member reference by number, searching all casts.
     */
    public CastMemberRef findMemberRefByNumber(int number) {
        for (CastLib cast : casts) {
            for (CastMember member : cast.members.values()) {
                if (member.number == number ||
                    getCastSlotNumber(cast.number, member.number) == number) {
                    return new CastMemberRef(cast.number, member.number);
                }
            }
        }
        return null;
    }

    /**
     * Find member reference by name, searching all casts.
     */
    public CastMemberRef findMemberRefByName(String name) {
        for (CastLib cast : casts) {
            CastMember member = cast.findMemberByName(name);
            if (member != null) {
                return new CastMemberRef(cast.number, member.number);
            }
        }
        return null;
    }

    /**
     * Find member reference by identifiers (name or number, optional cast).
     */
    public CastMemberRef findMemberRefByIdentifiers(Datum memberNameOrNum, Datum castNameOrNum) throws ScriptError {
        // Determine cast library
        CastLib castLib = null;
        if (castNameOrNum != null && !castNameOrNum.isVoid()) {
            if (castNameOrNum.isString()) {
                castLib = getCastByName(castNameOrNum.stringValue());
            } else if (castNameOrNum.isNumber()) {
                int intVal = castNameOrNum.intValue();
                if (intVal > 0) {
                    castLib = getCastOrNull(intVal);
                }
            } else {
                throw new ScriptError("Cast number or name invalid: " + castNameOrNum.getTypeString());
            }
        }

        // Find member
        if (memberNameOrNum.isString()) {
            String name = memberNameOrNum.stringValue();
            if (castLib != null) {
                CastMember member = castLib.findMemberByName(name);
                if (member != null) {
                    return new CastMemberRef(castLib.number, member.number);
                }
            } else {
                return findMemberRefByName(name);
            }
        } else if (memberNameOrNum.isNumber()) {
            int num = memberNameOrNum.intValue();
            if (castLib != null) {
                CastMember member = castLib.findMemberByNumber(num);
                if (member != null) {
                    return new CastMemberRef(castLib.number, member.number);
                }
            } else {
                return findMemberRefByNumber(num);
            }
        } else {
            throw new ScriptError("Member number or name type invalid: " + memberNameOrNum.getTypeString());
        }

        return null;
    }

    /**
     * Find member by identifiers.
     */
    public CastMember findMemberByIdentifiers(Datum memberNameOrNum, Datum castNameOrNum) throws ScriptError {
        CastMemberRef ref = findMemberRefByIdentifiers(memberNameOrNum, castNameOrNum);
        if (ref != null) {
            return findMemberByRef(ref);
        }
        return null;
    }

    /**
     * Find member by reference.
     */
    public CastMember findMemberByRef(CastMemberRef memberRef) {
        if (memberRef == null || !memberRef.isValid()) {
            return null;
        }
        int slotNumber = getCastSlotNumber(memberRef.getCastLib(), memberRef.getCastMember());
        return findMemberBySlotNumber(slotNumber);
    }

    /**
     * Find member by slot number.
     */
    public CastMember findMemberBySlotNumber(int slotNumber) {
        CastMemberRef memberRef = memberRefFromSlotNumber(slotNumber);
        if (!memberRef.isValid()) {
            return null;
        }
        if (memberRef.getCastLib() > 0) {
            CastLib cast = getCastOrNull(memberRef.getCastLib());
            if (cast != null) {
                return cast.findMemberByNumber(memberRef.getCastMember());
            }
        } else {
            for (CastLib cast : casts) {
                CastMember member = cast.findMemberByNumber(memberRef.getCastMember());
                if (member != null) {
                    return member;
                }
            }
        }
        return null;
    }

    /**
     * Get script by member reference.
     */
    public Script getScriptByRef(CastMemberRef memberRef) {
        if (memberRef == null || !memberRef.isValid()) {
            return null;
        }
        try {
            CastLib cast = getCast(memberRef.getCastLib());
            return cast.getScriptForMember(memberRef.getCastMember());
        } catch (ScriptError e) {
            return null;
        }
    }

    /**
     * Get field value by identifiers.
     */
    public String getFieldValueByIdentifiers(Datum memberNameOrNum, Datum castNameOrNum) throws ScriptError {
        CastMember member = findMemberByIdentifiers(memberNameOrNum, castNameOrNum);
        if (member == null) {
            throw new ScriptError("Cast member not found");
        }
        if (member.getMemberType() != CastMemberType.Field) {
            throw new ScriptError("Cast member is not a field");
        }
        // TODO: Get field text
        return "";
    }

    /**
     * Remove member with reference.
     */
    public void removeMemberWithRef(CastMemberRef memberRef) throws ScriptError {
        if (memberRef.getCastLib() <= 0 || memberRef.getCastLib() > casts.size()) {
            throw new ScriptError("Cannot remove member with invalid cast lib");
        }
        CastLib cast = getCastMut(memberRef.getCastLib());
        cast.removeMember(memberRef.getCastMember());
    }

    /**
     * Clear movie script cache.
     */
    public void clearMovieScriptCache() {
        movieScriptCache = null;
    }

    /**
     * Get movie scripts from all casts.
     */
    public List<Script> getMovieScripts() {
        if (movieScriptCache == null) {
            movieScriptCache = new ArrayList<>();
            for (CastLib cast : casts) {
                for (Script script : cast.scripts.values()) {
                    if (script.getScriptType() == ScriptType.Movie) {
                        movieScriptCache.add(script);
                    }
                }
            }
        }
        return movieScriptCache;
    }

    /**
     * Invalidate palette cache.
     */
    public void invalidatePaletteCache() {
        paletteCache = null;
    }

    /**
     * Get palettes from all casts.
     */
    public PaletteMap palettes() {
        if (paletteCache == null) {
            paletteCache = new PaletteMap();
            for (CastLib cast : casts) {
                for (CastMember member : cast.members.values()) {
                    if (member.getMemberType() == CastMemberType.Palette) {
                        int slotNumber = getCastSlotNumber(cast.number, member.number);
                        // Store palette in map
                        // TODO: Get palette data from member
                    }
                }
            }
        }
        return paletteCache;
    }

    /**
     * Get number of cast libraries.
     */
    public int getCastLibCount() {
        return casts.size();
    }

    // Helper methods for slot number calculations

    /**
     * Calculate slot number from cast lib and member number.
     */
    public static int getCastSlotNumber(int castLib, int castMember) {
        // Slot number formula: (castLib * 512) + castMember
        // Or for compatibility: (castLib << 16) | castMember
        return (castLib << 16) | castMember;
    }

    /**
     * Get member reference from slot number.
     */
    public static CastMemberRef memberRefFromSlotNumber(int slotNumber) {
        int castLib = (slotNumber >> 16) & 0xFFFF;
        int castMember = slotNumber & 0xFFFF;
        return new CastMemberRef(castLib, castMember);
    }

    // Legacy compatibility methods

    public List<CastLib> getCastLibs() {
        return casts;
    }

    public CastLib getCastLib(int index) {
        if (index >= 0 && index < casts.size()) {
            return casts.get(index);
        }
        return null;
    }

    public CastMember getMember(CastMemberRef ref) {
        return findMemberByRef(ref);
    }

    public CastMember getMember(int castLib, int castMember) {
        return findMemberByRef(new CastMemberRef(castLib, castMember));
    }

    public void addMember(CastMemberRef ref, CastMember member) {
        CastLib cast = getCastOrNull(ref.getCastLib());
        if (cast != null) {
            cast.insertMember(ref.getCastMember(), member);
        }
    }

    public CastMemberRef findMemberByName(String name) {
        return findMemberRefByName(name);
    }
}
