package com.dirplayer.director;

import com.dirplayer.director.chunks.*;
import com.dirplayer.io.BinaryReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CastDef - represents a cast library with its members.
 * Port of Rust CastDef struct.
 */
public class CastDef {
    private static final Logger logger = LoggerFactory.getLogger(CastDef.class);

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

        // Load cast members
        for (int i = 0; i < memberIds.size(); i++) {
            int memberId = memberIds.get(i);
            if (memberId == 0) continue;

            int slotNumber = minMember + i;

            // Find the CASt chunk for this member
            KeyTableChunk.KeyTableEntry memberEntry = null;
            for (KeyTableChunk.KeyTableEntry entry : keyTable.entries) {
                if (entry.sectionId == memberId && entry.fourcc == Utils.FOURCC("CASt")) {
                    memberEntry = entry;
                    break;
                }
            }

            if (memberEntry != null) {
                Chunk chunk = DirectorFile.getChunk(reader, chunkContainer, rifx, Utils.FOURCC("CASt"), memberId);
                if (chunk != null && chunk.asCastMember() != null) {
                    CastMemberDef memberDef = new CastMemberDef();
                    memberDef.chunk = chunk.asCastMember();
                    memberDef.number = slotNumber;

                    // Load associated script if this is a script member
                    if (castDef.lctx != null) {
                        for (ScriptContextChunk.ScriptContextMapEntry mapEntry : castDef.lctx.sectionMap) {
                            if (mapEntry.sectionId == memberId) {
                                // This member has a script
                                Chunk scriptChunk = DirectorFile.getChunk(reader, chunkContainer, rifx,
                                    Utils.FOURCC("Lscr"), mapEntry.sectionId);
                                if (scriptChunk != null && scriptChunk.asScript() != null) {
                                    memberDef.script = scriptChunk.asScript();
                                }
                                break;
                            }
                        }
                    }

                    // Load media data based on member type
                    loadMemberMedia(memberDef, memberId, reader, chunkContainer, rifx, keyTable);

                    castDef.members.put(slotNumber, memberDef);
                }
            }
        }

        logger.debug("Cast {} loaded with {} members", name, castDef.members.size());
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
