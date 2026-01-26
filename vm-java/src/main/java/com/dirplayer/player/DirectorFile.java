package com.dirplayer.player;

import com.dirplayer.director.chunks.ScriptChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a loaded Director file (DCR, DIR, DXR).
 * Port of Rust DirectorFile struct.
 */
public class DirectorFile {
    public String basePath;
    public String fileName;
    public int version;
    public List<CastListEntry> castEntries;
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
        this.basePath = "";
        this.fileName = "";
        this.version = 0;
        this.castEntries = new ArrayList<>();
        this.casts = new ArrayList<>();
        this.chunkContainer = new ChunkContainer();
    }

    // Placeholder chunk types - these would be fully implemented
    public static class CastListEntry {
        public int id;
        public String name;
        public String path;
        public int minMember;
        public int maxMember;

        public CastListEntry() {
            this.id = 0;
            this.name = "";
            this.path = "";
            this.minMember = 0;
            this.maxMember = 0;
        }
    }

    public static class CastDef {
        public int id;
        public String name;
        public Map<Integer, CastMemberDef> members;
        public ScriptContextData scriptContext;

        public CastDef() {
            this.id = 0;
            this.name = "";
            this.members = new HashMap<>();
        }
    }

    public static class CastMemberDef {
        public int number;
        public CastMemberChunkData chunk;
        public byte[] mediaData;

        public CastMemberDef() {
            this.number = 0;
        }
    }

    public static class CastMemberChunkData {
        public int memberType;
        public MemberInfo memberInfo;
        public byte[] specificData;

        public CastMemberChunkData() {
            this.memberType = 0;
        }
    }

    public static class MemberInfo {
        public String name;
        public int scriptId;

        public MemberInfo() {
            this.name = "";
            this.scriptId = 0;
        }
    }

    public static class ScriptContextData {
        public List<String> names;
        public Map<Integer, ScriptChunk> scripts;

        public ScriptContextData() {
            this.names = new ArrayList<>();
            this.scripts = new HashMap<>();
        }
    }

    public static class ConfigChunk {
        public int directorVersion;
        public int movieLeft;
        public int movieTop;
        public int movieRight;
        public int movieBottom;
        public int defaultPalette;
        public int stageColor;
        public int tempo;

        public ConfigChunk() {
            this.directorVersion = 0;
            this.movieLeft = 0;
            this.movieTop = 0;
            this.movieRight = 640;
            this.movieBottom = 480;
            this.defaultPalette = 0;
            this.stageColor = 255;
            this.tempo = 30;
        }
    }

    public static class ScoreChunk {
        public List<FrameData> frames;
        public int channelCount;

        public ScoreChunk() {
            this.frames = new ArrayList<>();
            this.channelCount = 0;
        }
    }

    public static class FrameData {
        public int frameNum;
        public List<SpriteData> sprites;
        public int tempo;
        public int transitionId;
        public int scriptId;

        public FrameData() {
            this.sprites = new ArrayList<>();
            this.tempo = 30;
        }
    }

    public static class SpriteData {
        public int channel;
        public int memberRef;
        public int locH;
        public int locV;
        public int width;
        public int height;
        public int ink;
        public int blend;
        public boolean visible;

        public SpriteData() {
            this.channel = 0;
            this.visible = true;
            this.blend = 100;
        }
    }

    public static class FrameLabelsChunk {
        public Map<Integer, String> labels;

        public FrameLabelsChunk() {
            this.labels = new HashMap<>();
        }
    }

    public static class SordChunk {
        public List<Integer> channelOrder;

        public SordChunk() {
            this.channelOrder = new ArrayList<>();
        }
    }

    public static class MediaChunk {
        public Map<Integer, byte[]> mediaData;

        public MediaChunk() {
            this.mediaData = new HashMap<>();
        }
    }

    public static class XMediaChunk {
        public Map<Integer, byte[]> xmediaData;

        public XMediaChunk() {
            this.xmediaData = new HashMap<>();
        }
    }

    public static class CastInfoChunk {
        public List<CastInfoEntry> entries;

        public CastInfoChunk() {
            this.entries = new ArrayList<>();
        }
    }

    public static class CastInfoEntry {
        public int id;
        public String name;

        public CastInfoEntry() {
            this.id = 0;
            this.name = "";
        }
    }

    public static class EffectChunk {
        public Map<Integer, EffectData> effects;

        public EffectChunk() {
            this.effects = new HashMap<>();
        }
    }

    public static class EffectData {
        public int id;
        public String name;

        public EffectData() {
            this.id = 0;
            this.name = "";
        }
    }

    public static class ThumChunk {
        public Map<Integer, byte[]> thumbnails;

        public ThumChunk() {
            this.thumbnails = new HashMap<>();
        }
    }

    public static class ChunkContainer {
        public Map<Integer, Object> deserializedChunks;
        public Map<Integer, ChunkInfo> chunkInfo;
        public Map<Integer, byte[]> cachedChunkViews;

        public ChunkContainer() {
            this.deserializedChunks = new HashMap<>();
            this.chunkInfo = new HashMap<>();
            this.cachedChunkViews = new HashMap<>();
        }
    }

    public static class ChunkInfo {
        public int id;
        public int fourcc;
        public int len;
        public int uncompressedLen;
        public int offset;

        public ChunkInfo() {
            this.id = 0;
            this.fourcc = 0;
            this.len = 0;
            this.uncompressedLen = 0;
            this.offset = 0;
        }
    }
}
