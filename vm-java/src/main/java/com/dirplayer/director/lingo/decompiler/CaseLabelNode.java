package com.dirplayer.director.lingo.decompiler;

import com.dirplayer.director.lingo.decompiler.DecompilerEnums.CaseExpect;

/**
 * Case label node for case statements.
 * Port of Rust CaseLabelNode struct.
 */
public class CaseLabelNode {
    private static final int MAX_WRITE_DEPTH = 100;

    public AstNode value;
    public CaseExpect expect;
    public CaseLabelNode nextOr;
    public CaseLabelNode nextLabel;
    public BlockNode block;

    public CaseLabelNode(AstNode value, CaseExpect expect) {
        this.value = value;
        this.expect = expect;
        this.nextOr = null;
        this.nextLabel = null;
        this.block = new BlockNode();
    }

    /**
     * Write the script representation of this case label.
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

        // Write value(s)
        value.writeScriptWithDepth(code, dot, sum, depth + 1);

        // Write chained "or" values
        CaseLabelNode currentOr = nextOr;
        while (currentOr != null) {
            code.write(", ");
            currentOr.value.writeScriptWithDepth(code, dot, sum, depth + 1);
            currentOr = currentOr.nextOr;
        }

        code.write(":");
        code.endLine();
        code.indent();
        block.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.unindent();
    }
}
