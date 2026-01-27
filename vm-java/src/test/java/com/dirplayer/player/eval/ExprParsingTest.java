package com.dirplayer.player.eval;

import com.dirplayer.player.ScriptError;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Tests for Lingo expression parsing.
 * Port of Rust vm-rust/tests/lingo/expr_parsing.rs
 */
public class ExprParsingTest {

    // Helper method to parse expression and return AST
    private LingoExpr parse(String source) throws ScriptError {
        return LingoParser.parse(source);
    }

    // ============ Literal Tests ============

    @Test
    void testSymbol() throws ScriptError {
        LingoExpr ast = parse("#symbol");
        assertInstanceOf(LingoExpr.SymbolLiteral.class, ast);
        assertEquals("symbol", ((LingoExpr.SymbolLiteral) ast).value);
    }

    @Test
    void testString() throws ScriptError {
        LingoExpr ast = parse("\"string\"");
        assertInstanceOf(LingoExpr.StringLiteral.class, ast);
        assertEquals("string", ((LingoExpr.StringLiteral) ast).value);
    }

    @Test
    void testInt() throws ScriptError {
        LingoExpr ast = parse("42");
        assertInstanceOf(LingoExpr.IntLiteral.class, ast);
        assertEquals(42, ((LingoExpr.IntLiteral) ast).value);
    }

    @Test
    void testNegInt() throws ScriptError {
        LingoExpr ast = parse("-42");
        // Negative integers might be parsed as Negate(IntLiteral) or IntLiteral(-42)
        if (ast instanceof LingoExpr.IntLiteral) {
            assertEquals(-42, ((LingoExpr.IntLiteral) ast).value);
        } else if (ast instanceof LingoExpr.Negate) {
            LingoExpr.Negate neg = (LingoExpr.Negate) ast;
            assertInstanceOf(LingoExpr.IntLiteral.class, neg.operand);
            assertEquals(42, ((LingoExpr.IntLiteral) neg.operand).value);
        } else {
            fail("Expected IntLiteral or Negate, got " + ast.getClass().getSimpleName());
        }
    }

    @Test
    void testFloat() throws ScriptError {
        LingoExpr ast = parse("42.5");
        assertInstanceOf(LingoExpr.FloatLiteral.class, ast);
        assertEquals(42.5, ((LingoExpr.FloatLiteral) ast).value);
    }

    @Test
    void testFloatEndingWithDot() throws ScriptError {
        LingoExpr ast = parse("42.");
        assertInstanceOf(LingoExpr.FloatLiteral.class, ast);
        assertEquals(42.0, ((LingoExpr.FloatLiteral) ast).value);
    }

    @Test
    void testNegFloat() throws ScriptError {
        LingoExpr ast = parse("-42.5");
        // Similar to negative int, may be Negate(FloatLiteral) or FloatLiteral(-42.5)
        if (ast instanceof LingoExpr.FloatLiteral) {
            assertEquals(-42.5, ((LingoExpr.FloatLiteral) ast).value);
        } else if (ast instanceof LingoExpr.Negate) {
            LingoExpr.Negate neg = (LingoExpr.Negate) ast;
            assertInstanceOf(LingoExpr.FloatLiteral.class, neg.operand);
            assertEquals(42.5, ((LingoExpr.FloatLiteral) neg.operand).value);
        } else {
            fail("Expected FloatLiteral or Negate, got " + ast.getClass().getSimpleName());
        }
    }

    @Test
    void testListEmpty() throws ScriptError {
        LingoExpr ast = parse("[]");
        assertInstanceOf(LingoExpr.ListLiteral.class, ast);
        assertEquals(0, ((LingoExpr.ListLiteral) ast).items.size());
    }

    @Test
    void testListSingle() throws ScriptError {
        LingoExpr ast = parse("[1]");
        assertInstanceOf(LingoExpr.ListLiteral.class, ast);
        List<LingoExpr> items = ((LingoExpr.ListLiteral) ast).items;
        assertEquals(1, items.size());
        assertInstanceOf(LingoExpr.IntLiteral.class, items.get(0));
        assertEquals(1, ((LingoExpr.IntLiteral) items.get(0)).value);
    }

    @Test
    void testListMulti() throws ScriptError {
        LingoExpr ast = parse("[1, 2, 3]");
        assertInstanceOf(LingoExpr.ListLiteral.class, ast);
        List<LingoExpr> items = ((LingoExpr.ListLiteral) ast).items;
        assertEquals(3, items.size());
        assertEquals(1, ((LingoExpr.IntLiteral) items.get(0)).value);
        assertEquals(2, ((LingoExpr.IntLiteral) items.get(1)).value);
        assertEquals(3, ((LingoExpr.IntLiteral) items.get(2)).value);
    }

    @Test
    void testProplistEmpty() throws ScriptError {
        LingoExpr ast = parse("[:]");
        assertInstanceOf(LingoExpr.PropListLiteral.class, ast);
        assertEquals(0, ((LingoExpr.PropListLiteral) ast).entries.size());
    }

    @Test
    void testProplistSingle() throws ScriptError {
        LingoExpr ast = parse("[#key1: 1]");
        assertInstanceOf(LingoExpr.PropListLiteral.class, ast);
        List<LingoExpr.PropListEntry> entries = ((LingoExpr.PropListLiteral) ast).entries;
        assertEquals(1, entries.size());

        LingoExpr.PropListEntry entry = entries.get(0);
        assertInstanceOf(LingoExpr.SymbolLiteral.class, entry.key);
        assertEquals("key1", ((LingoExpr.SymbolLiteral) entry.key).value);
        assertInstanceOf(LingoExpr.IntLiteral.class, entry.value);
        assertEquals(1, ((LingoExpr.IntLiteral) entry.value).value);
    }

    @Test
    void testProplistMulti() throws ScriptError {
        LingoExpr ast = parse("[#key1: 1, #key2: 2, #key3: 3]");
        assertInstanceOf(LingoExpr.PropListLiteral.class, ast);
        List<LingoExpr.PropListEntry> entries = ((LingoExpr.PropListLiteral) ast).entries;
        assertEquals(3, entries.size());

        assertEquals("key1", ((LingoExpr.SymbolLiteral) entries.get(0).key).value);
        assertEquals(1, ((LingoExpr.IntLiteral) entries.get(0).value).value);
        assertEquals("key2", ((LingoExpr.SymbolLiteral) entries.get(1).key).value);
        assertEquals(2, ((LingoExpr.IntLiteral) entries.get(1).value).value);
        assertEquals("key3", ((LingoExpr.SymbolLiteral) entries.get(2).key).value);
        assertEquals(3, ((LingoExpr.IntLiteral) entries.get(2).value).value);
    }

    @Test
    void testVoid() throws ScriptError {
        LingoExpr ast = parse("void");
        assertSame(LingoExpr.VoidLiteral.INSTANCE, ast);
    }

    @Test
    void testBool() throws ScriptError {
        LingoExpr astTrue = parse("true");
        assertInstanceOf(LingoExpr.BoolLiteral.class, astTrue);
        assertTrue(((LingoExpr.BoolLiteral) astTrue).value);

        LingoExpr astFalse = parse("false");
        assertInstanceOf(LingoExpr.BoolLiteral.class, astFalse);
        assertFalse(((LingoExpr.BoolLiteral) astFalse).value);
    }

    // ============ Handler Call Tests ============

    @Test
    void testHandlerCallNoArgs() throws ScriptError {
        LingoExpr ast = parse("handler_call()");
        assertInstanceOf(LingoExpr.HandlerCall.class, ast);
        LingoExpr.HandlerCall call = (LingoExpr.HandlerCall) ast;
        assertEquals("handler_call", call.handlerName);
        assertEquals(0, call.args.size());
    }

    @Test
    void testHandlerCallSingleArg() throws ScriptError {
        LingoExpr ast = parse("handler_call(1)");
        assertInstanceOf(LingoExpr.HandlerCall.class, ast);
        LingoExpr.HandlerCall call = (LingoExpr.HandlerCall) ast;
        assertEquals("handler_call", call.handlerName);
        assertEquals(1, call.args.size());
        assertEquals(1, ((LingoExpr.IntLiteral) call.args.get(0)).value);
    }

    @Test
    void testHandlerCallMultiArgs() throws ScriptError {
        LingoExpr ast = parse("handler_call(1, 2, 3)");
        assertInstanceOf(LingoExpr.HandlerCall.class, ast);
        LingoExpr.HandlerCall call = (LingoExpr.HandlerCall) ast;
        assertEquals("handler_call", call.handlerName);
        assertEquals(3, call.args.size());
        assertEquals(1, ((LingoExpr.IntLiteral) call.args.get(0)).value);
        assertEquals(2, ((LingoExpr.IntLiteral) call.args.get(1)).value);
        assertEquals(3, ((LingoExpr.IntLiteral) call.args.get(2)).value);
    }

    // ============ Object Property Tests ============

    @Test
    void testObjProp() throws ScriptError {
        LingoExpr ast = parse("obj.prop");
        assertInstanceOf(LingoExpr.ObjProp.class, ast);
        LingoExpr.ObjProp objProp = (LingoExpr.ObjProp) ast;
        assertInstanceOf(LingoExpr.Identifier.class, objProp.object);
        assertEquals("obj", ((LingoExpr.Identifier) objProp.object).name);
        assertEquals("prop", objProp.propName);
    }

    @Test
    void testDeepObjProp() throws ScriptError {
        LingoExpr ast = parse("obj.prop.subprop");
        assertInstanceOf(LingoExpr.ObjProp.class, ast);
        LingoExpr.ObjProp outer = (LingoExpr.ObjProp) ast;
        assertEquals("subprop", outer.propName);

        assertInstanceOf(LingoExpr.ObjProp.class, outer.object);
        LingoExpr.ObjProp inner = (LingoExpr.ObjProp) outer.object;
        assertEquals("prop", inner.propName);
        assertInstanceOf(LingoExpr.Identifier.class, inner.object);
        assertEquals("obj", ((LingoExpr.Identifier) inner.object).name);
    }

    // ============ Object Handler Call Tests ============

    @Test
    void testObjHandlerCallNoArgs() throws ScriptError {
        LingoExpr ast = parse("obj.handler()");
        assertInstanceOf(LingoExpr.ObjHandlerCall.class, ast);
        LingoExpr.ObjHandlerCall call = (LingoExpr.ObjHandlerCall) ast;
        assertInstanceOf(LingoExpr.Identifier.class, call.object);
        assertEquals("obj", ((LingoExpr.Identifier) call.object).name);
        assertEquals("handler", call.handlerName);
        assertEquals(0, call.args.size());
    }

    @Test
    void testObjHandlerCallSingleArg() throws ScriptError {
        LingoExpr ast = parse("obj.handler(1)");
        assertInstanceOf(LingoExpr.ObjHandlerCall.class, ast);
        LingoExpr.ObjHandlerCall call = (LingoExpr.ObjHandlerCall) ast;
        assertEquals("obj", ((LingoExpr.Identifier) call.object).name);
        assertEquals("handler", call.handlerName);
        assertEquals(1, call.args.size());
        assertEquals(1, ((LingoExpr.IntLiteral) call.args.get(0)).value);
    }

    @Test
    void testObjHandlerCallMultiArg() throws ScriptError {
        LingoExpr ast = parse("obj.handler(1, 2, 3)");
        assertInstanceOf(LingoExpr.ObjHandlerCall.class, ast);
        LingoExpr.ObjHandlerCall call = (LingoExpr.ObjHandlerCall) ast;
        assertEquals("obj", ((LingoExpr.Identifier) call.object).name);
        assertEquals("handler", call.handlerName);
        assertEquals(3, call.args.size());
    }

    @Test
    void testDeepObjHandlerCallNoArgs() throws ScriptError {
        LingoExpr ast = parse("obj.prop.handler()");
        assertInstanceOf(LingoExpr.ObjHandlerCall.class, ast);
        LingoExpr.ObjHandlerCall call = (LingoExpr.ObjHandlerCall) ast;
        assertEquals("handler", call.handlerName);
        assertEquals(0, call.args.size());

        assertInstanceOf(LingoExpr.ObjProp.class, call.object);
        LingoExpr.ObjProp objProp = (LingoExpr.ObjProp) call.object;
        assertEquals("prop", objProp.propName);
        assertInstanceOf(LingoExpr.Identifier.class, objProp.object);
        assertEquals("obj", ((LingoExpr.Identifier) objProp.object).name);
    }
}
