package com.dirplayer.director;

import com.dirplayer.director.chunks.*;
import com.dirplayer.io.BinaryReader;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DirectorFile - parses and represents a Director movie file.
 * Port of Rust DirectorFile struct.
 */
public class DirectorFile {
    private static final Logger logger = LoggerFactory.getLogger(DirectorFile.class);

    public URL basePath;
    public String fileName;
    public int version;
    public List<CastListChunk.CastListEntry> castEntries;
    public List<CastDef> casts;
    public ConfigChunk config;
    public ScoreChunk score;
    public FrameLabelsChunk frameLabels;
    public SordChunk scoreOrder;
    public MediaChunk media;
    public XMediaChunk xmedia;
    public CastInfoChunk castInfo;
    public EffectChunk effect;
    public ThumChunk thum;
    public ChunkContainer chunkContainer;

    public DirectorFile() {
        this.castEntries = new ArrayList<>();
        this.casts = new ArrayList<>();
        this.chunkContainer = new ChunkContainer();
    }

    public static DirectorFile read(String fileName, URL basePath, BinaryReader reader) {
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        DirectorFile file = new DirectorFile();
        file.fileName = fileName;
        file.basePath = basePath;

        ChunkContainer chunkContainer = new ChunkContainer();
        file.chunkContainer = chunkContainer;

        int metaFourcc = reader.readU32();
        if (metaFourcc == Utils.FOURCC("XFIR")) {
            reader.setEndian(ByteOrder.LITTLE_ENDIAN);
        }

        int metaLength = reader.readU32();
        int codec = reader.readU32();
        boolean afterBurned = false;
        int ilsBodyOffset = 0;

        if (codec == Utils.FOURCC("MV93") || codec == Utils.FOURCC("MC95")) {
            throw new RuntimeException("read mmap not implemented");
        } else if (codec == Utils.FOURCC("FGDM") || codec == Utils.FOURCC("FGDC")) {
            afterBurned = true;
            ilsBodyOffset = readAfterBurnerMap(reader, chunkContainer.cachedChunkViews, chunkContainer.chunkInfo);
        } else {
            throw new RuntimeException("Invalid codec");
        }

        RIFXReaderContext rifx = new RIFXReaderContext();
        rifx.afterBurned = afterBurned;
        rifx.ilsBodyOffset = ilsBodyOffset;
        rifx.dirVersion = 0;
        rifx.lctxCapitalX = false;

        KeyTableChunk keyTable = readKeyTable(reader, chunkContainer, rifx);
        file.config = readConfig(reader, chunkContainer, rifx);

        rifx.dirVersion = Utils.humanVersion(file.config.directorVersion);

        // Read casts
        Object[] castResult = readCasts(reader, chunkContainer, rifx, keyTable, file.config);
        file.castEntries = (List<CastListChunk.CastListEntry>) castResult[0];
        file.casts = (List<CastDef>) castResult[1];

        // Read optional chunks
        file.score = getScoreChunk(reader, chunkContainer, rifx);
        file.frameLabels = getFrameLabelsChunk(reader, chunkContainer, rifx);
        file.scoreOrder = getScoreOrderChunk(reader, chunkContainer, rifx);
        file.media = getMediaChunk(reader, chunkContainer, rifx);
        file.xmedia = getXMediaChunk(reader, chunkContainer, rifx);
        file.castInfo = getCastInfoChunk(reader, chunkContainer, rifx);
        file.effect = getEffectChunk(reader, chunkContainer, rifx);
        file.thum = getThumChunk(reader, chunkContainer, rifx);

        file.version = rifx.dirVersion;

        return file;
    }

    public static DirectorFile readBytes(byte[] bytes, String fileName, String basePath) {
        BinaryReader reader = new BinaryReader(bytes);
        try {
            return read(fileName, new URL(basePath), reader);
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException("Invalid base path URL: " + basePath, e);
        }
    }

    // Read the Afterburner map (DCR/DXR files)
    private static int readAfterBurnerMap(BinaryReader reader,
                                           Map<Integer, byte[]> cachedChunkViews,
                                           Map<Integer, Chunk.ChunkInfo> chunkInfo) {
        // File version (Fver)
        if (reader.readU32() != Utils.FOURCC("Fver")) {
            throw new RuntimeException("readAfterburnerMap(): Fver expected but not found");
        }

        int fverLength = reader.readVarInt();
        int start = reader.getPos();
        int fverVersion = reader.readVarInt();

        if (fverVersion >= 0x401) {
            int imapVersion = reader.readVarInt();
            int directorVersion = reader.readVarInt();
        }

        if (fverVersion >= 0x501) {
            int versionStringLen = reader.readU8();
            reader.readBytes(versionStringLen);
        }

        int end = reader.getPos();
        if (end - start != fverLength) {
            reader.setPos(start + fverLength);
        }

        // Compression types (Fcdr)
        if (reader.readU32() != Utils.FOURCC("Fcdr")) {
            throw new RuntimeException("readAfterburnerMap(): Fcdr expected but not found");
        }

        int fcdrLength = reader.readVarInt();
        byte[] fcdrUncomp = reader.readZlibBytes(fcdrLength);

        BinaryReader fcdrReader = new BinaryReader(fcdrUncomp);
        fcdrReader.setEndian(reader.getEndian());

        int compressionTypeCount = fcdrReader.readU16();
        List<MoaID> compressionIds = new ArrayList<>();
        List<String> compressionDescs = new ArrayList<>();

        for (int i = 0; i < compressionTypeCount; i++) {
            byte[] guidBytes = fcdrReader.readBytes(16);
            compressionIds.add(MoaID.fromBytes(guidBytes));
        }

        for (int i = 0; i < compressionTypeCount; i++) {
            compressionDescs.add(fcdrReader.readCString());
        }

        // ABMP (Afterburner map)
        if (reader.readU32() != Utils.FOURCC("ABMP")) {
            throw new RuntimeException("readAfterburnerMap(): ABMP expected but not found");
        }

        int abmpLength = reader.readVarInt();
        int abmpEnd = reader.getPos() + abmpLength;
        int abmpCompressionType = reader.readVarInt();
        int abmpUncompLength = reader.readVarInt();

        byte[] abmpUncomp = reader.readZlibBytes(abmpEnd - reader.getPos());

        BinaryReader abmpReader = new BinaryReader(abmpUncomp);
        abmpReader.setEndian(reader.getEndian());

        int abmpUnk1 = abmpReader.readVarInt();
        int abmpUnk2 = abmpReader.readVarInt();
        int resCount = abmpReader.readVarInt();

        for (int i = 0; i < resCount; i++) {
            int resId = abmpReader.readVarInt();
            int offset = abmpReader.readVarInt();
            int compSize = abmpReader.readVarInt();
            int uncompSize = abmpReader.readVarInt();
            int compressionType = abmpReader.readVarInt();
            int tag = abmpReader.readU32();

            Chunk.ChunkInfo info = new Chunk.ChunkInfo();
            info.id = resId;
            info.fourcc = tag;
            info.len = compSize;
            info.uncompressedLen = uncompSize;
            info.offset = offset;
            info.compressionId = compressionIds.get(compressionType);

            chunkInfo.put(resId, info);
        }

        // Initial load segment (ILS)
        if (!chunkInfo.containsKey(2)) {
            throw new RuntimeException("readAfterburnerMap(): Map has no entry for ILS");
        }
        if (reader.readU32() != Utils.FOURCC("FGEI")) {
            throw new RuntimeException("readAfterburnerMap(): FGEI expected but not found");
        }

        Chunk.ChunkInfo ilsInfo = chunkInfo.get(2);
        int ilsUnk1 = reader.readVarInt();
        int ilsBodyOffset = reader.getPos();

        byte[] ilsUncomp = reader.readZlibBytes(ilsInfo.len);

        BinaryReader ilsReader = new BinaryReader(ilsUncomp);
        ilsReader.setEndian(reader.getEndian());

        while (!ilsReader.eof()) {
            int resId = ilsReader.readVarInt();
            Chunk.ChunkInfo info = chunkInfo.get(resId);
            if (info != null) {
                cachedChunkViews.put(resId, ilsReader.readBytes(info.len));
            }
        }

        return ilsBodyOffset;
    }

    private static KeyTableChunk readKeyTable(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk.ChunkInfo info = getFirstChunkInfo(chunkContainer.chunkInfo, Utils.FOURCC("KEY*"));
        if (info == null) {
            throw new RuntimeException("No key chunk!");
        }

        Chunk chunk = getChunk(reader, chunkContainer, rifx, info.fourcc, info.id);
        return chunk.asKeyTable();
    }

    private static ConfigChunk readConfig(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk.ChunkInfo info = getFirstChunkInfo(chunkContainer.chunkInfo, Utils.FOURCC("DRCF"));
        if (info == null) {
            info = getFirstChunkInfo(chunkContainer.chunkInfo, Utils.FOURCC("VWCF"));
        }
        if (info == null) {
            throw new RuntimeException("No config chunk!");
        }

        Chunk chunk = getChunk(reader, chunkContainer, rifx, info.fourcc, info.id);
        return chunk.asConfig();
    }

    private static Object[] readCasts(BinaryReader reader, ChunkContainer chunkContainer,
                                       RIFXReaderContext rifx, KeyTableChunk keyTable, ConfigChunk config) {
        List<CastListChunk.CastListEntry> castEntries = new ArrayList<>();
        List<CastDef> casts = new ArrayList<>();
        boolean internal = true;

        if (rifx.dirVersion >= 500) {
            CastListChunk castList = getCastListChunk(reader, chunkContainer, rifx);
            if (castList != null) {
                for (CastListChunk.CastListEntry castEntry : castList.entries) {
                    CastChunk cast = getCastChunkForCast(reader, chunkContainer, rifx, keyTable, castEntry.id);
                    if (cast != null) {
                        CastDef castDef = CastDef.from(
                            castEntry.name,
                            castEntry.id,
                            castEntry.minMember,
                            cast.memberIds,
                            reader,
                            chunkContainer,
                            rifx,
                            keyTable
                        );
                        casts.add(castDef);
                    }
                }
                return new Object[] { castList.entries, casts };
            } else {
                internal = false;
            }
        }

        Chunk cast = getFirstChunk(reader, chunkContainer, rifx, Utils.FOURCC("CAS*"));
        if (cast != null && cast.asCast() != null) {
            CastDef castDef = CastDef.from(
                internal ? "Internal" : "External",
                1024,
                config.minMember,
                cast.asCast().memberIds,
                reader,
                chunkContainer,
                rifx,
                keyTable
            );
            casts.add(castDef);
            return new Object[] { castEntries, casts };
        }

        logger.debug("No cast!");
        return new Object[] { castEntries, casts };
    }

    // Get chunk methods
    private static Chunk.ChunkInfo getFirstChunkInfo(Map<Integer, Chunk.ChunkInfo> chunkInfo, int fourcc) {
        for (Chunk.ChunkInfo info : chunkInfo.values()) {
            if (info.fourcc == fourcc) {
                return info;
            }
        }
        return null;
    }

    private static Chunk getFirstChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx, int fourcc) {
        Chunk.ChunkInfo info = getFirstChunkInfo(chunkContainer.chunkInfo, fourcc);
        if (info != null) {
            return getChunk(reader, chunkContainer, rifx, info.fourcc, info.id);
        }
        return null;
    }

    public static Chunk getChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx, int fourcc, int id) {
        byte[] chunkView = getChunkData(reader, chunkContainer, rifx, fourcc, id);
        if (chunkView != null) {
            return Chunk.makeChunk(reader.getEndian(), fourcc, chunkView, rifx.dirVersion, rifx.lctxCapitalX);
        }
        return null;
    }

    private static byte[] getChunkData(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx, int fourcc, int id) {
        Chunk.ChunkInfo info = chunkContainer.chunkInfo.get(id);
        if (info == null) {
            logger.warn("Could not find chunk {} id {}", Utils.fourccToString(fourcc), id);
            return null;
        }

        if (fourcc != info.fourcc) {
            logger.warn("Expected chunk {} to be '{}', but is actually '{}'",
                id, Utils.fourccToString(fourcc), Utils.fourccToString(info.fourcc));
            return null;
        }

        if (chunkContainer.cachedChunkViews.containsKey(id)) {
            return chunkContainer.cachedChunkViews.get(id);
        }

        if (rifx.afterBurned) {
            reader.setPos(info.offset + rifx.ilsBodyOffset);

            if (info.len == 0 && info.uncompressedLen == 0) {
                byte[] data = reader.readBytes(info.len);
                chunkContainer.cachedChunkViews.put(id, data);
            } else if (info.compressionId != null && GuidConstants.isCompressionImplemented(info.compressionId.data)) {
                byte[] uncompBuf = null;
                if (GuidConstants.isZlibCompression(info.compressionId.data)) {
                    uncompBuf = reader.readZlibBytes(info.len);
                } else if (GuidConstants.isSndCompression(info.compressionId.data)) {
                    byte[] sndBytes = reader.readBytes(info.len);
                    BinaryReader chunkReader = new BinaryReader(sndBytes);
                    chunkReader.setEndian(ByteOrder.BIG_ENDIAN);
                    try {
                        SoundChunk soundChunk = SoundChunk.fromSndChunk(chunkReader, 0);
                        byte[] wavBytes = soundChunk.toWav();
                        chunkContainer.cachedChunkViews.put(id, wavBytes);
                        return wavBytes;
                    } catch (Exception e) {
                        logger.warn("Failed to parse SND chunk {}: {}", id, e.getMessage());
                        chunkContainer.cachedChunkViews.put(id, sndBytes);
                        return sndBytes;
                    }
                }

                if (uncompBuf != null) {
                    chunkContainer.cachedChunkViews.put(id, uncompBuf);
                }
            } else if (info.compressionId != null && GuidConstants.isFontmapCompression(info.compressionId.data)) {
                logger.warn("FONTMAP compression not implemented — skipping chunk {}", id);
                byte[] raw = reader.readBytes(info.len);
                chunkContainer.cachedChunkViews.put(id, raw);
                return raw;
            } else {
                byte[] data = reader.readBytes(info.len);
                chunkContainer.cachedChunkViews.put(id, data);
            }
        } else {
            reader.setPos(info.offset);
            byte[] data = readChunkDataDirect(reader, fourcc, info.len);
            chunkContainer.cachedChunkViews.put(id, data);
        }

        return chunkContainer.cachedChunkViews.get(id);
    }

    private static byte[] readChunkDataDirect(BinaryReader reader, int fourcc, int len) {
        int validFourcc = reader.readU32();
        int validLen = reader.readU32();

        if (len == 0xFFFFFFFF) {
            len = validLen;
        }

        if (fourcc != validFourcc || len != validLen) {
            throw new RuntimeException("Chunk validation failed");
        }

        return reader.readBytes(len);
    }

    // Get specific chunk types
    private static CastChunk getCastChunkForCast(BinaryReader reader, ChunkContainer chunkContainer,
                                                  RIFXReaderContext rifx, KeyTableChunk keyTable, int castId) {
        KeyTableChunk.KeyTableEntry keyEntry = null;
        for (KeyTableChunk.KeyTableEntry entry : keyTable.entries) {
            if (entry.castId == castId && entry.fourcc == Utils.FOURCC("CAS*")) {
                keyEntry = entry;
                break;
            }
        }

        if (keyEntry != null) {
            Chunk chunk = getChunk(reader, chunkContainer, rifx, Utils.FOURCC("CAS*"), keyEntry.sectionId);
            return chunk != null ? chunk.asCast() : null;
        }
        return null;
    }

    private static CastListChunk getCastListChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk chunk = getFirstChunk(reader, chunkContainer, rifx, Utils.FOURCC("MCsL"));
        return chunk != null ? chunk.asCastList() : null;
    }

    private static ScoreChunk getScoreChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk chunk = getFirstChunk(reader, chunkContainer, rifx, Utils.FOURCC("VWSC"));
        return chunk != null ? chunk.asScore() : null;
    }

    private static FrameLabelsChunk getFrameLabelsChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk chunk = getFirstChunk(reader, chunkContainer, rifx, Utils.FOURCC("VWLB"));
        return chunk != null ? chunk.asFrameLabels() : null;
    }

    private static SordChunk getScoreOrderChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk chunk = getFirstChunk(reader, chunkContainer, rifx, Utils.FOURCC("Sord"));
        return chunk != null ? (SordChunk) getChunkData(chunk, Chunk.Type.SCORE_ORDER) : null;
    }

    private static MediaChunk getMediaChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk chunk = getFirstChunk(reader, chunkContainer, rifx, Utils.FOURCC("ediM"));
        return chunk != null ? chunk.asMedia() : null;
    }

    private static XMediaChunk getXMediaChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk chunk = getFirstChunk(reader, chunkContainer, rifx, Utils.FOURCC("XMED"));
        return chunk != null ? (XMediaChunk) getChunkData(chunk, Chunk.Type.XMEDIA) : null;
    }

    private static CastInfoChunk getCastInfoChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk chunk = getFirstChunk(reader, chunkContainer, rifx, Utils.FOURCC("Cinf"));
        return chunk != null ? (CastInfoChunk) getChunkData(chunk, Chunk.Type.CST_INFO) : null;
    }

    private static EffectChunk getEffectChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk chunk = getFirstChunk(reader, chunkContainer, rifx, Utils.FOURCC("FXmp"));
        return chunk != null ? (EffectChunk) getChunkData(chunk, Chunk.Type.EFFECT) : null;
    }

    private static ThumChunk getThumChunk(BinaryReader reader, ChunkContainer chunkContainer, RIFXReaderContext rifx) {
        Chunk chunk = getFirstChunk(reader, chunkContainer, rifx, Utils.FOURCC("Thum"));
        return chunk != null ? (ThumChunk) getChunkData(chunk, Chunk.Type.THUM) : null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getChunkData(Chunk chunk, Chunk.Type expectedType) {
        if (chunk == null || chunk.getType() != expectedType) {
            return null;
        }
        try {
            java.lang.reflect.Method method = Chunk.class.getMethod("as" + expectedType.name().charAt(0) + expectedType.name().substring(1).toLowerCase().replace("_", ""));
            return (T) method.invoke(chunk);
        } catch (Exception e) {
            return null;
        }
    }

    public static int getVariableMultiplier(boolean capitalX, int dirVersion) {
        if (capitalX) {
            return 1;
        }
        if (dirVersion >= 500) {
            return 8;
        }
        return 6;
    }

    /**
     * Chunk container - holds deserialized chunks.
     */
    public static class ChunkContainer {
        public Map<Integer, Chunk> deserializedChunks = new HashMap<>();
        public Map<Integer, Chunk.ChunkInfo> chunkInfo = new HashMap<>();
        public Map<Integer, byte[]> cachedChunkViews = new HashMap<>();
    }
}
