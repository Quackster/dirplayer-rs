package com.dirplayer.director.lingo.decompiler;

import com.dirplayer.director.lingo.OpCode;
import com.dirplayer.director.lingo.decompiler.DecompilerEnums.ChunkExprType;
import com.dirplayer.director.lingo.decompiler.DecompilerEnums.DecompilerDatumType;
import com.dirplayer.director.lingo.decompiler.DecompilerEnums.PutType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Main AST node class representing decompiled Lingo code.
 * Port of Rust AstNode enum.
 */
public class AstNode {
    private static final int MAX_WRITE_DEPTH = 100;

    /**
     * AST node types.
     */
    public enum NodeType {
        Error,
        Comment,
        Literal,
        Block,
        Var,
        Assignment,
        BinaryOp,
        InverseOp,
        NotOp,
        ChunkExpr,
        ChunkHilite,
        ChunkDelete,
        SpriteIntersects,
        SpriteWithin,
        Member,
        The,
        TheProp,
        ObjProp,
        ObjBracket,
        ObjPropIndex,
        LastStringChunk,
        StringChunkCount,
        MenuProp,
        MenuItemProp,
        SoundProp,
        SpriteProp,
        Call,
        ObjCall,
        ObjCallV4,
        Exit,
        ExitRepeat,
        NextRepeat,
        Put,
        If,
        RepeatWhile,
        RepeatWithIn,
        RepeatWithTo,
        Tell,
        Case,
        NewObj,
        When,
        SoundCmd,
        PlayCmd
    }

    public NodeType nodeType;

    // Fields for various node types
    public String stringValue;           // Comment, Var, The, member_type, prop, etc.
    public DecompilerDatum datum;        // Literal
    public BlockNode blockNode;          // Block

    // Assignment, BinaryOp, Put
    public AstNode variable;
    public AstNode value;
    public boolean forceVerbose;

    // BinaryOp
    public OpCode opcode;
    public AstNode left;
    public AstNode right;

    // InverseOp, NotOp, ChunkHilite, ChunkDelete
    public AstNode operand;

    // ChunkExpr
    public ChunkExprType chunkType;
    public AstNode first;
    public AstNode last;
    public AstNode string;

    // SpriteIntersects, SpriteWithin
    public AstNode firstSprite;
    public AstNode secondSprite;

    // Member
    public String memberType;
    public AstNode memberId;
    public AstNode castId;

    // TheProp, ObjProp, ObjPropIndex
    public AstNode obj;
    public String prop;
    public AstNode index;
    public AstNode index2;

    // MenuProp, MenuItemProp
    public AstNode menuId;
    public AstNode itemId;
    public int propId;

    // SoundProp, SpriteProp
    public AstNode soundId;
    public AstNode spriteId;

    // Call, ObjCall, NewObj, SoundCmd
    public String name;
    public AstNode args;
    public String objType;
    public String cmd;

    // Put
    public PutType putType;

    // If
    public AstNode condition;
    public BlockNode block1;
    public BlockNode block2;
    public boolean hasElse;

    // RepeatWhile, RepeatWithIn, RepeatWithTo
    public BlockNode block;
    public int startIndex;
    public String varName;
    public AstNode list;
    public AstNode start;
    public AstNode end;
    public boolean up;

    // Tell
    public AstNode window;

    // Case
    public CaseLabelNode firstLabel;
    public OtherwiseNode otherwise;
    public int endPos;
    public int potentialOtherwisePos;

    // When
    public int event;
    public String script;

    private AstNode() {
        this.nodeType = NodeType.Error;
    }

    // Factory methods for creating different node types

    public static AstNode error() {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Error;
        return node;
    }

    public static AstNode comment(String text) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Comment;
        node.stringValue = text;
        return node;
    }

    public static AstNode literal(DecompilerDatum datum) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Literal;
        node.datum = datum;
        return node;
    }

    public static AstNode block(BlockNode block) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Block;
        node.blockNode = block;
        return node;
    }

    public static AstNode var(String name) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Var;
        node.stringValue = name;
        return node;
    }

    public static AstNode assignment(AstNode variable, AstNode value, boolean forceVerbose) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Assignment;
        node.variable = variable;
        node.value = value;
        node.forceVerbose = forceVerbose;
        return node;
    }

    public static AstNode binaryOp(OpCode opcode, AstNode left, AstNode right) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.BinaryOp;
        node.opcode = opcode;
        node.left = left;
        node.right = right;
        return node;
    }

    public static AstNode inverseOp(AstNode operand) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.InverseOp;
        node.operand = operand;
        return node;
    }

    public static AstNode notOp(AstNode operand) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.NotOp;
        node.operand = operand;
        return node;
    }

    public static AstNode chunkExpr(ChunkExprType chunkType, AstNode first, AstNode last, AstNode string) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.ChunkExpr;
        node.chunkType = chunkType;
        node.first = first;
        node.last = last;
        node.string = string;
        return node;
    }

    public static AstNode chunkHilite(AstNode chunk) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.ChunkHilite;
        node.operand = chunk;
        return node;
    }

    public static AstNode chunkDelete(AstNode chunk) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.ChunkDelete;
        node.operand = chunk;
        return node;
    }

    public static AstNode spriteIntersects(AstNode first, AstNode second) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.SpriteIntersects;
        node.firstSprite = first;
        node.secondSprite = second;
        return node;
    }

    public static AstNode spriteWithin(AstNode first, AstNode second) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.SpriteWithin;
        node.firstSprite = first;
        node.secondSprite = second;
        return node;
    }

    public static AstNode member(String memberType, AstNode memberId, AstNode castId) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Member;
        node.memberType = memberType;
        node.memberId = memberId;
        node.castId = castId;
        return node;
    }

    public static AstNode the(String prop) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.The;
        node.stringValue = prop;
        return node;
    }

    public static AstNode theProp(AstNode obj, String prop) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.TheProp;
        node.obj = obj;
        node.prop = prop;
        return node;
    }

    public static AstNode objProp(AstNode obj, String prop) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.ObjProp;
        node.obj = obj;
        node.prop = prop;
        return node;
    }

    public static AstNode objBracket(AstNode obj, AstNode prop) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.ObjBracket;
        node.obj = obj;
        node.operand = prop;
        return node;
    }

    public static AstNode objPropIndex(AstNode obj, String prop, AstNode index, AstNode index2) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.ObjPropIndex;
        node.obj = obj;
        node.prop = prop;
        node.index = index;
        node.index2 = index2;
        return node;
    }

    public static AstNode lastStringChunk(ChunkExprType chunkType, AstNode obj) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.LastStringChunk;
        node.chunkType = chunkType;
        node.obj = obj;
        return node;
    }

    public static AstNode stringChunkCount(ChunkExprType chunkType, AstNode obj) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.StringChunkCount;
        node.chunkType = chunkType;
        node.obj = obj;
        return node;
    }

    public static AstNode menuProp(AstNode menuId, int prop) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.MenuProp;
        node.menuId = menuId;
        node.propId = prop;
        return node;
    }

    public static AstNode menuItemProp(AstNode menuId, AstNode itemId, int prop) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.MenuItemProp;
        node.menuId = menuId;
        node.itemId = itemId;
        node.propId = prop;
        return node;
    }

    public static AstNode soundProp(AstNode soundId, int prop) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.SoundProp;
        node.soundId = soundId;
        node.propId = prop;
        return node;
    }

    public static AstNode spriteProp(AstNode spriteId, int prop) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.SpriteProp;
        node.spriteId = spriteId;
        node.propId = prop;
        return node;
    }

    public static AstNode call(String name, AstNode args) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Call;
        node.name = name;
        node.args = args;
        return node;
    }

    public static AstNode objCall(String name, AstNode args) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.ObjCall;
        node.name = name;
        node.args = args;
        return node;
    }

    public static AstNode objCallV4(AstNode obj, AstNode args) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.ObjCallV4;
        node.obj = obj;
        node.args = args;
        return node;
    }

    public static AstNode exit() {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Exit;
        return node;
    }

    public static AstNode exitRepeat() {
        AstNode node = new AstNode();
        node.nodeType = NodeType.ExitRepeat;
        return node;
    }

    public static AstNode nextRepeat() {
        AstNode node = new AstNode();
        node.nodeType = NodeType.NextRepeat;
        return node;
    }

    public static AstNode put(PutType putType, AstNode variable, AstNode value) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Put;
        node.putType = putType;
        node.variable = variable;
        node.value = value;
        return node;
    }

    public static AstNode ifNode(AstNode condition, BlockNode block1, BlockNode block2, boolean hasElse) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.If;
        node.condition = condition;
        node.block1 = block1;
        node.block2 = block2;
        node.hasElse = hasElse;
        return node;
    }

    public static AstNode repeatWhile(AstNode condition, BlockNode block, int startIndex) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.RepeatWhile;
        node.condition = condition;
        node.block = block;
        node.startIndex = startIndex;
        return node;
    }

    public static AstNode repeatWithIn(String varName, AstNode list, BlockNode block, int startIndex) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.RepeatWithIn;
        node.varName = varName;
        node.list = list;
        node.block = block;
        node.startIndex = startIndex;
        return node;
    }

    public static AstNode repeatWithTo(String varName, AstNode start, AstNode end, boolean up, BlockNode block, int startIndex) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.RepeatWithTo;
        node.varName = varName;
        node.start = start;
        node.end = end;
        node.up = up;
        node.block = block;
        node.startIndex = startIndex;
        return node;
    }

    public static AstNode tell(AstNode window, BlockNode block) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Tell;
        node.window = window;
        node.block = block;
        return node;
    }

    public static AstNode caseNode(AstNode value, CaseLabelNode firstLabel, OtherwiseNode otherwise, int endPos, int potentialOtherwisePos) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.Case;
        node.value = value;
        node.firstLabel = firstLabel;
        node.otherwise = otherwise;
        node.endPos = endPos;
        node.potentialOtherwisePos = potentialOtherwisePos;
        return node;
    }

    public static AstNode newObj(String objType, AstNode args) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.NewObj;
        node.objType = objType;
        node.args = args;
        return node;
    }

    public static AstNode when(int event, String script) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.When;
        node.event = event;
        node.script = script;
        return node;
    }

    public static AstNode soundCmd(String cmd, AstNode args) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.SoundCmd;
        node.cmd = cmd;
        node.args = args;
        return node;
    }

    public static AstNode playCmd(AstNode args) {
        AstNode node = new AstNode();
        node.nodeType = NodeType.PlayCmd;
        node.args = args;
        return node;
    }

    /**
     * Check if this node is an expression (has a value).
     */
    public boolean isExpression() {
        switch (nodeType) {
            case Literal:
            case Var:
            case BinaryOp:
            case InverseOp:
            case NotOp:
            case ChunkExpr:
            case Member:
            case The:
            case TheProp:
            case ObjProp:
            case ObjBracket:
            case ObjPropIndex:
            case LastStringChunk:
            case StringChunkCount:
            case MenuProp:
            case MenuItemProp:
            case SoundProp:
            case SpriteProp:
            case SpriteIntersects:
            case SpriteWithin:
            case NewObj:
                return true;

            case Call:
            case ObjCall:
            case ObjCallV4:
                // Calls are expressions if arg list is NOT ArgListNoRet
                if (args != null && args.nodeType == NodeType.Literal && args.datum != null) {
                    return args.datum.datumType != DecompilerDatumType.ArgListNoRet;
                }
                return true;

            default:
                return false;
        }
    }

    /**
     * Check if this node is a statement.
     */
    public boolean isStatement() {
        switch (nodeType) {
            case Assignment:
            case Exit:
            case ExitRepeat:
            case NextRepeat:
            case Put:
            case If:
            case RepeatWhile:
            case RepeatWithIn:
            case RepeatWithTo:
            case Tell:
            case Case:
            case ChunkHilite:
            case ChunkDelete:
            case When:
            case SoundCmd:
            case PlayCmd:
                return true;

            case Call:
            case ObjCall:
            case ObjCallV4:
                // Calls are statements if arg list IS ArgListNoRet
                if (args != null && args.nodeType == NodeType.Literal && args.datum != null) {
                    return args.datum.datumType == DecompilerDatumType.ArgListNoRet;
                }
                return false;

            default:
                return false;
        }
    }

    /**
     * Get the datum value if this is a literal node.
     */
    public DecompilerDatum getValue() {
        if (nodeType == NodeType.Literal) {
            return datum;
        }
        return null;
    }

    /**
     * Check if writing this node produces spaces (for formatting decisions).
     */
    public boolean hasSpaces(boolean dot) {
        switch (nodeType) {
            case Literal:
                return datum != null &&
                       datum.datumType != DecompilerDatumType.String &&
                       datum.datumType != DecompilerDatumType.Int &&
                       datum.datumType != DecompilerDatumType.Float;
            case Var:
                return true;
            case Member:
                return castId != null || !dot;
            case ObjProp:
            case ObjBracket:
            case ObjPropIndex:
                return !dot;
            case Call:
                if (args != null && args.nodeType == NodeType.Literal && args.datum != null) {
                    return args.datum.listValue.isEmpty();
                }
                return true;
            case ObjCall:
            case ObjCallV4:
                return !dot;
            case Error:
                return false;
            default:
                return true;
        }
    }

    /**
     * Write the script representation of this node.
     */
    public void writeScript(CodeWriter code, boolean dot, boolean sum) {
        writeScriptWithDepth(code, dot, sum, 0);
    }

    void writeScriptWithDepth(CodeWriter code, boolean dot, boolean sum, int depth) {
        if (depth > MAX_WRITE_DEPTH) {
            code.write("/* MAX DEPTH */");
            return;
        }

        switch (nodeType) {
            case Error:
                code.write("ERROR");
                break;

            case Comment:
                code.write("-- ");
                code.write(stringValue);
                break;

            case Literal:
                if (datum != null) {
                    datum.writeScriptWithDepth(code, dot, sum, depth);
                }
                break;

            case Block:
                if (blockNode != null) {
                    blockNode.writeScriptWithDepth(code, dot, sum, depth);
                }
                break;

            case Var:
                code.write(stringValue);
                break;

            case Assignment:
                writeAssignment(code, dot, sum, depth);
                break;

            case BinaryOp:
                writeBinaryOp(code, dot, sum, depth);
                break;

            case InverseOp:
                writeInverseOp(code, dot, sum, depth);
                break;

            case NotOp:
                writeNotOp(code, dot, sum, depth);
                break;

            case ChunkExpr:
                writeChunkExpr(code, dot, sum, depth);
                break;

            case ChunkHilite:
                code.write("hilite ");
                operand.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case ChunkDelete:
                code.write("delete ");
                operand.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case SpriteIntersects:
                code.write("sprite ");
                firstSprite.writeScriptWithDepth(code, dot, sum, depth + 1);
                code.write(" intersects ");
                secondSprite.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case SpriteWithin:
                code.write("sprite ");
                firstSprite.writeScriptWithDepth(code, dot, sum, depth + 1);
                code.write(" within ");
                secondSprite.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case Member:
                writeMember(code, dot, sum, depth);
                break;

            case The:
                code.write("the ");
                code.write(stringValue);
                break;

            case TheProp:
                code.write("the ");
                code.write(prop);
                code.write(" of ");
                obj.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case ObjProp:
                writeObjProp(code, dot, sum, depth);
                break;

            case ObjBracket:
                obj.writeScriptWithDepth(code, dot, sum, depth + 1);
                code.write("[");
                operand.writeScriptWithDepth(code, dot, sum, depth + 1);
                code.write("]");
                break;

            case ObjPropIndex:
                obj.writeScriptWithDepth(code, dot, sum, depth + 1);
                code.write(".");
                code.write(prop);
                code.write("[");
                index.writeScriptWithDepth(code, dot, sum, depth + 1);
                if (index2 != null) {
                    code.write("..");
                    index2.writeScriptWithDepth(code, dot, sum, depth + 1);
                }
                code.write("]");
                break;

            case LastStringChunk:
                code.write("the last ");
                code.write(chunkType.getName());
                code.write(" of ");
                obj.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case StringChunkCount:
                code.write("the number of ");
                code.write(chunkType.getName());
                code.write("s in ");
                obj.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case MenuProp:
                code.write("the ");
                code.write(getMenuPropName(propId));
                code.write(" of menu ");
                menuId.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case MenuItemProp:
                code.write("the ");
                code.write(getMenuItemPropName(propId));
                code.write(" of menuItem ");
                itemId.writeScriptWithDepth(code, dot, sum, depth + 1);
                code.write(" of menu ");
                menuId.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case SoundProp:
                code.write("the ");
                code.write(getSoundPropName(propId));
                code.write(" of sound ");
                soundId.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case SpriteProp:
                code.write("the ");
                code.write(getSpritePropName(propId));
                code.write(" of sprite ");
                spriteId.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case Call:
                writeCall(code, dot, sum, depth);
                break;

            case ObjCall:
                writeObjCall(code, dot, sum, depth);
                break;

            case ObjCallV4:
                obj.writeScriptWithDepth(code, dot, sum, depth + 1);
                code.write("(");
                args.writeScriptWithDepth(code, dot, sum, depth + 1);
                code.write(")");
                break;

            case Exit:
                code.write("exit");
                break;

            case ExitRepeat:
                code.write("exit repeat");
                break;

            case NextRepeat:
                code.write("next repeat");
                break;

            case Put:
                code.write("put ");
                value.writeScriptWithDepth(code, dot, sum, depth + 1);
                code.write(" ");
                code.write(putType.getName());
                code.write(" ");
                variable.writeScriptWithDepth(code, dot, sum, depth + 1);
                break;

            case If:
                writeIf(code, dot, sum, depth);
                break;

            case RepeatWhile:
                writeRepeatWhile(code, dot, sum, depth);
                break;

            case RepeatWithIn:
                writeRepeatWithIn(code, dot, sum, depth);
                break;

            case RepeatWithTo:
                writeRepeatWithTo(code, dot, sum, depth);
                break;

            case Tell:
                writeTell(code, dot, sum, depth);
                break;

            case Case:
                writeCase(code, dot, sum, depth);
                break;

            case NewObj:
                writeNewObj(code, dot, sum, depth);
                break;

            case When:
                code.write("when ");
                code.write(getEventName(event));
                code.write(" then ");
                code.write(script);
                break;

            case SoundCmd:
                writeSoundCmd(code, dot, sum, depth);
                break;

            case PlayCmd:
                writePlayCmd(code, dot, sum, depth);
                break;
        }
    }

    private void writeAssignment(CodeWriter code, boolean dot, boolean sum, int depth) {
        if (dot && !forceVerbose) {
            variable.writeScriptWithDepth(code, dot, sum, depth + 1);
            code.write(" = ");
            value.writeScriptWithDepth(code, dot, sum, depth + 1);
        } else {
            code.write("set ");
            variable.writeScriptWithDepth(code, dot, sum, depth + 1);
            code.write(" to ");
            value.writeScriptWithDepth(code, dot, sum, depth + 1);
        }
    }

    private void writeBinaryOp(CodeWriter code, boolean dot, boolean sum, int depth) {
        int precedence = getPrecedence(opcode);

        boolean leftNeedsParens = false;
        if (left.nodeType == NodeType.BinaryOp) {
            leftNeedsParens = getPrecedence(left.opcode) < precedence;
        }

        boolean rightNeedsParens = false;
        if (right.nodeType == NodeType.BinaryOp) {
            rightNeedsParens = getPrecedence(right.opcode) <= precedence;
        }

        if (leftNeedsParens) code.write("(");
        left.writeScriptWithDepth(code, dot, sum, depth + 1);
        if (leftNeedsParens) code.write(")");

        code.write(" ");
        code.write(getOpString(opcode));
        code.write(" ");

        if (rightNeedsParens) code.write("(");
        right.writeScriptWithDepth(code, dot, sum, depth + 1);
        if (rightNeedsParens) code.write(")");
    }

    private void writeInverseOp(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("-");
        boolean needsParens = operand.nodeType == NodeType.BinaryOp;
        if (needsParens) code.write("(");
        operand.writeScriptWithDepth(code, dot, sum, depth + 1);
        if (needsParens) code.write(")");
    }

    private void writeNotOp(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("not ");
        boolean needsParens = operand.nodeType == NodeType.BinaryOp;
        if (needsParens) code.write("(");
        operand.writeScriptWithDepth(code, dot, sum, depth + 1);
        if (needsParens) code.write(")");
    }

    private void writeChunkExpr(CodeWriter code, boolean dot, boolean sum, int depth) {
        String chunkName = chunkType.getName();

        // Check if first == last for single chunk reference
        boolean isSingle = false;
        if (first.nodeType == NodeType.Literal && last.nodeType == NodeType.Literal &&
            first.datum != null && last.datum != null &&
            first.datum.datumType == DecompilerDatumType.Int &&
            last.datum.datumType == DecompilerDatumType.Int &&
            first.datum.intValue == last.datum.intValue) {
            isSingle = true;
        }

        if (isSingle) {
            code.write(chunkName);
            code.write(" ");
            first.writeScriptWithDepth(code, dot, sum, depth + 1);
            code.write(" of ");
            string.writeScriptWithDepth(code, dot, sum, depth + 1);
        } else {
            code.write(chunkName);
            code.write(" ");
            first.writeScriptWithDepth(code, dot, sum, depth + 1);
            code.write(" to ");
            last.writeScriptWithDepth(code, dot, sum, depth + 1);
            code.write(" of ");
            string.writeScriptWithDepth(code, dot, sum, depth + 1);
        }
    }

    private void writeMember(CodeWriter code, boolean dot, boolean sum, int depth) {
        if (dot) {
            code.write(memberType);
            code.write("(");
            memberId.writeScriptWithDepth(code, dot, sum, depth + 1);
            if (castId != null) {
                code.write(", ");
                castId.writeScriptWithDepth(code, dot, sum, depth + 1);
            }
            code.write(")");
        } else {
            code.write(memberType);
            code.write(" ");
            memberId.writeScriptWithDepth(code, dot, sum, depth + 1);
            if (castId != null) {
                code.write(" of castLib ");
                castId.writeScriptWithDepth(code, dot, sum, depth + 1);
            }
        }
    }

    private void writeObjProp(CodeWriter code, boolean dot, boolean sum, int depth) {
        if (dot) {
            obj.writeScriptWithDepth(code, true, sum, depth + 1);
            code.write(".");
            code.write(prop);
        } else {
            code.write("the ");
            code.write(prop);
            code.write(" of ");
            obj.writeScriptWithDepth(code, dot, sum, depth + 1);
        }
    }

    private static final Set<String> NO_PARENS_COMMANDS = new HashSet<>(Arrays.asList(
        "go", "play", "playaccelerator", "pause", "stop", "halt", "pass", "continue",
        "alert", "beep", "updatestage", "puppetsprite", "puppetsound", "puppetpalette",
        "puppettempo", "puppettransition", "sound", "printwithout", "tell", "return",
        "nothing", "put"
    ));

    private void writeCall(CodeWriter code, boolean dot, boolean sum, int depth) {
        boolean noParens = NO_PARENS_COMMANDS.contains(name.toLowerCase());

        if (args != null && args.nodeType == NodeType.Literal && args.datum != null) {
            boolean isStatement = args.datum.datumType == DecompilerDatumType.ArgListNoRet;

            if (args.datum.listValue.isEmpty()) {
                // Empty argument list
                if (noParens && isStatement) {
                    code.write(name);
                } else {
                    code.write(name);
                    code.write("()");
                }
            } else if (noParens && isStatement) {
                // No-parens statement with arguments
                code.write(name);
                code.write(" ");
                for (int i = 0; i < args.datum.listValue.size(); i++) {
                    if (i > 0) code.write(", ");
                    args.datum.listValue.get(i).writeScriptWithDepth(code, dot, sum, depth + 1);
                }
            } else {
                // Normal function call with parentheses
                code.write(name);
                code.write("(");
                for (int i = 0; i < args.datum.listValue.size(); i++) {
                    if (i > 0) code.write(", ");
                    args.datum.listValue.get(i).writeScriptWithDepth(code, dot, sum, depth + 1);
                }
                code.write(")");
            }
        } else {
            code.write(name);
            code.write("(");
            if (args != null) {
                args.writeScriptWithDepth(code, dot, sum, depth + 1);
            }
            code.write(")");
        }
    }

    private void writeObjCall(CodeWriter code, boolean dot, boolean sum, int depth) {
        if (args != null && args.nodeType == NodeType.Literal && args.datum != null) {
            if (!args.datum.listValue.isEmpty()) {
                AstNode objArg = args.datum.listValue.get(0);
                if (dot) {
                    objArg.writeScriptWithDepth(code, true, sum, depth + 1);
                    code.write(".");
                    code.write(name);
                    code.write("(");
                    for (int i = 1; i < args.datum.listValue.size(); i++) {
                        if (i > 1) code.write(", ");
                        args.datum.listValue.get(i).writeScriptWithDepth(code, true, sum, depth + 1);
                    }
                    code.write(")");
                } else {
                    code.write(name);
                    code.write("(");
                    for (int i = 0; i < args.datum.listValue.size(); i++) {
                        if (i > 0) code.write(", ");
                        args.datum.listValue.get(i).writeScriptWithDepth(code, dot, sum, depth + 1);
                    }
                    code.write(")");
                }
            } else {
                code.write(name);
            }
        } else {
            code.write(name);
            code.write("(");
            if (args != null) {
                args.writeScriptWithDepth(code, dot, sum, depth + 1);
            }
            code.write(")");
        }
    }

    private void writeIf(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("if ");
        condition.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.write(" then");
        code.endLine();
        code.indent();
        block1.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.unindent();
        if (hasElse && block2 != null && !block2.children.isEmpty()) {
            code.write("else");
            code.endLine();
            code.indent();
            block2.writeScriptWithDepth(code, dot, sum, depth + 1);
            code.unindent();
        }
        code.write("end if");
    }

    private void writeRepeatWhile(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("repeat while ");
        condition.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.endLine();
        code.indent();
        block.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.unindent();
        code.write("end repeat");
    }

    private void writeRepeatWithIn(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("repeat with ");
        code.write(varName);
        code.write(" in ");
        list.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.endLine();
        code.indent();
        block.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.unindent();
        code.write("end repeat");
    }

    private void writeRepeatWithTo(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("repeat with ");
        code.write(varName);
        code.write(" = ");
        start.writeScriptWithDepth(code, dot, sum, depth + 1);
        if (up) {
            code.write(" to ");
        } else {
            code.write(" down to ");
        }
        end.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.endLine();
        code.indent();
        block.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.unindent();
        code.write("end repeat");
    }

    private void writeTell(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("tell ");
        window.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.endLine();
        code.indent();
        block.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.unindent();
        code.write("end tell");
    }

    private void writeCase(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("case ");
        value.writeScriptWithDepth(code, dot, sum, depth + 1);
        code.write(" of");
        code.endLine();
        code.indent();

        // Write case labels
        CaseLabelNode currentLabel = firstLabel;
        while (currentLabel != null) {
            currentLabel.writeScriptWithDepth(code, dot, sum, depth + 1);
            currentLabel = currentLabel.nextLabel;
        }

        // Write otherwise
        if (otherwise != null) {
            otherwise.writeScriptWithDepth(code, dot, sum, depth + 1);
        }

        code.unindent();
        code.write("end case");
    }

    private void writeNewObj(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("new(");
        code.write(objType);
        if (args != null && args.nodeType == NodeType.Literal && args.datum != null) {
            if (!args.datum.listValue.isEmpty()) {
                code.write(", ");
                for (int i = 0; i < args.datum.listValue.size(); i++) {
                    if (i > 0) code.write(", ");
                    args.datum.listValue.get(i).writeScriptWithDepth(code, dot, sum, depth + 1);
                }
            }
        }
        code.write(")");
    }

    private void writeSoundCmd(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("sound ");
        code.write(cmd);
        if (args != null && args.nodeType == NodeType.Literal && args.datum != null) {
            if (!args.datum.listValue.isEmpty()) {
                code.write(" ");
                for (int i = 0; i < args.datum.listValue.size(); i++) {
                    if (i > 0) code.write(", ");
                    args.datum.listValue.get(i).writeScriptWithDepth(code, dot, sum, depth + 1);
                }
            }
        }
    }

    private void writePlayCmd(CodeWriter code, boolean dot, boolean sum, int depth) {
        code.write("play");
        if (args != null && args.nodeType == NodeType.Literal && args.datum != null) {
            if (!args.datum.listValue.isEmpty()) {
                code.write(" ");
                for (int i = 0; i < args.datum.listValue.size(); i++) {
                    if (i > 0) code.write(", ");
                    args.datum.listValue.get(i).writeScriptWithDepth(code, dot, sum, depth + 1);
                }
            }
        }
    }

    // Helper methods for operator precedence and names

    private static String getOpString(OpCode opcode) {
        switch (opcode) {
            case Mul: return "*";
            case Add: return "+";
            case Sub: return "-";
            case Div: return "/";
            case Mod: return "mod";
            case JoinStr: return "&";
            case JoinPadStr: return "&&";
            case Lt: return "<";
            case LtEq: return "<=";
            case NtEq: return "<>";
            case Eq: return "=";
            case Gt: return ">";
            case GtEq: return ">=";
            case And: return "and";
            case Or: return "or";
            case ContainsStr:
            case Contains0Str: return "contains";
            default: return "???";
        }
    }

    private static int getPrecedence(OpCode opcode) {
        switch (opcode) {
            case Or: return 1;
            case And: return 2;
            case ContainsStr:
            case Contains0Str: return 3;
            case Lt:
            case LtEq:
            case NtEq:
            case Eq:
            case Gt:
            case GtEq: return 4;
            case JoinStr:
            case JoinPadStr: return 5;
            case Add:
            case Sub: return 6;
            case Mul:
            case Div:
            case Mod: return 7;
            default: return 0;
        }
    }

    private static String getMenuPropName(int prop) {
        switch (prop) {
            case 0x01: return "name";
            case 0x02: return "number";
            default: return "menuProp_" + prop;
        }
    }

    private static String getMenuItemPropName(int prop) {
        switch (prop) {
            case 0x01: return "name";
            case 0x02: return "checkMark";
            case 0x03: return "enabled";
            case 0x04: return "script";
            default: return "menuItemProp_" + prop;
        }
    }

    private static String getSoundPropName(int prop) {
        switch (prop) {
            case 0x01: return "volume";
            default: return "soundProp_" + prop;
        }
    }

    private static String getSpritePropName(int prop) {
        switch (prop) {
            case 0x01: return "type";
            case 0x02: return "backColor";
            case 0x03: return "bottom";
            case 0x04: return "castNum";
            case 0x05: return "constraint";
            case 0x06: return "cursor";
            case 0x07: return "foreColor";
            case 0x08: return "height";
            case 0x09: return "immediate";
            case 0x0a: return "ink";
            case 0x0b: return "left";
            case 0x0c: return "lineSize";
            case 0x0d: return "locH";
            case 0x0e: return "locV";
            case 0x0f: return "moveableSprite";
            case 0x10: return "pattern";
            case 0x11: return "puppet";
            case 0x12: return "right";
            case 0x13: return "scriptNum";
            case 0x14: return "stretch";
            case 0x15: return "top";
            case 0x16: return "trails";
            case 0x17: return "visible";
            case 0x18: return "width";
            case 0x19: return "blend";
            case 0x1a: return "scriptInstanceList";
            case 0x1b: return "loc";
            case 0x1c: return "rect";
            case 0x1d: return "member";
            default: return "spriteProp_" + prop;
        }
    }

    private static String getEventName(int event) {
        switch (event) {
            case 1: return "mouseDown";
            case 2: return "mouseUp";
            case 3: return "keyDown";
            case 4: return "keyUp";
            case 5: return "timeout";
            default: return "event_" + event;
        }
    }
}
