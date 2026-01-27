package com.dirplayer.player.eval;

import com.dirplayer.player.ScriptError;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Tests for list indexing in Lingo expression parsing.
 * Port of Rust vm-rust/tests/lingo/list_index.rs
 */
public class ListIndexTest {

    private LingoExpr parse(String source) throws ScriptError {
        return LingoParser.parse(source);
    }

    @Test
    void testListIndexLiteral() throws ScriptError {
        LingoExpr ast = parse("[1, 2, 3][1]");
        assertInstanceOf(LingoExpr.ListAccess.class, ast);
        LingoExpr.ListAccess access = (LingoExpr.ListAccess) ast;

        // Check the list
        assertInstanceOf(LingoExpr.ListLiteral.class, access.list);
        LingoExpr.ListLiteral list = (LingoExpr.ListLiteral) access.list;
        assertEquals(3, list.items.size());
        assertEquals(1, ((LingoExpr.IntLiteral) list.items.get(0)).value);
        assertEquals(2, ((LingoExpr.IntLiteral) list.items.get(1)).value);
        assertEquals(3, ((LingoExpr.IntLiteral) list.items.get(2)).value);

        // Check the index
        assertInstanceOf(LingoExpr.IntLiteral.class, access.index);
        assertEquals(1, ((LingoExpr.IntLiteral) access.index).value);
    }

    @Test
    void testListIndexIdentifier() throws ScriptError {
        LingoExpr ast = parse("myList[2]");
        assertInstanceOf(LingoExpr.ListAccess.class, ast);
        LingoExpr.ListAccess access = (LingoExpr.ListAccess) ast;

        assertInstanceOf(LingoExpr.Identifier.class, access.list);
        assertEquals("myList", ((LingoExpr.Identifier) access.list).name);

        assertInstanceOf(LingoExpr.IntLiteral.class, access.index);
        assertEquals(2, ((LingoExpr.IntLiteral) access.index).value);
    }

    @Test
    void testListIndexWithPropertyAccessBefore() throws ScriptError {
        LingoExpr ast = parse("obj.list[1]");
        assertInstanceOf(LingoExpr.ListAccess.class, ast);
        LingoExpr.ListAccess access = (LingoExpr.ListAccess) ast;

        // The list should be obj.list (ObjProp)
        assertInstanceOf(LingoExpr.ObjProp.class, access.list);
        LingoExpr.ObjProp objProp = (LingoExpr.ObjProp) access.list;
        assertEquals("list", objProp.propName);
        assertInstanceOf(LingoExpr.Identifier.class, objProp.object);
        assertEquals("obj", ((LingoExpr.Identifier) objProp.object).name);

        // Index
        assertInstanceOf(LingoExpr.IntLiteral.class, access.index);
        assertEquals(1, ((LingoExpr.IntLiteral) access.index).value);
    }

    @Test
    void testListIndexWithPropertyAccessAfter() throws ScriptError {
        LingoExpr ast = parse("myList[1].prop");
        assertInstanceOf(LingoExpr.ObjProp.class, ast);
        LingoExpr.ObjProp objProp = (LingoExpr.ObjProp) ast;
        assertEquals("prop", objProp.propName);

        assertInstanceOf(LingoExpr.ListAccess.class, objProp.object);
        LingoExpr.ListAccess access = (LingoExpr.ListAccess) objProp.object;
        assertInstanceOf(LingoExpr.Identifier.class, access.list);
        assertEquals("myList", ((LingoExpr.Identifier) access.list).name);
        assertInstanceOf(LingoExpr.IntLiteral.class, access.index);
        assertEquals(1, ((LingoExpr.IntLiteral) access.index).value);
    }

    @Test
    void testSpriteScriptInstanceListIndexProperty() throws ScriptError {
        // The original problem case: sprite(39).scriptInstanceList[2].plabel
        LingoExpr ast = parse("sprite(39).scriptInstanceList[2].plabel");
        assertInstanceOf(LingoExpr.ObjProp.class, ast);
        LingoExpr.ObjProp outer = (LingoExpr.ObjProp) ast;
        assertEquals("plabel", outer.propName);

        assertInstanceOf(LingoExpr.ListAccess.class, outer.object);
        LingoExpr.ListAccess access = (LingoExpr.ListAccess) outer.object;
        assertInstanceOf(LingoExpr.IntLiteral.class, access.index);
        assertEquals(2, ((LingoExpr.IntLiteral) access.index).value);

        assertInstanceOf(LingoExpr.ObjProp.class, access.list);
        LingoExpr.ObjProp scriptInstanceList = (LingoExpr.ObjProp) access.list;
        assertEquals("scriptInstanceList", scriptInstanceList.propName);

        assertInstanceOf(LingoExpr.HandlerCall.class, scriptInstanceList.object);
        LingoExpr.HandlerCall spriteCall = (LingoExpr.HandlerCall) scriptInstanceList.object;
        assertEquals("sprite", spriteCall.handlerName);
        assertEquals(1, spriteCall.args.size());
        assertInstanceOf(LingoExpr.IntLiteral.class, spriteCall.args.get(0));
        assertEquals(39, ((LingoExpr.IntLiteral) spriteCall.args.get(0)).value);
    }

    @Test
    void testListIndexWithExpression() throws ScriptError {
        LingoExpr ast = parse("myList[1 + 1]");
        assertInstanceOf(LingoExpr.ListAccess.class, ast);
        LingoExpr.ListAccess access = (LingoExpr.ListAccess) ast;

        assertInstanceOf(LingoExpr.Identifier.class, access.list);
        assertEquals("myList", ((LingoExpr.Identifier) access.list).name);

        assertInstanceOf(LingoExpr.Add.class, access.index);
        LingoExpr.Add add = (LingoExpr.Add) access.index;
        assertEquals(1, ((LingoExpr.IntLiteral) add.left).value);
        assertEquals(1, ((LingoExpr.IntLiteral) add.right).value);
    }

    @Test
    void testNestedListIndex() throws ScriptError {
        LingoExpr ast = parse("matrix[1][2]");
        assertInstanceOf(LingoExpr.ListAccess.class, ast);
        LingoExpr.ListAccess outer = (LingoExpr.ListAccess) ast;
        assertInstanceOf(LingoExpr.IntLiteral.class, outer.index);
        assertEquals(2, ((LingoExpr.IntLiteral) outer.index).value);

        assertInstanceOf(LingoExpr.ListAccess.class, outer.list);
        LingoExpr.ListAccess inner = (LingoExpr.ListAccess) outer.list;
        assertInstanceOf(LingoExpr.Identifier.class, inner.list);
        assertEquals("matrix", ((LingoExpr.Identifier) inner.list).name);
        assertInstanceOf(LingoExpr.IntLiteral.class, inner.index);
        assertEquals(1, ((LingoExpr.IntLiteral) inner.index).value);
    }

    @Test
    void testListIndexWithHandlerCall() throws ScriptError {
        LingoExpr ast = parse("getList()[1]");
        assertInstanceOf(LingoExpr.ListAccess.class, ast);
        LingoExpr.ListAccess access = (LingoExpr.ListAccess) ast;

        assertInstanceOf(LingoExpr.HandlerCall.class, access.list);
        LingoExpr.HandlerCall call = (LingoExpr.HandlerCall) access.list;
        assertEquals("getList", call.handlerName);
        assertEquals(0, call.args.size());

        assertInstanceOf(LingoExpr.IntLiteral.class, access.index);
        assertEquals(1, ((LingoExpr.IntLiteral) access.index).value);
    }

    @Test
    void testListLiteral() throws ScriptError {
        // Make sure list literals still parse correctly
        LingoExpr ast = parse("[1, 2, 3]");
        assertInstanceOf(LingoExpr.ListLiteral.class, ast);
        LingoExpr.ListLiteral list = (LingoExpr.ListLiteral) ast;
        assertEquals(3, list.items.size());
        assertEquals(1, ((LingoExpr.IntLiteral) list.items.get(0)).value);
        assertEquals(2, ((LingoExpr.IntLiteral) list.items.get(1)).value);
        assertEquals(3, ((LingoExpr.IntLiteral) list.items.get(2)).value);
    }

    @Test
    void testEmptyList() throws ScriptError {
        // Make sure empty lists still parse correctly
        LingoExpr ast = parse("[]");
        assertInstanceOf(LingoExpr.ListLiteral.class, ast);
        assertEquals(0, ((LingoExpr.ListLiteral) ast).items.size());
    }
}
