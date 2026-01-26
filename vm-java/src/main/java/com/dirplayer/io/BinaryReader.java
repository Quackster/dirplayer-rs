package com.dirplayer.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.InflaterInputStream;

/**
 * Binary reader for Director file formats with support for both big and little endian.
 * Port of the Rust BinaryReader with DirectorExt trait implementations.
 */
public class BinaryReader {
    private final byte[] data;
    private int pos;
    private ByteOrder endian;

    public BinaryReader(byte[] data) {
        this.data = data;
        this.pos = 0;
        this.endian = ByteOrder.BIG_ENDIAN;
    }

    public static BinaryReader fromBytes(byte[] data) {
        return new BinaryReader(data);
    }

    public void setEndian(ByteOrder endian) {
        this.endian = endian;
    }

    public ByteOrder getEndian() {
        return endian;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getLength() {
        return data.length;
    }

    public int bytesLeft() {
        return Math.max(0, data.length - pos);
    }

    public boolean eof() {
        return pos >= data.length;
    }

    public void skip(int count) {
        pos += count;
    }

    // Basic read methods
    public byte readI8() {
        return data[pos++];
    }

    public int readU8() {
        return data[pos++] & 0xFF;
    }

    public short readI16() {
        byte[] bytes = readBytes(2);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(endian);
        return buffer.getShort();
    }

    public int readU16() {
        byte[] bytes = readBytes(2);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(endian);
        return buffer.getShort() & 0xFFFF;
    }

    public int readI32() {
        byte[] bytes = readBytes(4);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(endian);
        return buffer.getInt();
    }

    public long readU32() {
        byte[] bytes = readBytes(4);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(endian);
        return buffer.getInt() & 0xFFFFFFFFL;
    }

    /**
     * Read an unsigned 32-bit integer as int (truncating high bit if set).
     * Safe for Director files which don't use full u32 range.
     */
    public int readU32AsInt() {
        return (int) readU32();
    }

    public long readI64() {
        byte[] bytes = readBytes(8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(endian);
        return buffer.getLong();
    }

    public float readF32() {
        byte[] bytes = readBytes(4);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(endian);
        return buffer.getFloat();
    }

    public double readF64() {
        byte[] bytes = readBytes(8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(endian);
        return buffer.getDouble();
    }

    public byte[] readBytes(int length) {
        byte[] result = new byte[length];
        System.arraycopy(data, pos, result, 0, length);
        pos += length;
        return result;
    }

    public byte[] readAllBytes() {
        return readBytes(bytesLeft());
    }

    // Director-specific extensions

    /**
     * Read a variable-length integer (7-bit encoding with continuation bit).
     */
    public int readVarInt() {
        int val = 0;
        int b;
        do {
            b = readU8();
            val = (val << 7) | (b & 0x7F);
        } while ((b >> 7) != 0);
        return val;
    }

    /**
     * Read and decompress zlib-compressed bytes.
     */
    public byte[] readZlibBytes(int length) throws IOException {
        byte[] compressed = readBytes(length);
        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        InflaterInputStream iis = new InflaterInputStream(bais);
        return iis.readAllBytes();
    }

    /**
     * Read a Pascal string (length-prefixed with single byte).
     */
    public String readPascalString() {
        int len = readU8();
        return readString(len);
    }

    /**
     * Read a string of specified length.
     */
    public String readString(int len) {
        byte[] bytes = readBytes(len);
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    /**
     * Read a null-terminated string.
     */
    public String readNullTerminatedString() {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = readU8()) != 0) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    /**
     * Alias for readNullTerminatedString (C-style string).
     */
    public String readCString() {
        return readNullTerminatedString();
    }

    /**
     * Read an 80-bit Apple Extended precision float (SANE format).
     * Converts to double precision.
     */
    public double readAppleFloat80() {
        byte[] data = readBytes(10);

        // Extract exponent (first 2 bytes, big endian)
        int exponent = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        long f64sign = ((long)(exponent & 0x8000)) << 48;
        exponent = exponent & 0x7FFF;

        // Extract 64-bit fraction (remaining 8 bytes, big endian)
        long fraction = 0;
        for (int i = 2; i < 10; i++) {
            fraction = (fraction << 8) | (data[i] & 0xFF);
        }
        fraction &= 0x7FFFFFFFFFFFFFFFL;

        long f64exp;
        if (exponent == 0) {
            f64exp = 0;
        } else if (exponent == 0x7FFF) {
            f64exp = 0x7FF;
        } else {
            long normexp = exponent - 0x3FFF;
            if (normexp < -0x3FE || normexp >= 0x3FF) {
                throw new RuntimeException("Constant float exponent too big for a double");
            }
            f64exp = normexp + 0x3FF;
        }

        f64exp = f64exp << 52;
        long f64fract = fraction >> 11;
        long f64bin = f64sign | f64exp | f64fract;

        return Double.longBitsToDouble(f64bin);
    }

    /**
     * Read a FourCC (4-character code).
     */
    public String readFourCC() {
        byte[] bytes = readBytes(4);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    /**
     * Peek at bytes without advancing position.
     */
    public byte[] peekBytes(int length) {
        byte[] result = new byte[length];
        System.arraycopy(data, pos, result, 0, length);
        return result;
    }

    /**
     * Get underlying data array.
     */
    public byte[] getData() {
        return data;
    }
}
