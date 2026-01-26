package com.dirplayer.director.lingo.decompiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Block node for containing statements.
 * Port of Rust BlockNode struct.
 */
public class BlockNode {
    private static final int MAX_WRITE_DEPTH = 100;

    public List<AstNode> children;
    public int endPos;
    public CaseLabelNode currentCaseLabel;

    public BlockNode() {
        this.children = new ArrayList<>();
        this.endPos = Integer.MAX_VALUE;
        this.currentCaseLabel = null;
    }

    /**
     * Add a child node to this block.
     */
    public void addChild(AstNode child) {
        children.add(child);
    }

    /**
     * Write the script representation of this block.
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
        for (AstNode child : children) {
            child.writeScriptWithDepth(code, dot, sum, depth + 1);
            code.endLine();
        }
    }
}
