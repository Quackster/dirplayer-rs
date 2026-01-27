package com.dirplayer.player;

import com.dirplayer.director.ScriptType;
import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.cast.CastMemberData;
import com.dirplayer.player.script.Script;
import com.dirplayer.SimpleLogger;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cast library containing cast members.
 * Port of Rust CastLib struct.
 */
public class CastLib {
    private static final SimpleLogger logger = SimpleLogger.getLogger(CastLib.class);

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
    public com.dirplayer.director.lingo.ScriptContext scriptContext;  // Holds names from lnam

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
        this.scriptContext = new com.dirplayer.director.lingo.ScriptContext();
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

    /**
     * Preload this cast library from its external file.
     * Port of Rust CastLib::preload method.
     */
    public void preload(NetManager netManager,
                        com.dirplayer.player.bitmap.BitmapManager bitmapManager,
                        java.util.Map<String, com.dirplayer.director.DirectorFile> dirCache) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }

        // Check cache first
        if (dirCache.containsKey(fileName)) {
            loadFromDirFile(dirCache.get(fileName), fileName, bitmapManager);
            return;
        }

        logger.info("Loading cast {}", fileName);
        state = CastLibState.Loading;

        int taskId = netManager.preloadNetThing(fileName);
        if (!netManager.isTaskDone(taskId)) {
            netManager.awaitTask(taskId);
        }

        NetTask task = netManager.getTask(taskId);
        NetTask.NetResult result = netManager.getTaskResult(taskId);

        if (task != null && result != null) {
            onCastPreloadResult(result, task.resolvedUrl, bitmapManager, dirCache);
        } else {
            logger.warn("Failed to preload cast {}: task or result is null", fileName);
            state = CastLibState.None;
        }
    }

    /**
     * Handle the result of a cast preload operation.
     */
    private void onCastPreloadResult(NetTask.NetResult result,
                                      java.net.URI resolvedUrl,
                                      com.dirplayer.player.bitmap.BitmapManager bitmapManager,
                                      java.util.Map<String, com.dirplayer.director.DirectorFile> dirCache) {
        String loadFileName = resolvedUrl.toString();

        if (result.isOk()) {
            try {
                byte[] castBytes = result.getData();
                String baseUrl = getBaseUrl(resolvedUrl);

                com.dirplayer.director.DirectorFile castFile =
                    com.dirplayer.director.DirectorFile.readBytes(castBytes, loadFileName, baseUrl);

                dirCache.put(loadFileName, castFile);
                loadFromDirFile(castFile, loadFileName, bitmapManager);
                return;
            } catch (Exception e) {
                logger.warn("Could not parse {}: {}", loadFileName, e.getMessage());
            }
        } else {
            logger.warn("Fetching {} failed", loadFileName);
        }

        state = CastLibState.None;
    }

    /**
     * Load this cast from a DirectorFile.
     */
    private void loadFromDirFile(com.dirplayer.director.DirectorFile file,
                                  String loadFileName,
                                  com.dirplayer.player.bitmap.BitmapManager bitmapManager) {
        clear();

        this.fileName = loadFileName;
        this.state = CastLibState.Loaded;

        if (name == null || name.isEmpty()) {
            name = getBasenameNoExtension(loadFileName);
        }

        if (!file.casts.isEmpty()) {
            com.dirplayer.director.CastDef castDef = file.casts.get(0);
            applyCastDef(file, castDef, bitmapManager);
        }

        logger.debug("Loaded cast {}: {} members, {} scripts", loadFileName, members.size(), scripts.size());
    }

    /**
     * Apply a cast definition to this cast library.
     */
    public void applyCastDef(com.dirplayer.director.DirectorFile dirFile,
                              com.dirplayer.director.CastDef castDef,
                              com.dirplayer.player.bitmap.BitmapManager bitmapManager) {
        this.dirVersion = dirFile.version;

        // Populate scriptContext with names from lnam
        if (castDef.lnam != null && castDef.lnam.names != null) {
            this.scriptContext.names.clear();
            this.scriptContext.names.addAll(castDef.lnam.names);
        }

        // Load members from cast definition
        int scriptMembers = 0;
        int scriptsCreated = 0;
        boolean hasLctx = castDef.lctx != null;

        for (java.util.Map.Entry<Integer, com.dirplayer.director.CastDef.CastMemberDef> entry :
                castDef.members.entrySet()) {
            int memberId = entry.getKey();
            com.dirplayer.director.CastDef.CastMemberDef memberDef = entry.getValue();

            CastMember member = createMemberFromDef(memberId, memberDef, castDef, bitmapManager);
            if (member != null) {
                insertMember(memberId, member);

                // Create script if this is a script member
                if (member.getMemberType() == com.dirplayer.director.MemberType.Script) {
                    scriptMembers++;
                    if (castDef.lctx != null) {
                        Script script = createScriptFromMember(memberId, member, memberDef, castDef);
                        if (script != null) {
                            scripts.put(memberId, script);
                            scriptsCreated++;
                        }
                    }
                }
            }
        }

        if (scriptMembers > 0) {
            logger.info("Cast {}: {} script members, {} scripts created, hasLctx={}",
                name, scriptMembers, scriptsCreated, hasLctx);
        }

        logger.debug("Applied cast def to cast {}: {} members, {} scripts",
            number, members.size(), scripts.size());
    }

    /**
     * Create a CastMember from a CastMemberDef.
     */
    private CastMember createMemberFromDef(int memberNumber,
                                            com.dirplayer.director.CastDef.CastMemberDef memberDef,
                                            com.dirplayer.director.CastDef castDef,
                                            com.dirplayer.player.bitmap.BitmapManager bitmapManager) {
        if (memberDef.chunk == null) {
            return null;
        }

        CastMember member = new CastMember();
        member.number = memberNumber;
        member.memberRef = new CastMemberRef(this.number, memberNumber);
        member.memberType = memberDef.chunk.memberType;
        member.type = memberDef.chunk.memberType;
        member.isLoaded = true;

        // Set name from member info
        if (memberDef.chunk.memberInfo != null) {
            member.name = memberDef.chunk.memberInfo.name;
        }

        // Load type-specific data
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
            default:
                break;
        }

        return member;
    }

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

        if (memberDef.bitmap != null && bitmapManager != null && info != null) {
            try {
                com.dirplayer.player.bitmap.Bitmap bitmap = new com.dirplayer.player.bitmap.Bitmap(
                    info.width, info.height, info.bitDepth, info.bitDepth, 0,
                    com.dirplayer.player.bitmap.PaletteRef.ofBuiltIn(
                        com.dirplayer.player.bitmap.BuiltInPalette.SystemWin));

                if (memberDef.bitmap.data != null && memberDef.bitmap.data.length > 0) {
                    bitmap = com.dirplayer.player.bitmap.BitmapDecoder.decompressBitmap(
                        memberDef.bitmap.data, info, 0, 0);
                }

                int bitmapId = bitmapManager.addBitmap(bitmap);
                member.bitmap = new com.dirplayer.player.bitmap.BitmapRef(
                    bitmapId, bitmap.getWidth(), bitmap.getHeight(), info.bitDepth);
            } catch (Exception e) {
                logger.warn("Failed to decode bitmap: {}", e.getMessage());
            }
        }
    }

    private void loadTextMember(CastMember member,
                                 com.dirplayer.director.CastDef.CastMemberDef memberDef,
                                 com.dirplayer.director.chunks.CastMemberSpecificData specificData) {
        if (memberDef.text != null) {
            member.text = memberDef.text.text;
        }
        com.dirplayer.director.FieldInfo fieldInfo = specificData != null ? specificData.getFieldInfo() : null;
        if (fieldInfo != null) {
            member.textWidth = fieldInfo.width;
            member.textHeight = fieldInfo.height;
        }
    }

    private void loadSoundMember(CastMember member,
                                  com.dirplayer.director.CastDef.CastMemberDef memberDef) {
        if (memberDef.sound != null) {
            member.sampleRate = memberDef.sound.getSampleRate();
            member.channels = memberDef.sound.getChannels();
            member.sampleCount = memberDef.sound.getSampleCount();
        }
    }

    private void loadScriptMember(CastMember member,
                                   com.dirplayer.director.chunks.CastMemberSpecificData specificData) {
        if (specificData != null) {
            ScriptType scriptType = specificData.getScriptType();
            if (scriptType != null) {
                member.scriptType = scriptType.ordinal();
            }
        }
    }

    private Script createScriptFromMember(int memberNumber, CastMember member,
                                           com.dirplayer.director.CastDef.CastMemberDef memberDef,
                                           com.dirplayer.director.CastDef castDef) {
        if (memberDef.script == null) {
            // Script chunk not loaded for this member
            return null;
        }
        if (castDef.lctx == null) {
            logger.warn("Cannot create script for member {}: no lctx", memberNumber);
            return null;
        }

        try {
            ScriptType scriptType = ScriptType.Unknown;
            if (memberDef.chunk.specificData != null) {
                ScriptType st = memberDef.chunk.specificData.getScriptType();
                if (st != null) {
                    scriptType = st;
                }
            }

            Script script = new Script(
                new CastMemberRef(this.number, memberNumber),
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
     * Get base URL from a URI.
     */
    private static String getBaseUrl(java.net.URI uri) {
        String uriStr = uri.toString();
        int lastSlash = uriStr.lastIndexOf('/');
        if (lastSlash > 0) {
            return uriStr.substring(0, lastSlash + 1);
        }
        return uriStr;
    }

    /**
     * Get basename without extension from a path.
     */
    private static String getBasenameNoExtension(String path) {
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String basename = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        int lastDot = basename.lastIndexOf('.');
        return lastDot > 0 ? basename.substring(0, lastDot) : basename;
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
