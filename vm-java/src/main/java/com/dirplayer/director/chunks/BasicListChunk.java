package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic list chunk - helper for reading list-based chunks.
 * Port of Rust BasicListChunk struct.
 */
public class BasicListChunk {

    public static int readHeader(BinaryReader reader, int dirVersion) {
        return (int) reader.readU32();
    }

    public static List<Integer> readOffsetTable(BinaryReader reader, int dirVersion, int dataOffset) {
        reader.setPos(dataOffset);
        int offsetTableLen = reader.readU16();

        List<Integer> offsetTable = new ArrayList<>();
        for (int i = 0; i < offsetTableLen; i++) {
            offsetTable.add((int) reader.readU32());
        }
        return offsetTable;
    }

    public static List<byte[]> readItems(BinaryReader reader, int dirVersion, int dataOffset, List<Integer> offsetTable) {
        int itemsLen = (int) reader.readU32();
        int listOffset = reader.getPos();

        List<byte[]> items = new ArrayList<>();
        for (int i = 0; i < offsetTable.size(); i++) {
            int offset = offsetTable.get(i);
            int nextOffset = (i == offsetTable.size() - 1) ? itemsLen : offsetTable.get(i + 1);

            reader.setPos(listOffset + offset);
            int length = nextOffset - offset;
            if (length > 0) {
                items.add(reader.readBytes(length));
            } else {
                items.add(new byte[0]);
            }
        }
        return items;
    }

    public static List<byte[]> fromReader(BinaryReader reader, int dirVersion) {
        int header = readHeader(reader, dirVersion);
        List<Integer> offsetTable = readOffsetTable(reader, dirVersion, header);
        return readItems(reader, dirVersion, header, offsetTable);
    }
}
