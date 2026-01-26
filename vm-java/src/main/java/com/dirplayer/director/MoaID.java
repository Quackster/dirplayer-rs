package com.dirplayer.director;

import java.util.Arrays;

/**
 * MoaID - GUID for MOA (Macromedia Open Architecture) compression types.
 * Port of Rust MoaID struct.
 */
public class MoaID {
    public byte[] data;

    public MoaID() {
        this.data = new byte[16];
    }

    public MoaID(byte[] data) {
        this.data = data != null ? data : new byte[16];
    }

    public static MoaID fromBytes(byte[] bytes) {
        return new MoaID(bytes);
    }

    public boolean isZero() {
        for (byte b : data) {
            if (b != 0) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MoaID)) return false;
        MoaID other = (MoaID) obj;
        return Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
