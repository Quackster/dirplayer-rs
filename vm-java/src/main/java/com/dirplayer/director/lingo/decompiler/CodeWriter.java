package com.dirplayer.director.lingo.decompiler;

/**
 * Code writer for generating Lingo source text.
 * Port of Rust CodeWriter struct.
 */
public class CodeWriter {
    private StringBuilder output;
    private int indentLevel;
    private boolean atLineStart;

    public CodeWriter() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.atLineStart = true;
    }

    /**
     * Increase indentation level.
     */
    public void indent() {
        indentLevel++;
    }

    /**
     * Decrease indentation level.
     */
    public void unindent() {
        if (indentLevel > 0) {
            indentLevel--;
        }
    }

    /**
     * Write text to the output, adding indentation if at line start.
     */
    public void write(String text) {
        if (atLineStart && !text.isEmpty()) {
            for (int i = 0; i < indentLevel; i++) {
                output.append("  ");
            }
            atLineStart = false;
        }
        output.append(text);
    }

    /**
     * Write text followed by a newline.
     */
    public void writeln(String text) {
        write(text);
        endLine();
    }

    /**
     * End the current line with a newline character.
     */
    public void endLine() {
        output.append('\n');
        atLineStart = true;
    }

    /**
     * Get the resulting string.
     */
    public String toStringResult() {
        return output.toString();
    }

    /**
     * Get the current indentation level.
     */
    public int currentIndent() {
        return indentLevel;
    }
}
