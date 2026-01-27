package com.dirplayer.director;

import com.dirplayer.director.chunks.*;
import com.dirplayer.io.BinaryReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.dirplayer.SimpleLogger;


/**
 * CastDef - represents a cast library with its members.
 * Port of Rust CastDef struct.
 */
public class CastDef {
    private static final SimpleLogger logger = SimpleLogger.getLogger(CastDef.class);

    public String name;
    public int id;
    public int minMember;
    public List<Integer> memberIds;
    public Map<Integer, CastMemberDef> members;
    public ScriptContextChunk lctx;
    public ScriptNamesChunk lnam;

    public CastDef() {
        this.members = new HashMap<>();
    }

    public static CastDef from(String name, int id, int minMember, List<Integer> memberIds,
                                BinaryReader reader, DirectorFile.ChunkContainer chunkContainer,
                                RIFXReaderContext rifx, KeyTableChunk keyTable) {
        CastDef castDef = new CastDef();
        castDef.name = name;
        castDef.id = id;
        castDef.minMember = minMember;
        castDef.memberIds = memberIds;

        // Find script context entry for this cast
        KeyTableChunk.KeyTableEntry lctxEntry = null;
        for (KeyTableChunk.KeyTableEntry entry : keyTable.entries) {
            if (entry.castId == id &&
                (entry.fourcc == Utils.FOURCC("Lctx") || entry.fourcc == Utils.FOURCC("LctX"))) {
                lctxEntry = entry;
                if (entry.fourcc == Utils.FOURCC("LctX")) {
                    rifx.lctxCapitalX = true;
                }
                break;
            }
        }

        // Load script context if found
        if (lctxEntry != null) {
            Chunk chunk = DirectorFile.getChunk(reader, chunkContainer, rifx, lctxEntry.fourcc, lctxEntry.sectionId);
            if (chunk != null && chunk.asScriptContext() != null) {
                castDef.lctx = chunk.asScriptContext();

                // Load script names
                Chunk lnamChunk = DirectorFile.getChunk(reader, chunkContainer, rifx,
                    Utils.FOURCC("Lnam"), castDef.lctx.lnamSectionId);
                if (lnamChunk != null && lnamChunk.asScriptNames() != null) {
                    castDef.lnam = lnamChunk.asScriptNames();
                }
            }
        }

        // Load cast members - directly read CASt chunks by section ID (like Rust does)
        logger.debug("CastDef.from: loading {} memberIds", memberIds.size());

        int skippedZero = 0;
        int chunkLoadFailed = 0;

        for (int i = 0; i < memberIds.size(); i++) {
            int sectionId = memberIds.get(i);
            if (sectionId <= 0) {
                skippedZero++;
                continue;
            }

            int memberId = minMember + i;

            // Directly read the CASt chunk by section ID (don't look up in keyTable first)
            Chunk chunk = DirectorFile.getChunk(reader, chunkContainer, rifx, Utils.FOURCC("CASt"), sectionId);
            if (chunk == null || chunk.asCastMember() == null) {
                chunkLoadFailed++;
                continue;
            }

            CastMemberDef memberDef = new CastMemberDef();
            memberDef.chunk = chunk.asCastMember();
            memberDef.number = memberId;

            // Load associated script using the member's scriptId
            // The scriptId is an index (1-based) into the lctx.sectionMap
            if (castDef.lctx != null && memberDef.chunk.memberInfo != null &&
                memberDef.chunk.memberInfo.header != null) {
                int scriptId = memberDef.chunk.memberInfo.header.scriptId;
                if (scriptId > 0 && scriptId <= castDef.lctx.sectionMap.size()) {
                    // scriptId is 1-based, so subtract 1 to get index
                    ScriptContextChunk.ScriptContextMapEntry mapEntry =
                        castDef.lctx.sectionMap.get(scriptId - 1);
                    if (mapEntry.sectionId > 0) {
                        Chunk scriptChunk = DirectorFile.getChunk(reader, chunkContainer, rifx,
                            Utils.FOURCC("Lscr"), mapEntry.sectionId);
                        if (scriptChunk != null && scriptChunk.asScript() != null) {
                            memberDef.script = scriptChunk.asScript();
                        }
                    }
                }
            }

            // Load media data based on member type - use sectionId to find children in keyTable
            loadMemberMedia(memberDef, sectionId, reader, chunkContainer, rifx, keyTable);

            castDef.members.put(memberId, memberDef);
        }

        logger.debug("Cast {} loaded: {} members (skippedZero={}, chunkLoadFailed={})",
            name, castDef.members.size(), skippedZero, chunkLoadFailed);
        return castDef;
    }

    private static void loadMemberMedia(CastMemberDef memberDef, int memberId,
                                         BinaryReader reader, DirectorFile.ChunkContainer chunkContainer,
                                         RIFXReaderContext rifx, KeyTableChunk keyTable) {
        MemberType memberType = memberDef.chunk.memberType;

        // Find associated media chunks
        for (KeyTableChunk.KeyTableEntry entry : keyTable.entries) {
            if (entry.castId != memberId) continue;

            String fourccStr = Utils.fourccToString(entry.fourcc);

            switch (fourccStr) {
                case "BITD":
                    Chunk bitmapChunk = DirectorFile.getChunk(reader, chunkContainer, rifx, entry.fourcc, entry.sectionId);
                    if (bitmapChunk != null) {
                        memberDef.bitmap = bitmapChunk.asBitmap();
                    }
                    break;
                case "snd ":
                    Chunk soundChunk = DirectorFile.getChunk(reader, chunkContainer, rifx, entry.fourcc, entry.sectionId);
                    if (soundChunk != null) {
                        memberDef.sound = soundChunk.asSound();
                    }
                    break;
                case "STXT":
                    Chunk textChunk = DirectorFile.getChunk(reader, chunkContainer, rifx, entry.fourcc, entry.sectionId);
                    if (textChunk != null) {
                        memberDef.text = textChunk.asText();
                    }
                    break;
                case "CLUT":
                    Chunk paletteChunk = DirectorFile.getChunk(reader, chunkContainer, rifx, entry.fourcc, entry.sectionId);
                    if (paletteChunk != null) {
                        memberDef.palette = paletteChunk.asPalette();
                    }
                    break;
            }
        }
    }

    /**
     * Cast member definition - represents a single cast member.
     */
    public static class CastMemberDef {
        public int number;
        public CastMemberChunk chunk;
        public ScriptChunk script;
        public BitmapChunk bitmap;
        public SoundChunk sound;
        public TextChunk text;
        public PaletteChunk palette;

        public String getName() {
            if (chunk != null && chunk.memberInfo != null) {
                return chunk.memberInfo.name;
            }
            return "";
        }

        public MemberType getType() {
            return chunk != null ? chunk.memberType : MemberType.NULL;
        }
    }
}
