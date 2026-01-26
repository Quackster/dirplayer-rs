package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler record - contains handler metadata and bytecode.
 * Port of Rust HandlerRecord struct.
 */
public class HandlerRecord {
    public int nameId;
    public int vectorPos;
    public int compiledLen;
    public int compiledOffset;
    public int argumentCount;
    public int argumentOffset;
    public int localsCount;
    public int localsOffset;
    public int globalsCount;
    public int globalsOffset;
    public int unknown1;
    public int unknown2;
    public int lineCount;
    public int lineOffset;

    public static HandlerRecord readRecord(BinaryReader reader, int dirVersion, boolean capitalX) {
        HandlerRecord record = new HandlerRecord();
        record.nameId = reader.readU16();
        record.vectorPos = reader.readU16();
        record.compiledLen = reader.readU32();
        record.compiledOffset = reader.readU32();
        record.argumentCount = reader.readU16();
        record.argumentOffset = reader.readU32();
        record.localsCount = reader.readU16();
        record.localsOffset = reader.readU32();
        record.globalsCount = reader.readU16();
        record.globalsOffset = reader.readU32();
        record.unknown1 = reader.readU32();
        record.unknown2 = reader.readU16();
        record.lineCount = reader.readU16();
        record.lineOffset = reader.readU32();

        if (capitalX) {
            int stackHeight = reader.readU32();
        }

        return record;
    }

    public static HandlerDef readData(BinaryReader reader, HandlerRecord record) {
        List<Bytecode> bytecodeArray = new ArrayList<>();
        Map<Integer, Integer> bytecodeIndexMap = new HashMap<>();

        reader.setPos(record.compiledOffset);

        while (reader.getPos() < record.compiledOffset + record.compiledLen) {
            int pos = reader.getPos() - record.compiledOffset;
            int op = reader.readU8();
            int opcode = op >= 0x40 ? 0x40 + op % 0x40 : op;

            long obj = 0;
            if (op >= 0xC0) {
                // Four bytes
                obj = reader.readI32();
            } else if (op >= 0x80) {
                // Two bytes
                if (opcode == 0x41 || opcode == 0x42) { // PushInt16, PushInt8
                    obj = reader.readI16();
                } else {
                    obj = reader.readU16();
                }
            } else if (op >= 0x40) {
                // One byte
                if (opcode == 0x42) { // PushInt8
                    obj = reader.readI8();
                } else {
                    obj = reader.readU8();
                }
            }

            Bytecode bytecode = new Bytecode();
            bytecode.opcode = opcode;
            bytecode.obj = obj;
            bytecode.pos = pos;

            bytecodeArray.add(bytecode);
            bytecodeIndexMap.put(pos, bytecodeArray.size() - 1);
        }

        List<Integer> argumentNameIds = readVarnamesTable(reader, record.argumentCount, record.argumentOffset);
        List<Integer> localNameIds = readVarnamesTable(reader, record.localsCount, record.localsOffset);
        List<Integer> globalNameIds = readVarnamesTable(reader, record.globalsCount, record.globalsOffset);

        HandlerDef def = new HandlerDef();
        def.nameId = record.nameId;
        def.bytecodeArray = bytecodeArray;
        def.bytecodeIndexMap = bytecodeIndexMap;
        def.argumentNameIds = argumentNameIds;
        def.localNameIds = localNameIds;
        def.globalNameIds = globalNameIds;

        return def;
    }

    private static List<Integer> readVarnamesTable(BinaryReader reader, int count, int offset) {
        reader.setPos(offset);
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(reader.readU16());
        }
        return result;
    }
}
