package com.dirplayer.io;

import java.nio.ByteOrder;
import java.util.List;

/**
 * Utility functions for reading data from list of byte buffers.
 * Port of Rust list_readers.rs
 */
public class ListReaders {

    public static String readPascalString(List<byte[]> itemBufs, int index, ByteOrder endian) {
        if (index >= itemBufs.size()) {
            return "";
        }

        byte[] buf = itemBufs.get(index);
        if (buf.length == 0) {
            return "";
        }

        BinaryReader reader = new BinaryReader(buf);
        reader.setEndian(endian);
        return reader.readPascalString();
    }

    public static String readString(List<byte[]> itemBufs, int index) {
        if (index >= itemBufs.size()) {
            return "";
        }

        byte[] buf = itemBufs.get(index);
        return new String(buf, java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    public static int readU16(List<byte[]> itemBufs, int index, ByteOrder endian) {
        if (index >= itemBufs.size()) {
            return 0;
        }

        byte[] buf = itemBufs.get(index);
        BinaryReader reader = new BinaryReader(buf);
        reader.setEndian(endian);
        return reader.readU16();
    }

    public static int readI16(List<byte[]> itemBufs, int index, ByteOrder endian) {
        if (index >= itemBufs.size()) {
            return 0;
        }

        byte[] buf = itemBufs.get(index);
        BinaryReader reader = new BinaryReader(buf);
        reader.setEndian(endian);
        return reader.readI16();
    }

    public static int readI32(List<byte[]> itemBufs, int index, ByteOrder endian) {
        if (index >= itemBufs.size()) {
            return 0;
        }

        byte[] buf = itemBufs.get(index);
        BinaryReader reader = new BinaryReader(buf);
        reader.setEndian(endian);
        return reader.readI32();
    }

    public static long readU32(List<byte[]> itemBufs, int index, ByteOrder endian) {
        if (index >= itemBufs.size()) {
            return 0;
        }

        byte[] buf = itemBufs.get(index);
        BinaryReader reader = new BinaryReader(buf);
        reader.setEndian(endian);
        return reader.readU32();
    }

    public static byte[] readBytes(List<byte[]> itemBufs, int index) {
        if (index >= itemBufs.size()) {
            return new byte[0];
        }
        return itemBufs.get(index);
    }
}
