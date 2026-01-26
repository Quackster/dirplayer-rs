package com.dirplayer.director.chunks;

import com.dirplayer.director.lingo.OpCode;
import com.dirplayer.io.BinaryReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler definition containing bytecode and variable information.
 * Port of Rust HandlerDef struct.
 */
public class HandlerDef {
    public int nameId;
    public List<Bytecode> bytecodeArray;
    public Map<Integer, Integer> bytecodeIndexMap;
    public List<Integer> argumentNameIds;
    public List<Integer> localNameIds;
    public List<Integer> globalNameIds;

    public HandlerDef() {
        this.bytecodeArray = new ArrayList<>();
        this.bytecodeIndexMap = new HashMap<>();
        this.argumentNameIds = new ArrayList<>();
        this.localNameIds = new ArrayList<>();
        this.globalNameIds = new ArrayList<>();
    }

    /**
     * Handler record for parsing handler metadata.
     */
    public static class HandlerRecord {
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
            record.compiledLen = (int) reader.readU32();
            record.compiledOffset = (int) reader.readU32();
            record.argumentCount = reader.readU16();
            record.argumentOffset = (int) reader.readU32();
            record.localsCount = reader.readU16();
            record.localsOffset = (int) reader.readU32();
            record.globalsCount = reader.readU16();
            record.globalsOffset = (int) reader.readU32();
            record.unknown1 = (int) reader.readU32();
            record.unknown2 = reader.readU16();
            record.lineCount = reader.readU16();
            record.lineOffset = (int) reader.readU32();

            // Capital X format has additional stack height field
            if (capitalX) {
                reader.readU32(); // stack_height
            }

            return record;
        }
    }

    public static HandlerDef readData(BinaryReader reader, HandlerRecord record) {
        HandlerDef handler = new HandlerDef();
        handler.nameId = record.nameId;

        // Read bytecode
        reader.setPos(record.compiledOffset);

        while (reader.getPos() < record.compiledOffset + record.compiledLen) {
            int pos = reader.getPos() - record.compiledOffset;
            int op = reader.readU8();
            OpCode opcode = OpCode.from(op >= 0x40 ? 0x40 + op % 0x40 : op);

            long obj = 0;
            if (op >= 0xC0) {
                // Four bytes
                obj = reader.readI32();
            } else if (op >= 0x80) {
                // Two bytes
                if (opcode == OpCode.PushInt16 || opcode == OpCode.PushInt8) {
                    // Treat pushint's arg as signed
                    obj = reader.readI16();
                } else {
                    obj = reader.readU16();
                }
            } else if (op >= 0x40) {
                // One byte
                if (opcode == OpCode.PushInt8) {
                    // Treat pushint's arg as signed
                    obj = reader.readI8();
                } else {
                    obj = reader.readU8();
                }
            }

            Bytecode bytecode = new Bytecode(opcode, obj, pos);
            handler.bytecodeIndexMap.put(pos, handler.bytecodeArray.size());
            handler.bytecodeArray.add(bytecode);
        }

        // Read variable name tables
        handler.argumentNameIds = readVarnamesTable(reader, record.argumentCount, record.argumentOffset);
        handler.localNameIds = readVarnamesTable(reader, record.localsCount, record.localsOffset);
        handler.globalNameIds = readVarnamesTable(reader, record.globalsCount, record.globalsOffset);

        return handler;
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
