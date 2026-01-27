package com.dirplayer.player.eval;

import com.dirplayer.player.ScriptError;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Lingo command parsing.
 * Port of Rust vm-rust/tests/lingo/command_parsing.rs
 */
public class CommandParsingTest {

    private LingoExpr parseCommand(String source) throws ScriptError {
        return LingoParser.parseCommand(source);
    }

    // ============ Handler Call Tests ============

    @Test
    void testGlobalHandlerNoArgs() {
        // put() without args - may fail or succeed depending on grammar
        // Original Rust test expects this to fail
        assertThrows(ScriptError.class, () -> parseCommand("put()"));
    }

    @Test
    void testGlobalHandlerOneArg() throws ScriptError {
        // put(1) displays the value 1
        LingoExpr ast = parseCommand("put(1)");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.IntLiteral.class, put.value);
        assertEquals(1, ((LingoExpr.IntLiteral) put.value).value);
    }

    @Test
    void testGlobalHandlerMultiArgs() throws ScriptError {
        // put(1, 2, 3) is a handler call
        LingoExpr ast = parseCommand("put(1, 2, 3)");
        assertInstanceOf(LingoExpr.HandlerCall.class, ast);
        LingoExpr.HandlerCall call = (LingoExpr.HandlerCall) ast;
        assertEquals("put", call.handlerName);
        assertEquals(3, call.args.size());
        assertEquals(1, ((LingoExpr.IntLiteral) call.args.get(0)).value);
        assertEquals(2, ((LingoExpr.IntLiteral) call.args.get(1)).value);
        assertEquals(3, ((LingoExpr.IntLiteral) call.args.get(2)).value);
    }

    @Test
    void testCommandNoArgs() throws ScriptError {
        LingoExpr ast = parseCommand("put");
        assertInstanceOf(LingoExpr.HandlerCall.class, ast);
        LingoExpr.HandlerCall call = (LingoExpr.HandlerCall) ast;
        assertEquals("put", call.handlerName);
        assertEquals(0, call.args.size());
    }

    @Test
    void testCommandOneArg() throws ScriptError {
        // put 1 without parens is PutDisplay
        LingoExpr ast = parseCommand("put 1");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.IntLiteral.class, put.value);
        assertEquals(1, ((LingoExpr.IntLiteral) put.value).value);
    }

    @Test
    void testCommandMultiArgs() throws ScriptError {
        LingoExpr ast = parseCommand("put 1, 2, 3");
        assertInstanceOf(LingoExpr.HandlerCall.class, ast);
        LingoExpr.HandlerCall call = (LingoExpr.HandlerCall) ast;
        assertEquals("put", call.handlerName);
        assertEquals(3, call.args.size());
    }

    @Test
    void testCommandMultiArgsInline() {
        // "put 1 2 3" should fail - space-separated args without commas
        assertThrows(ScriptError.class, () -> parseCommand("put 1 2 3"));
    }

    @Test
    void testCommandMultiArgsMixed() {
        // "put 1 2, 3" should fail - mixed spacing
        assertThrows(ScriptError.class, () -> parseCommand("put 1 2, 3"));
    }

    // ============ Assignment Tests ============

    @Test
    void testTopLevelAssignment() throws ScriptError {
        LingoExpr ast = parseCommand("obj = 1");
        assertInstanceOf(LingoExpr.Assignment.class, ast);
        LingoExpr.Assignment assign = (LingoExpr.Assignment) ast;

        assertInstanceOf(LingoExpr.Identifier.class, assign.target);
        assertEquals("obj", ((LingoExpr.Identifier) assign.target).name);

        assertInstanceOf(LingoExpr.IntLiteral.class, assign.value);
        assertEquals(1, ((LingoExpr.IntLiteral) assign.value).value);
    }

    @Test
    void testDeepAssignment() throws ScriptError {
        LingoExpr ast = parseCommand("obj.prop = 1");
        assertInstanceOf(LingoExpr.Assignment.class, ast);
        LingoExpr.Assignment assign = (LingoExpr.Assignment) ast;

        assertInstanceOf(LingoExpr.ObjProp.class, assign.target);
        LingoExpr.ObjProp objProp = (LingoExpr.ObjProp) assign.target;
        assertEquals("prop", objProp.propName);
        assertInstanceOf(LingoExpr.Identifier.class, objProp.object);
        assertEquals("obj", ((LingoExpr.Identifier) objProp.object).name);

        assertInstanceOf(LingoExpr.IntLiteral.class, assign.value);
        assertEquals(1, ((LingoExpr.IntLiteral) assign.value).value);
    }

    @Test
    void testObjHandlerCallNoArgs() throws ScriptError {
        LingoExpr ast = parseCommand("obj.handler()");
        assertInstanceOf(LingoExpr.ObjHandlerCall.class, ast);
        LingoExpr.ObjHandlerCall call = (LingoExpr.ObjHandlerCall) ast;
        assertEquals("handler", call.handlerName);
        assertEquals(0, call.args.size());
        assertInstanceOf(LingoExpr.Identifier.class, call.object);
        assertEquals("obj", ((LingoExpr.Identifier) call.object).name);
    }

    // ============ PUT Display Tests ============

    @Test
    void testPutDisplayString() throws ScriptError {
        LingoExpr ast = parseCommand("put \"hello world\"");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.StringLiteral.class, put.value);
        assertEquals("hello world", ((LingoExpr.StringLiteral) put.value).value);
    }

    @Test
    void testPutDisplayFloat() throws ScriptError {
        LingoExpr ast = parseCommand("put 3.14");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.FloatLiteral.class, put.value);
        assertEquals(3.14, ((LingoExpr.FloatLiteral) put.value).value, 0.001);
    }

    @Test
    void testPutDisplaySymbol() throws ScriptError {
        LingoExpr ast = parseCommand("put #mySymbol");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.SymbolLiteral.class, put.value);
        assertEquals("mySymbol", ((LingoExpr.SymbolLiteral) put.value).value);
    }

    @Test
    void testPutDisplayVoid() throws ScriptError {
        LingoExpr ast = parseCommand("put void");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertSame(LingoExpr.VoidLiteral.INSTANCE, put.value);
    }

    @Test
    void testPutDisplayList() throws ScriptError {
        LingoExpr ast = parseCommand("put [1, 2, 3]");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.ListLiteral.class, put.value);
        LingoExpr.ListLiteral list = (LingoExpr.ListLiteral) put.value;
        assertEquals(3, list.items.size());
    }

    @Test
    void testPutDisplayProplist() throws ScriptError {
        LingoExpr ast = parseCommand("put [#a: 1, #b: 2]");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.PropListLiteral.class, put.value);
        LingoExpr.PropListLiteral propList = (LingoExpr.PropListLiteral) put.value;
        assertEquals(2, propList.entries.size());
    }

    @Test
    void testPutDisplayIdentifier() throws ScriptError {
        LingoExpr ast = parseCommand("put x");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.Identifier.class, put.value);
        assertEquals("x", ((LingoExpr.Identifier) put.value).name);
    }

    // ============ PUT Display Expression Tests ============

    @Test
    void testPutDisplayAddition() throws ScriptError {
        LingoExpr ast = parseCommand("put 5 + 3");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.Add.class, put.value);
        LingoExpr.Add add = (LingoExpr.Add) put.value;
        assertEquals(5, ((LingoExpr.IntLiteral) add.left).value);
        assertEquals(3, ((LingoExpr.IntLiteral) add.right).value);
    }

    @Test
    void testPutDisplayConcatenation() throws ScriptError {
        LingoExpr ast = parseCommand("put \"value: \" & x");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.Join.class, put.value);
        LingoExpr.Join join = (LingoExpr.Join) put.value;
        assertInstanceOf(LingoExpr.StringLiteral.class, join.left);
        assertEquals("value: ", ((LingoExpr.StringLiteral) join.left).value);
        assertInstanceOf(LingoExpr.Identifier.class, join.right);
        assertEquals("x", ((LingoExpr.Identifier) join.right).name);
    }

    @Test
    void testPutDisplayComparison() throws ScriptError {
        LingoExpr ast = parseCommand("put x = 5");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.Eq.class, put.value);
        LingoExpr.Eq eq = (LingoExpr.Eq) put.value;
        assertInstanceOf(LingoExpr.Identifier.class, eq.left);
        assertEquals("x", ((LingoExpr.Identifier) eq.left).name);
        assertEquals(5, ((LingoExpr.IntLiteral) eq.right).value);
    }

    @Test
    void testPutDisplayAndOperation() throws ScriptError {
        LingoExpr ast = parseCommand("put x and y");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.And.class, put.value);
        LingoExpr.And and = (LingoExpr.And) put.value;
        assertEquals("x", ((LingoExpr.Identifier) and.left).name);
        assertEquals("y", ((LingoExpr.Identifier) and.right).name);
    }

    @Test
    void testPutDisplayOrOperation() throws ScriptError {
        LingoExpr ast = parseCommand("put x or y");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.Or.class, put.value);
        LingoExpr.Or or = (LingoExpr.Or) put.value;
        assertEquals("x", ((LingoExpr.Identifier) or.left).name);
        assertEquals("y", ((LingoExpr.Identifier) or.right).name);
    }

    @Test
    void testPutDisplayNotOperation() throws ScriptError {
        LingoExpr ast = parseCommand("put not x");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.Not.class, put.value);
        LingoExpr.Not not = (LingoExpr.Not) put.value;
        assertInstanceOf(LingoExpr.Identifier.class, not.operand);
        assertEquals("x", ((LingoExpr.Identifier) not.operand).name);
    }

    @Test
    void testPutDisplayHandlerCall() throws ScriptError {
        LingoExpr ast = parseCommand("put ilk(x)");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.HandlerCall.class, put.value);
        LingoExpr.HandlerCall call = (LingoExpr.HandlerCall) put.value;
        assertEquals("ilk", call.handlerName);
        assertEquals(1, call.args.size());
    }

    // ============ PUT "the" Property Tests ============

    @Test
    void testPutDisplayTheProperty() throws ScriptError {
        LingoExpr ast = parseCommand("put the itemDelimiter");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.Identifier.class, put.value);
        assertEquals("the itemDelimiter", ((LingoExpr.Identifier) put.value).name);
    }

    @Test
    void testPutDisplayTheMouseLoc() throws ScriptError {
        LingoExpr ast = parseCommand("put the mouseLoc");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) ast;
        assertInstanceOf(LingoExpr.Identifier.class, put.value);
        assertEquals("the mouseLoc", ((LingoExpr.Identifier) put.value).name);
    }

    @Test
    void testPutDisplaySpriteWithTheProperty() throws ScriptError {
        // put the rect of sprite the currentSpriteNum
        LingoExpr ast = parseCommand("put the rect of sprite the currentSpriteNum");
        assertInstanceOf(LingoExpr.PutDisplay.class, ast);
        // Should parse successfully (structure check done by parsing without error)
    }

    // ============ PUT INTO Tests ============

    @Test
    void testPutIntoBasic() throws ScriptError {
        LingoExpr ast = parseCommand("put 42 into x");
        assertInstanceOf(LingoExpr.PutInto.class, ast);
        LingoExpr.PutInto putInto = (LingoExpr.PutInto) ast;
        assertInstanceOf(LingoExpr.IntLiteral.class, putInto.value);
        assertEquals(42, ((LingoExpr.IntLiteral) putInto.value).value);
        assertInstanceOf(LingoExpr.Identifier.class, putInto.target);
        assertEquals("x", ((LingoExpr.Identifier) putInto.target).name);
    }

    @Test
    void testPutIntoString() throws ScriptError {
        LingoExpr ast = parseCommand("put \"hello\" into myString");
        assertInstanceOf(LingoExpr.PutInto.class, ast);
        LingoExpr.PutInto putInto = (LingoExpr.PutInto) ast;
        assertEquals("hello", ((LingoExpr.StringLiteral) putInto.value).value);
        assertEquals("myString", ((LingoExpr.Identifier) putInto.target).name);
    }

    @Test
    void testPutIntoList() throws ScriptError {
        LingoExpr ast = parseCommand("put [1, 2, 3] into myList");
        assertInstanceOf(LingoExpr.PutInto.class, ast);
        LingoExpr.PutInto putInto = (LingoExpr.PutInto) ast;
        assertInstanceOf(LingoExpr.ListLiteral.class, putInto.value);
        assertEquals("myList", ((LingoExpr.Identifier) putInto.target).name);
    }

    @Test
    void testPutIntoExpression() throws ScriptError {
        LingoExpr ast = parseCommand("put 5 + 3 into result");
        assertInstanceOf(LingoExpr.PutInto.class, ast);
        LingoExpr.PutInto putInto = (LingoExpr.PutInto) ast;
        assertInstanceOf(LingoExpr.Add.class, putInto.value);
        assertEquals("result", ((LingoExpr.Identifier) putInto.target).name);
    }

    // ============ PUT BEFORE Tests ============

    @Test
    void testPutBeforeBasic() throws ScriptError {
        LingoExpr ast = parseCommand("put \"hello \" before myStr");
        assertInstanceOf(LingoExpr.PutBefore.class, ast);
        LingoExpr.PutBefore putBefore = (LingoExpr.PutBefore) ast;
        assertEquals("hello ", ((LingoExpr.StringLiteral) putBefore.value).value);
        assertEquals("myStr", ((LingoExpr.Identifier) putBefore.target).name);
    }

    @Test
    void testPutBeforeExpression() throws ScriptError {
        LingoExpr ast = parseCommand("put x & \" \" before myStr");
        assertInstanceOf(LingoExpr.PutBefore.class, ast);
        LingoExpr.PutBefore putBefore = (LingoExpr.PutBefore) ast;
        assertInstanceOf(LingoExpr.Join.class, putBefore.value);
        assertEquals("myStr", ((LingoExpr.Identifier) putBefore.target).name);
    }

    // ============ PUT AFTER Tests ============

    @Test
    void testPutAfterBasic() throws ScriptError {
        LingoExpr ast = parseCommand("put \" world\" after myStr");
        assertInstanceOf(LingoExpr.PutAfter.class, ast);
        LingoExpr.PutAfter putAfter = (LingoExpr.PutAfter) ast;
        assertEquals(" world", ((LingoExpr.StringLiteral) putAfter.value).value);
        assertEquals("myStr", ((LingoExpr.Identifier) putAfter.target).name);
    }

    @Test
    void testPutAfterExpression() throws ScriptError {
        LingoExpr ast = parseCommand("put \" \" & x after myStr");
        assertInstanceOf(LingoExpr.PutAfter.class, ast);
        LingoExpr.PutAfter putAfter = (LingoExpr.PutAfter) ast;
        assertInstanceOf(LingoExpr.Join.class, putAfter.value);
        assertEquals("myStr", ((LingoExpr.Identifier) putAfter.target).name);
    }

    // ============ PUT INTO/BEFORE/AFTER Chunk Tests ============

    @Test
    void testPutIntoChar() throws ScriptError {
        LingoExpr ast = parseCommand("put \"X\" into char 1 of myStr");
        assertInstanceOf(LingoExpr.PutInto.class, ast);
        LingoExpr.PutInto putInto = (LingoExpr.PutInto) ast;
        assertEquals("X", ((LingoExpr.StringLiteral) putInto.value).value);

        assertInstanceOf(LingoExpr.ChunkExpr.class, putInto.target);
        LingoExpr.ChunkExpr chunk = (LingoExpr.ChunkExpr) putInto.target;
        assertEquals("char", chunk.chunkType);
        assertEquals(1, ((LingoExpr.IntLiteral) chunk.startIndex).value);
        assertEquals("myStr", ((LingoExpr.Identifier) chunk.source).name);
    }

    @Test
    void testPutIntoWord() throws ScriptError {
        LingoExpr ast = parseCommand("put \"goodbye\" into word 1 of myStr");
        assertInstanceOf(LingoExpr.PutInto.class, ast);
        LingoExpr.PutInto putInto = (LingoExpr.PutInto) ast;

        assertInstanceOf(LingoExpr.ChunkExpr.class, putInto.target);
        LingoExpr.ChunkExpr chunk = (LingoExpr.ChunkExpr) putInto.target;
        assertEquals("word", chunk.chunkType);
    }

    @Test
    void testPutIntoLine() throws ScriptError {
        LingoExpr ast = parseCommand("put \"newline\" into line 1 of myText");
        assertInstanceOf(LingoExpr.PutInto.class, ast);
        LingoExpr.PutInto putInto = (LingoExpr.PutInto) ast;

        assertInstanceOf(LingoExpr.ChunkExpr.class, putInto.target);
        LingoExpr.ChunkExpr chunk = (LingoExpr.ChunkExpr) putInto.target;
        assertEquals("line", chunk.chunkType);
    }

    @Test
    void testPutIntoItem() throws ScriptError {
        LingoExpr ast = parseCommand("put \"X\" into item 2 of myList");
        assertInstanceOf(LingoExpr.PutInto.class, ast);
        LingoExpr.PutInto putInto = (LingoExpr.PutInto) ast;

        assertInstanceOf(LingoExpr.ChunkExpr.class, putInto.target);
        LingoExpr.ChunkExpr chunk = (LingoExpr.ChunkExpr) putInto.target;
        assertEquals("item", chunk.chunkType);
        assertEquals(2, ((LingoExpr.IntLiteral) chunk.startIndex).value);
    }

    @Test
    void testPutBeforeChar() throws ScriptError {
        LingoExpr ast = parseCommand("put \"X\" before char 1 of myStr");
        assertInstanceOf(LingoExpr.PutBefore.class, ast);
        LingoExpr.PutBefore putBefore = (LingoExpr.PutBefore) ast;

        assertInstanceOf(LingoExpr.ChunkExpr.class, putBefore.target);
        LingoExpr.ChunkExpr chunk = (LingoExpr.ChunkExpr) putBefore.target;
        assertEquals("char", chunk.chunkType);
    }

    @Test
    void testPutAfterChar() throws ScriptError {
        LingoExpr ast = parseCommand("put \"X\" after char 5 of myStr");
        assertInstanceOf(LingoExpr.PutAfter.class, ast);
        LingoExpr.PutAfter putAfter = (LingoExpr.PutAfter) ast;

        assertInstanceOf(LingoExpr.ChunkExpr.class, putAfter.target);
        LingoExpr.ChunkExpr chunk = (LingoExpr.ChunkExpr) putAfter.target;
        assertEquals("char", chunk.chunkType);
        assertEquals(5, ((LingoExpr.IntLiteral) chunk.startIndex).value);
    }

    @Test
    void testPutBeforeWord() throws ScriptError {
        LingoExpr ast = parseCommand("put \"beautiful \" before word 2 of myStr");
        assertInstanceOf(LingoExpr.PutBefore.class, ast);
        LingoExpr.PutBefore putBefore = (LingoExpr.PutBefore) ast;

        assertInstanceOf(LingoExpr.ChunkExpr.class, putBefore.target);
        LingoExpr.ChunkExpr chunk = (LingoExpr.ChunkExpr) putBefore.target;
        assertEquals("word", chunk.chunkType);
        assertEquals(2, ((LingoExpr.IntLiteral) chunk.startIndex).value);
    }

    @Test
    void testPutAfterWord() throws ScriptError {
        LingoExpr ast = parseCommand("put \"!\" after word 2 of myStr");
        assertInstanceOf(LingoExpr.PutAfter.class, ast);
        LingoExpr.PutAfter putAfter = (LingoExpr.PutAfter) ast;

        assertInstanceOf(LingoExpr.ChunkExpr.class, putAfter.target);
        LingoExpr.ChunkExpr chunk = (LingoExpr.ChunkExpr) putAfter.target;
        assertEquals("word", chunk.chunkType);
    }
}
