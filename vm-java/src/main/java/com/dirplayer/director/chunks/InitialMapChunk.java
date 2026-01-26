package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;

/**
 * Initial map chunk (imap) - contains directory information.
 * Port of Rust InitialMapChunk struct.
 */
public class InitialMapChunk {
    public int version;
    public int mmapOffset;
    public int directorVersion;
    public int unused1;
    public int unused2;
    public int unused3;

    public InitialMapChunk() {
    }

    public static InitialMapChunk fromReader(BinaryReader reader, int dirVersion) {
        // TODO: Implement proper parsing
        throw new UnsupportedOperationException("InitialMapChunk parsing not yet implemented");
    }
}
