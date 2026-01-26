package com.dirplayer.director;

/**
 * RIFX reader context - tracks state during Director file parsing.
 * Port of Rust RIFXReaderContext struct.
 */
public class RIFXReaderContext {
    public boolean afterBurned;
    public int ilsBodyOffset;
    public int dirVersion;
    public boolean lctxCapitalX;

    public RIFXReaderContext() {
        this.afterBurned = false;
        this.ilsBodyOffset = 0;
        this.dirVersion = 0;
        this.lctxCapitalX = false;
    }
}
