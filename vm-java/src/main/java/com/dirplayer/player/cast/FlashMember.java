package com.dirplayer.player.cast;

/**
 * Flash/SWF cast member data.
 * Port of Rust FlashMember struct.
 */
public class FlashMember {
    public byte[] data;

    public FlashMember() {
        this.data = new byte[0];
    }

    public FlashMember(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public int getDataSize() {
        return data != null ? data.length : 0;
    }

    public FlashMember copy() {
        return new FlashMember(data != null ? data.clone() : null);
    }
}
