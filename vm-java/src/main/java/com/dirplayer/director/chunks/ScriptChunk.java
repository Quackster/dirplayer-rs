package com.dirplayer.director.chunks;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.StaticDatum;
import com.dirplayer.io.BinaryReader;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Script chunk containing Lingo script data.
 * Port of Rust ScriptChunk struct.
 */
public class ScriptChunk {
    public List<Datum> literals;
    public List<HandlerDef> handlers;
    public List<Integer> propertyNameIds;
    public Map<Integer, StaticDatum> propertyDefaults;

    public ScriptChunk() {
        this.literals = new ArrayList<>();
        this.handlers = new ArrayList<>();
        this.propertyNameIds = new ArrayList<>();
        this.propertyDefaults = new HashMap<>();
    }

    public static ScriptChunk fromReader(BinaryReader reader, int dirVersion, boolean capitalX) {
        // Lingo scripts are always big endian regardless of file endianness
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        reader.setPos(8);

        int totalLength = (int) reader.readU32();        // 8
        int totalLength2 = (int) reader.readU32();       // 12
        int headerLength = reader.readU16();             // 16
        int scriptNumber = reader.readU16();             // 18
        int unk20 = reader.readU16();                    // 20
        int parentNumber = reader.readU16();             // 22

        reader.setPos(38);
        int scriptFlags = (int) reader.readU32();        // 38
        int unk42 = reader.readU16();                    // 42
        int castId = (int) reader.readU32();             // 44
        int factoryNameId = reader.readU16();            // 48
        int handlerVectorsCount = reader.readU16();      // 50
        int handlerVectorsOffset = (int) reader.readU32();  // 52
        int handlerVectorsSize = (int) reader.readU32();    // 56
        int propertiesCount = reader.readU16();          // 60
        int propertiesOffset = (int) reader.readU32();   // 62
        int globalsCount = reader.readU16();             // 66
        int globalsOffset = (int) reader.readU32();      // 68
        int handlersCount = reader.readU16();            // 72
        int handlersOffset = (int) reader.readU32();     // 74
        int literalsCount = reader.readU16();            // 78
        int literalsOffset = (int) reader.readU32();     // 80
        int literalsDataCount = (int) reader.readU32();  // 84
        int literalsDataOffset = (int) reader.readU32(); // 88

        // Read property and global name tables
        List<Integer> propertyNameIds = readVarnamesTable(reader, propertiesCount, propertiesOffset);
        List<Integer> globalNameIds = readVarnamesTable(reader, globalsCount, globalsOffset);

        // Read handler records
        reader.setPos(handlersOffset);
        List<HandlerDef.HandlerRecord> handlerRecords = new ArrayList<>();
        for (int i = 0; i < handlersCount; i++) {
            handlerRecords.add(HandlerDef.HandlerRecord.readRecord(reader, dirVersion, capitalX));
        }

        // Read handler data
        List<HandlerDef> handlers = new ArrayList<>();
        for (HandlerDef.HandlerRecord record : handlerRecords) {
            handlers.add(HandlerDef.readData(reader, record));
        }

        // Read literal records
        reader.setPos(literalsOffset);
        List<LiteralStore.LiteralStoreRecord> literalRecords = new ArrayList<>();
        for (int i = 0; i < literalsCount; i++) {
            literalRecords.add(LiteralStore.readRecord(reader, dirVersion));
        }

        // Read literal data
        List<Datum> literals = new ArrayList<>();
        for (LiteralStore.LiteralStoreRecord record : literalRecords) {
            literals.add(LiteralStore.readData(reader, record, literalsDataOffset));
        }

        // Map property IDs to default values
        Map<Integer, StaticDatum> propertyDefaults = new HashMap<>();
        for (int i = 0; i < propertyNameIds.size(); i++) {
            int propId = propertyNameIds.get(i);
            if (i < literals.size()) {
                Datum literal = literals.get(i);
                if (!propertyDefaults.containsKey(propId)) {
                    propertyDefaults.put(propId, StaticDatum.from(literal));
                }
            }
        }

        ScriptChunk chunk = new ScriptChunk();
        chunk.literals = literals;
        chunk.handlers = handlers;
        chunk.propertyNameIds = propertyNameIds;
        chunk.propertyDefaults = propertyDefaults;

        return chunk;
    }

    private static List<Integer> readVarnamesTable(BinaryReader reader, int count, int offset) {
        reader.setPos(offset);
        List<Integer> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(reader.readU16());
        }
        return result;
    }
}
