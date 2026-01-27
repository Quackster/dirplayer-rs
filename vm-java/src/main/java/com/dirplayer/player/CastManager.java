package com.dirplayer.player;

import com.dirplayer.director.MemberType;
import com.dirplayer.director.ScriptType;
import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.bitmap.PaletteMap;
import com.dirplayer.player.cast.CastMemberType;
import com.dirplayer.player.cast.PaletteMember;
import com.dirplayer.player.script.Script;
import com.dirplayer.SimpleLogger;


import java.util.ArrayList;
import java.util.List;

/**
 * Manager for cast libraries and cast members.
 * Port of Rust CastManager struct.
 */
public class CastManager {
    private static final SimpleLogger logger = SimpleLogger.getLogger(CastManager.class);

    public List<CastLib> casts;
    public java.util.Map<Integer, CastLib> castLibs;
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
        this.castLibs = new java.util.HashMap<>();
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
     * Find cast library number by name (case-insensitive).
     * Returns null if not found.
     */
    public Integer findCastLibByName(String name) {
        CastLib cast = getCastByName(name);
        if (cast != null) {
            return cast.number;
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
                throw new ScriptError("Cast number or name invalid: " + castNameOrNum.getTypeName());
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
            throw new ScriptError("Member number or name type invalid: " + memberNameOrNum.getTypeName());
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
        if (!member.isField() && !member.isText()) {
            throw new ScriptError("Cast member is not a field or text member");
        }
        String text = member.getText();
        return text != null ? text : "";
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
                    if (member.getMemberType() == MemberType.Palette) {
                        int slotNumber = getCastSlotNumber(cast.number, member.number);
                        // Extract palette data from member specificData
                        if (member.specificData instanceof PaletteMember) {
                            PaletteMember paletteMember = (PaletteMember) member.specificData;
                            paletteCache.addCastPalette(slotNumber, paletteMember.colors);
                        }
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

    /**
     * Insert a member at a specific cast library and member number.
     */
    public void insertMember(int castLib, int memberNumber, CastMember member) {
        CastLib cast = getCastOrNull(castLib);
        if (cast != null) {
            cast.insertMember(memberNumber, member);
        }
    }

    /**
     * Remove a member by reference.
     */
    public void removeMember(CastMemberRef memberRef) {
        if (memberRef != null && memberRef.isValid()) {
            CastLib cast = getCastOrNull(memberRef.getCastLib());
            if (cast != null) {
                cast.removeMember(memberRef.getCastMember());
            }
        }
    }

    /**
     * Load cast libraries and members from a DirectorFile, with external cast preloading.
     * Port of Rust load_from_dir method.
     */
    public void loadFromDir(com.dirplayer.director.DirectorFile dirFile,
                            NetManager netManager,
                            com.dirplayer.player.bitmap.BitmapManager bitmapManager,
                            java.util.Map<String, com.dirplayer.director.DirectorFile> dirCache) {
        loadFromDirInternal(dirFile, bitmapManager, netManager);

        // Preload external casts based on preload mode
        if (netManager != null && dirCache != null) {
            preloadCasts(CastPreloadReason.MovieLoaded, netManager, bitmapManager, dirCache);
        }
    }

    /**
     * Load cast libraries and members from a DirectorFile.
     * Port of Rust load_from_dir method.
     */
    public void loadFromDir(com.dirplayer.director.DirectorFile dirFile,
                            com.dirplayer.player.bitmap.BitmapManager bitmapManager) {
        loadFromDirInternal(dirFile, bitmapManager, null);
    }

    /**
     * Internal method to load cast libraries.
     */
    private void loadFromDirInternal(com.dirplayer.director.DirectorFile dirFile,
                                      com.dirplayer.player.bitmap.BitmapManager bitmapManager,
                                      NetManager netManager) {
        if (dirFile == null) {
            return;
        }

        // Set base path on net manager for resolving relative URLs
        if (netManager != null && dirFile.basePath != null) {
            netManager.setBasePath(dirFile.basePath.toString());
        }

        List<CastLib> loadedCasts = new ArrayList<>();

        // Iterate through cast entries
        for (int index = 0; index < dirFile.castEntries.size(); index++) {
            com.dirplayer.director.chunks.CastListChunk.CastListEntry castEntry = dirFile.castEntries.get(index);

            // Find matching CastDef
            com.dirplayer.director.CastDef castDef = null;
            for (com.dirplayer.director.CastDef def : dirFile.casts) {
                if (def.id == castEntry.id) {
                    castDef = def;
                    break;
                }
            }

            // Normalize cast lib path
            String castFileName = normalizeCastLibPath(
                netManager != null ? netManager.basePath : null,
                castEntry.filePath);

            // Create CastLib
            CastLib cast = new CastLib();
            cast.name = castEntry.name;
            cast.fileName = castFileName;
            cast.number = index + 1;  // 1-indexed
            cast.isExternal = castDef == null;
            cast.state = castDef != null ? CastLib.CastLibState.Loaded : CastLib.CastLibState.None;
            cast.preloadMode = castEntry.preloadSettings;

            // Apply cast definition if available
            if (castDef != null) {
                applyCastDef(cast, dirFile, castDef, bitmapManager);
                clearMovieScriptCache();
            } else if (cast.fileName != null && !cast.fileName.isEmpty()) {
                // External cast file needs to be loaded
                logger.warn("External cast library '{}' references file '{}' which needs to be loaded separately",
                    cast.name.isEmpty() ? "Cast " + cast.number : cast.name, cast.fileName);
            }

            loadedCasts.add(cast);
        }

        this.casts = loadedCasts;
        logger.debug("Loaded {} cast libraries", casts.size());
    }

    /**
     * Apply cast definition to a cast library, loading all members.
     */
    private void applyCastDef(CastLib cast, com.dirplayer.director.DirectorFile dirFile,
                               com.dirplayer.director.CastDef castDef,
                               com.dirplayer.player.bitmap.BitmapManager bitmapManager) {
        cast.dirVersion = dirFile.version;

        // Populate scriptContext with names from lnam
        if (castDef.lnam != null && castDef.lnam.names != null) {
            cast.scriptContext.names.clear();
            cast.scriptContext.names.addAll(castDef.lnam.names);
        }

        // Load each member from the cast definition
        for (java.util.Map.Entry<Integer, com.dirplayer.director.CastDef.CastMemberDef> entry : castDef.members.entrySet()) {
            int memberNumber = entry.getKey();
            com.dirplayer.director.CastDef.CastMemberDef memberDef = entry.getValue();

            CastMember member = createMemberFromDef(cast.number, memberNumber, memberDef, castDef, bitmapManager);
            if (member != null) {
                cast.insertMember(memberNumber, member);

                // Create script if this is a script member
                if (member.getMemberType() == MemberType.Script && castDef.lctx != null) {
                    Script script = createScriptFromMember(cast, memberNumber, member, memberDef, castDef);
                    if (script != null) {
                        cast.scripts.put(memberNumber, script);
                    }
                }
            }
        }

        logger.debug("Applied cast def to cast {}: {} members, {} scripts",
            cast.number, cast.members.size(), cast.scripts.size());
    }

    /**
     * Create a CastMember from a CastMemberDef.
     */
    private CastMember createMemberFromDef(int castLibNum, int memberNumber,
                                            com.dirplayer.director.CastDef.CastMemberDef memberDef,
                                            com.dirplayer.director.CastDef castDef,
                                            com.dirplayer.player.bitmap.BitmapManager bitmapManager) {
        if (memberDef.chunk == null) {
            return null;
        }

        CastMember member = new CastMember();
        member.number = memberNumber;
        member.memberRef = new CastMemberRef(castLibNum, memberNumber);
        member.memberType = memberDef.chunk.memberType;
        member.type = memberDef.chunk.memberType;
        member.isLoaded = true;

        // Set name from member info
        if (memberDef.chunk.memberInfo != null) {
            member.name = memberDef.chunk.memberInfo.name;
        }

        // Load type-specific data using specificData from the chunk
        com.dirplayer.director.chunks.CastMemberSpecificData specificData = memberDef.chunk.specificData;

        switch (memberDef.chunk.memberType) {
            case Bitmap:
                loadBitmapMember(member, memberDef, specificData, bitmapManager);
                break;
            case Text:
            case RTE:
            case Button:
                loadTextMember(member, memberDef, specificData);
                break;
            case Sound:
                loadSoundMember(member, memberDef);
                break;
            case Script:
                loadScriptMember(member, specificData);
                break;
            case Palette:
                loadPaletteMember(member, memberDef);
                break;
            case Shape:
                loadShapeMember(member, specificData);
                break;
            case FilmLoop:
                loadFilmLoopMember(member, specificData);
                break;
            default:
                // Other types keep default values
                break;
        }

        return member;
    }

    /**
     * Load bitmap-specific data into a member.
     * Matches Rust behavior: always creates a bitmap reference, using placeholder if data is missing.
     */
    private void loadBitmapMember(CastMember member,
                                   com.dirplayer.director.CastDef.CastMemberDef memberDef,
                                   com.dirplayer.director.chunks.CastMemberSpecificData specificData,
                                   com.dirplayer.player.bitmap.BitmapManager bitmapManager) {
        com.dirplayer.director.BitmapInfo info = specificData != null ? specificData.getBitmapInfo() : null;
        if (info != null) {
            member.bitmapWidth = info.width;
            member.bitmapHeight = info.height;
            member.bitDepth = info.bitDepth;
            member.regPointX = info.regX;
            member.regPointY = info.regY;
            member.paletteRef = info.paletteId;
        }

        if (bitmapManager == null) {
            return;
        }

        // Load bitmap data if available, otherwise create placeholder (matching Rust behavior)
        try {
            com.dirplayer.player.bitmap.Bitmap bitmap = null;

            if (memberDef.bitmap != null && memberDef.bitmap.data != null &&
                memberDef.bitmap.data.length > 0 && info != null) {
                // Decode bitmap data using decompressBitmap
                bitmap = com.dirplayer.player.bitmap.BitmapDecoder.decompressBitmap(
                    memberDef.bitmap.data, info, 0, 0);
                logger.debug("Decoded bitmap member {}: {}x{} {} bytes",
                    member.number, bitmap.getWidth(), bitmap.getHeight(), memberDef.bitmap.data.length);
            } else {
                // No bitmap data - create placeholder (matching Rust behavior)
                int width = info != null ? Math.max(1, info.width) : 1;
                int height = info != null ? Math.max(1, info.height) : 1;
                int bitDepth = info != null ? info.bitDepth : 8;
                bitmap = new com.dirplayer.player.bitmap.Bitmap(
                    width, height, bitDepth, bitDepth, 0,
                    com.dirplayer.player.bitmap.PaletteRef.ofBuiltIn(
                        com.dirplayer.player.bitmap.BuiltInPalette.GrayScale));
                logger.debug("Created placeholder bitmap for member {}: {}x{} (no BITD data)",
                    member.number, width, height);
            }

            int bitmapId = bitmapManager.addBitmap(bitmap);
            int bitDepth = info != null ? info.bitDepth : 8;
            member.bitmap = new com.dirplayer.player.bitmap.BitmapRef(
                bitmapId, bitmap.getWidth(), bitmap.getHeight(), bitDepth);
        } catch (Exception e) {
            logger.warn("Failed to decode bitmap for member {}: {}", member.number, e.getMessage());
            // Create placeholder on failure (matching Rust behavior)
            com.dirplayer.player.bitmap.Bitmap placeholder = new com.dirplayer.player.bitmap.Bitmap(
                1, 1, 8, 8, 0,
                com.dirplayer.player.bitmap.PaletteRef.ofBuiltIn(
                    com.dirplayer.player.bitmap.BuiltInPalette.GrayScale));
            int bitmapId = bitmapManager.addBitmap(placeholder);
            member.bitmap = new com.dirplayer.player.bitmap.BitmapRef(bitmapId, 1, 1, 8);
        }
    }

    /**
     * Load text-specific data into a member.
     */
    private void loadTextMember(CastMember member,
                                 com.dirplayer.director.CastDef.CastMemberDef memberDef,
                                 com.dirplayer.director.chunks.CastMemberSpecificData specificData) {
        if (memberDef.text != null) {
            member.text = memberDef.text.text;
        }
        com.dirplayer.director.FieldInfo fieldInfo = specificData.getFieldInfo();
        if (fieldInfo != null) {
            member.textWidth = fieldInfo.width;
            member.textHeight = fieldInfo.height;
        }
    }

    /**
     * Load sound-specific data into a member.
     */
    private void loadSoundMember(CastMember member,
                                  com.dirplayer.director.CastDef.CastMemberDef memberDef) {
        if (memberDef.sound != null) {
            member.sampleRate = memberDef.sound.getSampleRate();
            member.channels = memberDef.sound.getChannels();
            member.sampleCount = memberDef.sound.getSampleCount();
        }
    }

    /**
     * Load script-specific data into a member.
     */
    private void loadScriptMember(CastMember member,
                                   com.dirplayer.director.chunks.CastMemberSpecificData specificData) {
        ScriptType scriptType = specificData.getScriptType();
        if (scriptType != null) {
            member.scriptType = scriptType.ordinal();
        }
    }

    /**
     * Load palette-specific data into a member.
     */
    private void loadPaletteMember(CastMember member,
                                    com.dirplayer.director.CastDef.CastMemberDef memberDef) {
        if (memberDef.palette != null && memberDef.palette.colors != null) {
            // Convert palette colors to PaletteMember format
            java.util.List<int[]> colorList = memberDef.palette.colors;
            PaletteMember paletteMember = new PaletteMember(colorList.size());
            for (int i = 0; i < colorList.size(); i++) {
                int[] rgb = colorList.get(i);
                if (rgb.length >= 3) {
                    paletteMember.setColor(i, rgb[0], rgb[1], rgb[2]);
                }
            }
            member.specificData = paletteMember;
        }
    }

    /**
     * Load shape-specific data into a member.
     */
    private void loadShapeMember(CastMember member,
                                  com.dirplayer.director.chunks.CastMemberSpecificData specificData) {
        com.dirplayer.director.ShapeInfo shapeInfo = specificData.getShapeInfo();
        if (shapeInfo != null) {
            // Shape info is stored in specificData if needed
        }
    }

    /**
     * Load filmloop-specific data into a member.
     */
    private void loadFilmLoopMember(CastMember member,
                                     com.dirplayer.director.chunks.CastMemberSpecificData specificData) {
        com.dirplayer.director.FilmLoopInfo filmLoopInfo = specificData.getFilmLoopInfo();
        if (filmLoopInfo != null) {
            // FilmLoop data is processed separately
        }
    }

    /**
     * Create a Script object from a script member.
     */
    private Script createScriptFromMember(CastLib cast, int memberNumber, CastMember member,
                                           com.dirplayer.director.CastDef.CastMemberDef memberDef,
                                           com.dirplayer.director.CastDef castDef) {
        if (memberDef.script == null || castDef.lctx == null) {
            return null;
        }

        try {
            // Get script type from specific data
            ScriptType scriptType = ScriptType.Unknown;
            if (memberDef.chunk.specificData != null) {
                ScriptType st = memberDef.chunk.specificData.getScriptType();
                if (st != null) {
                    scriptType = st;
                }
            }

            // Create script from script chunk
            Script script = new Script(
                new CastMemberRef(cast.number, memberNumber),
                member.name,
                memberDef.script,
                scriptType
            );

            // Populate handlers map from script chunk using lnam names
            if (castDef.lnam != null && memberDef.script.handlers != null) {
                java.util.List<String> names = castDef.lnam.names;
                for (com.dirplayer.director.chunks.HandlerDef handler : memberDef.script.handlers) {
                    if (handler.nameId >= 0 && handler.nameId < names.size()) {
                        String handlerName = names.get(handler.nameId);
                        script.handlers.put(handlerName.toLowerCase(), handler);
                        script.handlerNames.add(handlerName);
                    }
                }
            }

            return script;
        } catch (Exception e) {
            logger.warn("Failed to create script: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Preload external casts based on their preload mode.
     * Port of Rust CastManager::preload_casts method.
     */
    public void preloadCasts(CastPreloadReason reason,
                              NetManager netManager,
                              com.dirplayer.player.bitmap.BitmapManager bitmapManager,
                              java.util.Map<String, com.dirplayer.director.DirectorFile> dirCache) {
        for (CastLib cast : casts) {
            if (cast.isExternal &&
                cast.state == CastLib.CastLibState.None &&
                cast.fileName != null &&
                !cast.fileName.isEmpty()) {

                logger.debug("Cast File {} - Preload Mode: {}", cast.fileName, cast.preloadMode);

                switch (cast.preloadMode) {
                    case 0:
                        // Preload: When Needed - don't preload now
                        break;
                    case 1:
                        // Preload: After frame one
                        if (reason == CastPreloadReason.AfterFrameOne) {
                            cast.preload(netManager, bitmapManager, dirCache);
                            clearMovieScriptCache();
                        }
                        break;
                    case 2:
                        // Preload: Before frame one
                        if (reason == CastPreloadReason.MovieLoaded) {
                            cast.preload(netManager, bitmapManager, dirCache);
                            clearMovieScriptCache();
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Normalize a cast library file path.
     * Port of Rust normalize_cast_lib_path function.
     *
     * This function extracts the base filename from absolute paths
     * (e.g., "D:\LINGO\Work\fuse_client.cst" -> "fuse_client.cct")
     * and resolves it relative to the base path.
     */
    private static String normalizeCastLibPath(String basePath, String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        // Normalize slashes
        String normalized = filePath.replace("\\", "/");

        // Split on both slashes and colons to get filename components
        String[] parts = normalized.split("[/:]");
        String fileBaseName = parts.length > 0 ? parts[parts.length - 1] : "";

        if (fileBaseName.isEmpty()) {
            return "";
        }

        // Change extension from .cst to .cct (compressed cast)
        String castFileName;
        int dotIndex = fileBaseName.lastIndexOf('.');
        if (dotIndex > 0) {
            castFileName = fileBaseName.substring(0, dotIndex) + ".cct";
        } else {
            castFileName = fileBaseName + ".cct";
        }

        // Make ASCII-safe (remove non-ASCII characters)
        castFileName = asciiSafe(castFileName);

        // If no base path, return just the filename
        if (basePath == null || basePath.isEmpty()) {
            return castFileName;
        }

        // Resolve relative to base path
        try {
            java.net.URI baseUri = new java.net.URI(basePath);
            java.net.URI resolved = baseUri.resolve(castFileName);
            return resolved.toString();
        } catch (Exception e) {
            return castFileName;
        }
    }

    /**
     * Make a string ASCII-safe by removing non-ASCII characters.
     */
    private static String asciiSafe(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c < 128) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Load fonts from cast members into the font manager.
     */
    public void loadFontsIntoManager(com.dirplayer.player.FontManager fontManager) {
        if (fontManager == null) {
            return;
        }

        for (CastLib cast : casts) {
            for (CastMember member : cast.members.values()) {
                if (member.getMemberType() == MemberType.Font) {
                    // Font members would be loaded here
                    // For now, most Director content uses system fonts
                    logger.debug("Found font member: {} in cast {}", member.name, cast.number);
                }
            }
        }
    }
}
