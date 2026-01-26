package com.dirplayer.director.chunks;

import com.dirplayer.director.MoaID;
import com.dirplayer.director.Utils;
import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Chunk - wrapper for all Director chunk types.
 * Port of Rust Chunk enum and ChunkContainer.
 */
public class Chunk {
    public enum Type {
        CAST,
        CAST_LIST,
        CAST_MEMBER,
        CAST_INFO,
        CONFIG,
        INITIAL_MAP,
        KEY_TABLE,
        MEMORY_MAP,
        SCRIPT,
        SCRIPT_CONTEXT,
        SCRIPT_NAMES,
        FRAME_LABELS,
        SCORE,
        SCORE_ORDER,
        TEXT,
        BITMAP,
        PALETTE,
        SOUND,
        MEDIA,
        XMEDIA,
        CST_INFO,
        EFFECT,
        THUM,
        RAW
    }

    private Type type;
    private Object data;

    private Chunk(Type type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() { return type; }

    public static Chunk cast(CastChunk chunk) { return new Chunk(Type.CAST, chunk); }
    public static Chunk castList(CastListChunk chunk) { return new Chunk(Type.CAST_LIST, chunk); }
    public static Chunk castMember(CastMemberChunk chunk) { return new Chunk(Type.CAST_MEMBER, chunk); }
    public static Chunk config(ConfigChunk chunk) { return new Chunk(Type.CONFIG, chunk); }
    public static Chunk initialMap(InitialMapChunk chunk) { return new Chunk(Type.INITIAL_MAP, chunk); }
    public static Chunk keyTable(KeyTableChunk chunk) { return new Chunk(Type.KEY_TABLE, chunk); }
    public static Chunk script(ScriptChunk chunk) { return new Chunk(Type.SCRIPT, chunk); }
    public static Chunk scriptContext(ScriptContextChunk chunk) { return new Chunk(Type.SCRIPT_CONTEXT, chunk); }
    public static Chunk scriptNames(ScriptNamesChunk chunk) { return new Chunk(Type.SCRIPT_NAMES, chunk); }
    public static Chunk frameLabels(FrameLabelsChunk chunk) { return new Chunk(Type.FRAME_LABELS, chunk); }
    public static Chunk score(ScoreChunk chunk) { return new Chunk(Type.SCORE, chunk); }
    public static Chunk scoreOrder(SordChunk chunk) { return new Chunk(Type.SCORE_ORDER, chunk); }
    public static Chunk text(TextChunk chunk) { return new Chunk(Type.TEXT, chunk); }
    public static Chunk bitmap(BitmapChunk chunk) { return new Chunk(Type.BITMAP, chunk); }
    public static Chunk palette(PaletteChunk chunk) { return new Chunk(Type.PALETTE, chunk); }
    public static Chunk sound(SoundChunk chunk) { return new Chunk(Type.SOUND, chunk); }
    public static Chunk media(MediaChunk chunk) { return new Chunk(Type.MEDIA, chunk); }
    public static Chunk xmedia(XMediaChunk chunk) { return new Chunk(Type.XMEDIA, chunk); }
    public static Chunk cstInfo(CastInfoChunk chunk) { return new Chunk(Type.CST_INFO, chunk); }
    public static Chunk effect(EffectChunk chunk) { return new Chunk(Type.EFFECT, chunk); }
    public static Chunk thum(ThumChunk chunk) { return new Chunk(Type.THUM, chunk); }
    public static Chunk raw(byte[] data) { return new Chunk(Type.RAW, data); }

    public TextChunk asText() { return type == Type.TEXT ? (TextChunk) data : null; }
    public BitmapChunk asBitmap() { return type == Type.BITMAP ? (BitmapChunk) data : null; }
    public PaletteChunk asPalette() { return type == Type.PALETTE ? (PaletteChunk) data : null; }
    public ScoreChunk asScore() { return type == Type.SCORE ? (ScoreChunk) data : null; }
    public SoundChunk asSound() { return type == Type.SOUND ? (SoundChunk) data : null; }
    public ScriptChunk asScript() { return type == Type.SCRIPT ? (ScriptChunk) data : null; }
    public ConfigChunk asConfig() { return type == Type.CONFIG ? (ConfigChunk) data : null; }
    public CastChunk asCast() { return type == Type.CAST ? (CastChunk) data : null; }
    public CastMemberChunk asCastMember() { return type == Type.CAST_MEMBER ? (CastMemberChunk) data : null; }
    public CastListChunk asCastList() { return type == Type.CAST_LIST ? (CastListChunk) data : null; }
    public KeyTableChunk asKeyTable() { return type == Type.KEY_TABLE ? (KeyTableChunk) data : null; }
    public ScriptContextChunk asScriptContext() { return type == Type.SCRIPT_CONTEXT ? (ScriptContextChunk) data : null; }
    public ScriptNamesChunk asScriptNames() { return type == Type.SCRIPT_NAMES ? (ScriptNamesChunk) data : null; }
    public FrameLabelsChunk asFrameLabels() { return type == Type.FRAME_LABELS ? (FrameLabelsChunk) data : null; }
    public MediaChunk asMedia() { return type == Type.MEDIA ? (MediaChunk) data : null; }
    public byte[] asBytes() { return type == Type.RAW ? (byte[]) data : null; }

    /**
     * Create a chunk from raw data and fourcc.
     */
    public static Chunk makeChunk(ByteOrder endian, int fourcc, byte[] view, int version, boolean lctxCapitalX) {
        BinaryReader chunkReader = new BinaryReader(view);
        chunkReader.setEndian(endian);

        String fourccStr = Utils.fourccToString(fourcc);

        switch (fourccStr) {
            case "imap":
                return initialMap(InitialMapChunk.fromReader(chunkReader, version));
            case "CAS*":
                return cast(CastChunk.fromReader(chunkReader, version));
            case "CASt":
                return castMember(CastMemberChunk.fromReader(chunkReader, version));
            case "KEY*":
                return keyTable(KeyTableChunk.fromReader(chunkReader, version));
            case "LctX":
            case "Lctx":
                return scriptContext(ScriptContextChunk.fromReader(chunkReader, version));
            case "Lnam":
                return scriptNames(ScriptNamesChunk.fromReader(chunkReader, version));
            case "Lscr":
                return script(ScriptChunk.fromReader(chunkReader, version, lctxCapitalX));
            case "DRCF":
            case "VWCF":
                return config(ConfigChunk.fromReader(chunkReader, version, endian));
            case "MCsL":
                return castList(CastListChunk.fromReader(chunkReader, version, endian));
            case "VWSC":
            case "SCVW":
                return score(ScoreChunk.read(chunkReader, version));
            case "VWLB":
                return frameLabels(FrameLabelsChunk.fromReader(chunkReader, version));
            case "ediM":
                return media(MediaChunk.fromReader(chunkReader));
            case "Sord":
                return scoreOrder(SordChunk.fromReader(chunkReader));
            case "snd ":
                return sound(SoundChunk.fromSndChunk(chunkReader, version));
            case "STXT":
                return text(TextChunk.read(chunkReader));
            case "BITD":
                return bitmap(BitmapChunk.read(chunkReader, version));
            case "XMED":
                return xmedia(XMediaChunk.fromReader(chunkReader));
            case "Cinf":
                return cstInfo(CastInfoChunk.fromReader(chunkReader));
            case "FXmp":
                return effect(EffectChunk.fromReader(chunkReader));
            case "Thum":
                return thum(ThumChunk.fromReader(chunkReader));
            case "CLUT":
                return palette(PaletteChunk.fromReader(chunkReader, version));
            default:
                throw new RuntimeException("Could not deserialize '" + fourccStr + "' chunk");
        }
    }

    /**
     * Chunk info - metadata about a chunk.
     */
    public static class ChunkInfo {
        public int id;
        public int fourcc;
        public int len;
        public int uncompressedLen;
        public int offset;
        public MoaID compressionId;
    }

    /**
     * Chunk container - holds deserialized chunks.
     */
    public static class ChunkContainer {
        public Map<Integer, Chunk> deserializedChunks = new HashMap<>();
        public Map<Integer, ChunkInfo> chunkInfo = new HashMap<>();
        public Map<Integer, byte[]> cachedChunkViews = new HashMap<>();
    }
}
