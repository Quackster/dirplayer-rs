package com.dirplayer.director.lingo.decompiler;

/**
 * Enums used by the Lingo decompiler.
 * Port of Rust decompiler enums.
 */
public class DecompilerEnums {

    /**
     * Tags for bytecode instructions used for loop identification.
     */
    public enum BytecodeTag {
        None,
        Skip,
        RepeatWhile,
        RepeatWithIn,
        RepeatWithTo,
        RepeatWithDownTo,
        NextRepeatTarget,
        EndCase
    }

    /**
     * Datum types used in the decompiler.
     * Note: This is separate from the runtime DatumType.
     */
    public enum DecompilerDatumType {
        Void,
        Symbol,
        VarRef,
        String,
        Int,
        Float,
        List,
        ArgList,
        ArgListNoRet,
        PropList
    }

    /**
     * Chunk expression types (char, word, item, line).
     */
    public enum ChunkExprType {
        Char(0x01, "char"),
        Word(0x02, "word"),
        Item(0x03, "item"),
        Line(0x04, "line");

        private final int value;
        private final String name;

        ChunkExprType(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public static ChunkExprType fromValue(int value) {
            for (ChunkExprType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return Char;
        }
    }

    /**
     * Put statement types.
     */
    public enum PutType {
        Into(0x01, "into"),
        After(0x02, "after"),
        Before(0x03, "before");

        private final int value;
        private final String name;

        PutType(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public static PutType fromValue(int value) {
            for (PutType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return Into;
        }
    }

    /**
     * Expected case statement continuations.
     */
    public enum CaseExpect {
        End,
        Or,
        Next,
        Otherwise
    }
}
