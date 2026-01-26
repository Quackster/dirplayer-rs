package com.dirplayer.director.lingo;

/**
 * String chunk expression for accessing parts of strings.
 * Port of Rust StringChunkExpr struct.
 */
public class StringChunkExpr {
    public StringChunkType chunkType;
    public int start;
    public int end;
    public char itemDelimiter;

    public StringChunkExpr() {
        this.chunkType = StringChunkType.CHAR;
        this.start = 0;
        this.end = 0;
        this.itemDelimiter = ',';
    }

    public StringChunkExpr(StringChunkType chunkType, int start, int end, char itemDelimiter) {
        this.chunkType = chunkType;
        this.start = start;
        this.end = end;
        this.itemDelimiter = itemDelimiter;
    }

    public StringChunkExpr copy() {
        return new StringChunkExpr(chunkType, start, end, itemDelimiter);
    }

    public StringChunkType getChunkType() {
        return chunkType;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public char getItemDelimiter() {
        return itemDelimiter;
    }
}
