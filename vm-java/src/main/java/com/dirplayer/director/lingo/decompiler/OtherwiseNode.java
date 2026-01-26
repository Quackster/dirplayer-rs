package com.dirplayer.director.lingo.decompiler;

/**
 * Otherwise node for case statements.
 * Port of Rust OtherwiseNode struct.
 */
public class OtherwiseNode {
    private static final int MAX_WRITE_DEPTH = 100;

    public BlockNode block;

    public OtherwiseNode() {
        this.block = new BlockNode();
    }

    /**
     * Write the script representation of this otherwise clause.
     */
    public void writeScript(CodeWriter code, boolean dot, boolean sum) {
        writeScriptWithDepth(code, dot, sum, 0);
    }

    void writeScriptWithDepth(CodeWriter code, boolean dot, boolean sum, int depth) {
        if (depth > MAX_WRITE_DEPTH) {
            code.write("-- MAX DEPTH EXCEEDED");
            code.endLine();
            return;
        }

        code.write("otherwise:");
        code.endLine();
        code.indent();
        block.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.unindent();
    }
}
