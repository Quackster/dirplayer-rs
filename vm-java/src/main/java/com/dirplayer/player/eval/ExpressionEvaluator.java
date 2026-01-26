package com.dirplayer.player.eval;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.director.lingo.StringChunkExpr;
import com.dirplayer.director.lingo.StringChunkType;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.handlers.datum.StringChunkHandlers;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates LingoExpr AST nodes to produce Datum values.
 * Port of Rust eval_lingo_expr_ast_runtime function.
 *
 * This class is the runtime evaluator that takes parsed AST nodes
 * and evaluates them in the context of the DirPlayer state.
 */
public class ExpressionEvaluator {

    private final DirPlayer player;

    public ExpressionEvaluator(DirPlayer player) {
        this.player = player;
    }

    /**
     * Parse and evaluate a Lingo expression string.
     */
    public int evalExpression(String source) throws ScriptError {
        LingoExpr expr = LingoParser.parse(source);
        return evaluate(expr);
    }

    /**
     * Parse and evaluate a Lingo command string.
     */
    public int evalCommand(String source) throws ScriptError {
        LingoExpr expr = LingoParser.parseCommand(source);
        return evaluate(expr);
    }

    /**
     * Evaluate an AST node and return a DatumRef (integer ID).
     */
    public int evaluate(LingoExpr expr) throws ScriptError {
        if (expr == null) {
            return 0;  // Void reference
        }

        // ============ Literals ============

        if (expr instanceof LingoExpr.IntLiteral) {
            return player.allocDatum(Datum.ofInt(((LingoExpr.IntLiteral) expr).value));
        }

        if (expr instanceof LingoExpr.FloatLiteral) {
            return player.allocDatum(Datum.ofFloat(((LingoExpr.FloatLiteral) expr).value));
        }

        if (expr instanceof LingoExpr.StringLiteral) {
            return player.allocDatum(Datum.ofString(((LingoExpr.StringLiteral) expr).value));
        }

        if (expr instanceof LingoExpr.SymbolLiteral) {
            return player.allocDatum(Datum.ofSymbol(((LingoExpr.SymbolLiteral) expr).value));
        }

        if (expr instanceof LingoExpr.BoolLiteral) {
            return player.allocDatum(Datum.ofInt(((LingoExpr.BoolLiteral) expr).value ? 1 : 0));
        }

        if (expr instanceof LingoExpr.VoidLiteral) {
            return 0;  // Void reference
        }

        if (expr instanceof LingoExpr.ListLiteral) {
            LingoExpr.ListLiteral list = (LingoExpr.ListLiteral) expr;
            List<Integer> items = new ArrayList<>();
            for (LingoExpr item : list.items) {
                items.add(evaluate(item));
            }
            return player.allocDatum(Datum.ofList(DatumType.List, items, false));
        }

        if (expr instanceof LingoExpr.PropListLiteral) {
            LingoExpr.PropListLiteral propList = (LingoExpr.PropListLiteral) expr;
            List<Datum.PropListPair> pairs = new ArrayList<>();
            for (LingoExpr.PropListEntry entry : propList.entries) {
                int keyRef = evaluate(entry.key);
                int valueRef = evaluate(entry.value);
                pairs.add(new Datum.PropListPair(keyRef, valueRef));
            }
            return player.allocDatum(Datum.ofPropList(pairs, false));
        }

        if (expr instanceof LingoExpr.ColorLiteral) {
            ColorRef color = ((LingoExpr.ColorLiteral) expr).color;
            return player.allocDatum(Datum.ofColorRef(color));
        }

        if (expr instanceof LingoExpr.RectLiteral) {
            LingoExpr.RectLiteral rect = (LingoExpr.RectLiteral) expr;
            int leftRef = evaluate(rect.left);
            int topRef = evaluate(rect.top);
            int rightRef = evaluate(rect.right);
            int bottomRef = evaluate(rect.bottom);
            return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
        }

        if (expr instanceof LingoExpr.PointLiteral) {
            LingoExpr.PointLiteral point = (LingoExpr.PointLiteral) expr;
            int xRef = evaluate(point.x);
            int yRef = evaluate(point.y);
            return player.allocDatum(Datum.ofPoint(xRef, yRef));
        }

        // ============ References ============

        if (expr instanceof LingoExpr.Identifier) {
            String name = ((LingoExpr.Identifier) expr).name;
            return evaluateIdentifier(name);
        }

        if (expr instanceof LingoExpr.MemberRef) {
            LingoExpr.MemberRef memberRef = (LingoExpr.MemberRef) expr;
            int memberNumRef = evaluate(memberRef.memberExpr);
            int memberNum = player.getDatum(memberNumRef).intValue();

            int castLibNum = 1;
            if (memberRef.castLibExpr != null) {
                int castLibRef = evaluate(memberRef.castLibExpr);
                Datum castLibDatum = player.getDatum(castLibRef);
                if (castLibDatum.getType() == DatumType.CastLibRef) {
                    castLibNum = castLibDatum.intValue();
                } else {
                    castLibNum = castLibDatum.intValue();
                }
            }

            return player.allocDatum(Datum.ofCastMember(new CastMemberRef(castLibNum, memberNum)));
        }

        // ============ Binary Operations ============

        if (expr instanceof LingoExpr.Add) {
            LingoExpr.Add add = (LingoExpr.Add) expr;
            int leftRef = evaluate(add.left);
            int rightRef = evaluate(add.right);
            return DatumOperations.add(leftRef, rightRef, player);
        }

        if (expr instanceof LingoExpr.Subtract) {
            LingoExpr.Subtract sub = (LingoExpr.Subtract) expr;
            int leftRef = evaluate(sub.left);
            int rightRef = evaluate(sub.right);
            return DatumOperations.subtract(leftRef, rightRef, player);
        }

        if (expr instanceof LingoExpr.Multiply) {
            LingoExpr.Multiply mul = (LingoExpr.Multiply) expr;
            int leftRef = evaluate(mul.left);
            int rightRef = evaluate(mul.right);
            return DatumOperations.multiply(leftRef, rightRef, player);
        }

        if (expr instanceof LingoExpr.Divide) {
            LingoExpr.Divide div = (LingoExpr.Divide) expr;
            int leftRef = evaluate(div.left);
            int rightRef = evaluate(div.right);
            return DatumOperations.divide(leftRef, rightRef, player);
        }

        if (expr instanceof LingoExpr.Mod) {
            LingoExpr.Mod mod = (LingoExpr.Mod) expr;
            int leftRef = evaluate(mod.left);
            int rightRef = evaluate(mod.right);
            return DatumOperations.mod(leftRef, rightRef, player);
        }

        // ============ String Operations ============

        if (expr instanceof LingoExpr.Join) {
            LingoExpr.Join join = (LingoExpr.Join) expr;
            int leftRef = evaluate(join.left);
            int rightRef = evaluate(join.right);
            return DatumOperations.concat(leftRef, rightRef, player, false);
        }

        if (expr instanceof LingoExpr.JoinPad) {
            LingoExpr.JoinPad joinPad = (LingoExpr.JoinPad) expr;
            int leftRef = evaluate(joinPad.left);
            int rightRef = evaluate(joinPad.right);
            return DatumOperations.concat(leftRef, rightRef, player, true);
        }

        // ============ Comparison Operations ============

        if (expr instanceof LingoExpr.Eq) {
            LingoExpr.Eq eq = (LingoExpr.Eq) expr;
            int leftRef = evaluate(eq.left);
            int rightRef = evaluate(eq.right);
            boolean result = DatumOperations.equals(leftRef, rightRef, player);
            return player.allocDatum(Datum.ofInt(result ? 1 : 0));
        }

        if (expr instanceof LingoExpr.Ne) {
            LingoExpr.Ne ne = (LingoExpr.Ne) expr;
            int leftRef = evaluate(ne.left);
            int rightRef = evaluate(ne.right);
            boolean result = !DatumOperations.equals(leftRef, rightRef, player);
            return player.allocDatum(Datum.ofInt(result ? 1 : 0));
        }

        if (expr instanceof LingoExpr.Lt) {
            LingoExpr.Lt lt = (LingoExpr.Lt) expr;
            int leftRef = evaluate(lt.left);
            int rightRef = evaluate(lt.right);
            boolean result = DatumOperations.lessThan(leftRef, rightRef, player);
            return player.allocDatum(Datum.ofInt(result ? 1 : 0));
        }

        if (expr instanceof LingoExpr.Gt) {
            LingoExpr.Gt gt = (LingoExpr.Gt) expr;
            int leftRef = evaluate(gt.left);
            int rightRef = evaluate(gt.right);
            boolean result = DatumOperations.greaterThan(leftRef, rightRef, player);
            return player.allocDatum(Datum.ofInt(result ? 1 : 0));
        }

        if (expr instanceof LingoExpr.Le) {
            LingoExpr.Le le = (LingoExpr.Le) expr;
            int leftRef = evaluate(le.left);
            int rightRef = evaluate(le.right);
            boolean eq = DatumOperations.equals(leftRef, rightRef, player);
            boolean lt = DatumOperations.lessThan(leftRef, rightRef, player);
            return player.allocDatum(Datum.ofInt((eq || lt) ? 1 : 0));
        }

        if (expr instanceof LingoExpr.Ge) {
            LingoExpr.Ge ge = (LingoExpr.Ge) expr;
            int leftRef = evaluate(ge.left);
            int rightRef = evaluate(ge.right);
            boolean eq = DatumOperations.equals(leftRef, rightRef, player);
            boolean gt = DatumOperations.greaterThan(leftRef, rightRef, player);
            return player.allocDatum(Datum.ofInt((eq || gt) ? 1 : 0));
        }

        // ============ Logical Operations ============

        if (expr instanceof LingoExpr.And) {
            LingoExpr.And and = (LingoExpr.And) expr;
            int leftRef = evaluate(and.left);
            int rightRef = evaluate(and.right);
            boolean leftBool = player.getDatum(leftRef).boolValue();
            boolean rightBool = player.getDatum(rightRef).boolValue();
            return player.allocDatum(Datum.ofInt((leftBool && rightBool) ? 1 : 0));
        }

        if (expr instanceof LingoExpr.Or) {
            LingoExpr.Or or = (LingoExpr.Or) expr;
            int leftRef = evaluate(or.left);
            int rightRef = evaluate(or.right);
            boolean leftBool = player.getDatum(leftRef).boolValue();
            boolean rightBool = player.getDatum(rightRef).boolValue();
            return player.allocDatum(Datum.ofInt((leftBool || rightBool) ? 1 : 0));
        }

        if (expr instanceof LingoExpr.Not) {
            LingoExpr.Not not = (LingoExpr.Not) expr;
            int operandRef = evaluate(not.operand);
            Datum operand = player.getDatum(operandRef);
            boolean boolVal = operand.boolValue();
            return player.allocDatum(Datum.ofInt(boolVal ? 0 : 1));
        }

        // ============ Unary Operations ============

        if (expr instanceof LingoExpr.Negate) {
            LingoExpr.Negate neg = (LingoExpr.Negate) expr;
            int operandRef = evaluate(neg.operand);
            return DatumOperations.negate(operandRef, player);
        }

        // ============ Access Operations ============

        if (expr instanceof LingoExpr.ObjProp) {
            LingoExpr.ObjProp objProp = (LingoExpr.ObjProp) expr;
            int objRef = evaluate(objProp.object);
            return getObjProp(objRef, objProp.propName);
        }

        if (expr instanceof LingoExpr.ListAccess) {
            LingoExpr.ListAccess listAccess = (LingoExpr.ListAccess) expr;
            int listRef = evaluate(listAccess.list);
            int indexRef = evaluate(listAccess.index);
            return evaluateListAccess(listRef, indexRef);
        }

        if (expr instanceof LingoExpr.ThePropOf) {
            LingoExpr.ThePropOf thePropOf = (LingoExpr.ThePropOf) expr;
            int objRef = evaluate(thePropOf.object);
            return getObjProp(objRef, thePropOf.propName);
        }

        // ============ Call Operations ============

        if (expr instanceof LingoExpr.HandlerCall) {
            LingoExpr.HandlerCall call = (LingoExpr.HandlerCall) expr;
            List<Integer> argRefs = new ArrayList<>();
            for (LingoExpr arg : call.args) {
                argRefs.add(evaluate(arg));
            }
            return callHandler(call.handlerName, argRefs);
        }

        if (expr instanceof LingoExpr.ObjHandlerCall) {
            LingoExpr.ObjHandlerCall call = (LingoExpr.ObjHandlerCall) expr;
            int objRef = evaluate(call.object);
            List<Integer> argRefs = new ArrayList<>();
            for (LingoExpr arg : call.args) {
                argRefs.add(evaluate(arg));
            }
            return callObjHandler(objRef, call.handlerName, argRefs);
        }

        // ============ Assignment Operations ============

        if (expr instanceof LingoExpr.Assignment) {
            LingoExpr.Assignment assign = (LingoExpr.Assignment) expr;
            int valueRef = evaluate(assign.value);
            return evaluateAssignment(assign.target, valueRef);
        }

        // ============ Put Operations ============

        if (expr instanceof LingoExpr.PutInto) {
            LingoExpr.PutInto put = (LingoExpr.PutInto) expr;
            int valueRef = evaluate(put.value);
            return evaluatePutInto(put.target, valueRef);
        }

        if (expr instanceof LingoExpr.PutBefore) {
            LingoExpr.PutBefore put = (LingoExpr.PutBefore) expr;
            int valueRef = evaluate(put.value);
            return evaluatePutBefore(put.target, valueRef);
        }

        if (expr instanceof LingoExpr.PutAfter) {
            LingoExpr.PutAfter put = (LingoExpr.PutAfter) expr;
            int valueRef = evaluate(put.value);
            return evaluatePutAfter(put.target, valueRef);
        }

        if (expr instanceof LingoExpr.PutDisplay) {
            LingoExpr.PutDisplay put = (LingoExpr.PutDisplay) expr;
            int valueRef = evaluate(put.value);
            // Display value (log or message window)
            String displayStr = player.formatDatum(player.getDatum(valueRef));
            System.out.println("-- " + displayStr);
            return 0;  // Void
        }

        // ============ Chunk Operations ============

        if (expr instanceof LingoExpr.ChunkExpr) {
            LingoExpr.ChunkExpr chunk = (LingoExpr.ChunkExpr) expr;
            return evaluateChunkExpr(chunk);
        }

        throw new ScriptError("Unknown expression type: " + expr.getExprType());
    }

    // ============ Helper Methods ============

    private int evaluateIdentifier(String name) throws ScriptError {
        // Handle "the X" properties
        if (name.startsWith("the ")) {
            String propName = name.substring(4);
            return getMovieProp(propName);
        }

        // Check globals
        if (player.globals.containsKey(name)) {
            return player.globals.get(name);
        }

        // Try top-level properties
        return getTopLevelProp(name);
    }

    private int evaluateListAccess(int listRef, int indexRef) throws ScriptError {
        Datum list = player.getDatum(listRef);
        int index = player.getDatum(indexRef).intValue();

        if (list.isList()) {
            List<Integer> items = list.toList();
            // Lingo uses 1-based indexing
            if (index < 1 || index > items.size()) {
                throw new ScriptError("List index " + index + " out of bounds (list has " + items.size() + " items)");
            }
            return items.get(index - 1);
        }

        throw new ScriptError("Cannot index non-list type: " + list.getTypeName());
    }

    private int evaluateAssignment(LingoExpr target, int valueRef) throws ScriptError {
        if (target instanceof LingoExpr.Identifier) {
            String name = ((LingoExpr.Identifier) target).name;
            if (name.startsWith("the ")) {
                String propName = name.substring(4);
                setMovieProp(propName, valueRef);
            } else {
                player.globals.put(name, valueRef);
            }
            return valueRef;
        }

        if (target instanceof LingoExpr.ObjProp) {
            LingoExpr.ObjProp objProp = (LingoExpr.ObjProp) target;
            int objRef = evaluate(objProp.object);
            setObjProp(objRef, objProp.propName, valueRef);
            return valueRef;
        }

        if (target instanceof LingoExpr.ThePropOf) {
            LingoExpr.ThePropOf thePropOf = (LingoExpr.ThePropOf) target;
            int objRef = evaluate(thePropOf.object);
            setObjProp(objRef, thePropOf.propName, valueRef);
            return valueRef;
        }

        throw new ScriptError("Invalid assignment target: " + target.getExprType());
    }

    private int evaluatePutInto(LingoExpr target, int valueRef) throws ScriptError {
        if (target instanceof LingoExpr.Identifier) {
            String name = ((LingoExpr.Identifier) target).name;
            player.globals.put(name, valueRef);
            return 0;  // Void
        }

        if (target instanceof LingoExpr.ChunkExpr) {
            return evaluatePutIntoChunk((LingoExpr.ChunkExpr) target, valueRef);
        }

        throw new ScriptError("Invalid put into target: " + target.getExprType());
    }

    private int evaluatePutBefore(LingoExpr target, int valueRef) throws ScriptError {
        if (target instanceof LingoExpr.Identifier) {
            String name = ((LingoExpr.Identifier) target).name;
            int currentRef = player.globals.getOrDefault(name, player.allocDatum(Datum.ofString("")));
            String currentStr = player.datumToStringForConcat(player.getDatum(currentRef));
            String valueStr = player.datumToStringForConcat(player.getDatum(valueRef));
            int newRef = player.allocDatum(Datum.ofString(valueStr + currentStr));
            player.globals.put(name, newRef);
            return 0;  // Void
        }

        if (target instanceof LingoExpr.ChunkExpr) {
            return evaluatePutBeforeChunk((LingoExpr.ChunkExpr) target, valueRef);
        }

        throw new ScriptError("Invalid put before target: " + target.getExprType());
    }

    private int evaluatePutAfter(LingoExpr target, int valueRef) throws ScriptError {
        if (target instanceof LingoExpr.Identifier) {
            String name = ((LingoExpr.Identifier) target).name;
            int currentRef = player.globals.getOrDefault(name, player.allocDatum(Datum.ofString("")));
            String currentStr = player.datumToStringForConcat(player.getDatum(currentRef));
            String valueStr = player.datumToStringForConcat(player.getDatum(valueRef));
            int newRef = player.allocDatum(Datum.ofString(currentStr + valueStr));
            player.globals.put(name, newRef);
            return 0;  // Void
        }

        if (target instanceof LingoExpr.ChunkExpr) {
            return evaluatePutAfterChunk((LingoExpr.ChunkExpr) target, valueRef);
        }

        throw new ScriptError("Invalid put after target: " + target.getExprType());
    }

    private int evaluateChunkExpr(LingoExpr.ChunkExpr chunk) throws ScriptError {
        int sourceRef = evaluate(chunk.source);
        String sourceStr = player.getDatum(sourceRef).stringValue();

        int startIndex = player.getDatum(evaluate(chunk.startIndex)).intValue();
        int endIndex = chunk.endIndex != null ?
            player.getDatum(evaluate(chunk.endIndex)).intValue() : startIndex;

        StringChunkType chunkType = StringChunkType.fromString(chunk.chunkType);
        StringChunkExpr chunkExpr = new StringChunkExpr(chunkType, startIndex, endIndex, player.movie.itemDelimiter);

        String result = StringChunkHandlers.resolveChunkExprString(sourceStr, chunkExpr);
        return player.allocDatum(Datum.ofString(result));
    }

    private int evaluatePutIntoChunk(LingoExpr.ChunkExpr chunk, int valueRef) throws ScriptError {
        String varName = getChunkSourceVarName(chunk.source);
        int sourceRef = player.globals.getOrDefault(varName, player.allocDatum(Datum.ofString("")));
        String sourceStr = player.getDatum(sourceRef).stringValue();
        String valueStr = player.datumToStringForConcat(player.getDatum(valueRef));

        int startIndex = player.getDatum(evaluate(chunk.startIndex)).intValue();
        int endIndex = chunk.endIndex != null ?
            player.getDatum(evaluate(chunk.endIndex)).intValue() : startIndex;

        StringChunkType chunkType = StringChunkType.fromString(chunk.chunkType);
        StringChunkExpr chunkExpr = new StringChunkExpr(chunkType, startIndex, endIndex, player.movie.itemDelimiter);

        String newStr = StringChunkHandlers.stringByPuttingIntoChunk(sourceStr, chunkExpr, valueStr);
        int newRef = player.allocDatum(Datum.ofString(newStr));
        player.globals.put(varName, newRef);
        return 0;  // Void
    }

    private int evaluatePutBeforeChunk(LingoExpr.ChunkExpr chunk, int valueRef) throws ScriptError {
        String varName = getChunkSourceVarName(chunk.source);
        int sourceRef = player.globals.getOrDefault(varName, player.allocDatum(Datum.ofString("")));
        String sourceStr = player.getDatum(sourceRef).stringValue();
        String valueStr = player.datumToStringForConcat(player.getDatum(valueRef));

        int startIndex = player.getDatum(evaluate(chunk.startIndex)).intValue();
        int endIndex = chunk.endIndex != null ?
            player.getDatum(evaluate(chunk.endIndex)).intValue() : startIndex;

        StringChunkType chunkType = StringChunkType.fromString(chunk.chunkType);
        StringChunkExpr chunkExpr = new StringChunkExpr(chunkType, startIndex, endIndex, player.movie.itemDelimiter);

        String newStr = StringChunkHandlers.stringByPuttingBeforeChunk(sourceStr, chunkExpr, valueStr);
        int newRef = player.allocDatum(Datum.ofString(newStr));
        player.globals.put(varName, newRef);
        return 0;  // Void
    }

    private int evaluatePutAfterChunk(LingoExpr.ChunkExpr chunk, int valueRef) throws ScriptError {
        String varName = getChunkSourceVarName(chunk.source);
        int sourceRef = player.globals.getOrDefault(varName, player.allocDatum(Datum.ofString("")));
        String sourceStr = player.getDatum(sourceRef).stringValue();
        String valueStr = player.datumToStringForConcat(player.getDatum(valueRef));

        int startIndex = player.getDatum(evaluate(chunk.startIndex)).intValue();
        int endIndex = chunk.endIndex != null ?
            player.getDatum(evaluate(chunk.endIndex)).intValue() : startIndex;

        StringChunkType chunkType = StringChunkType.fromString(chunk.chunkType);
        StringChunkExpr chunkExpr = new StringChunkExpr(chunkType, startIndex, endIndex, player.movie.itemDelimiter);

        String newStr = StringChunkHandlers.stringByPuttingAfterChunk(sourceStr, chunkExpr, valueStr);
        int newRef = player.allocDatum(Datum.ofString(newStr));
        player.globals.put(varName, newRef);
        return 0;  // Void
    }

    private String getChunkSourceVarName(LingoExpr source) throws ScriptError {
        if (source instanceof LingoExpr.Identifier) {
            return ((LingoExpr.Identifier) source).name;
        }
        throw new ScriptError("Expected identifier as chunk source");
    }

    // ============ Property Access Methods ============
    // These interact with player state for property get/set

    private int getMovieProp(String propName) throws ScriptError {
        // Movie properties are accessed through the DirPlayer
        return player.getMovieProp(propName);
    }

    private void setMovieProp(String propName, int valueRef) throws ScriptError {
        // Movie property setting through DirPlayer
        Datum value = player.getDatum(valueRef);
        player.setMovieProp(propName, value);
    }

    private int getTopLevelProp(String propName) throws ScriptError {
        // Check global variables first
        Datum global = player.getGlobal(propName);
        if (global != null) {
            return player.globals.get(propName);
        }
        // Return void for undefined variables
        return 0;
    }

    private int getObjProp(int objRef, String propName) throws ScriptError {
        // Get property from a datum object
        Datum datum = player.getDatum(objRef);
        if (datum == null || datum.isVoid()) {
            throw new ScriptError("Cannot get property of void");
        }
        return com.dirplayer.player.bytecode.GetSetBytecodeHandler.getObjPropInternal(
            player, objRef, propName);
    }

    private void setObjProp(int objRef, String propName, int valueRef) throws ScriptError {
        // Set property on a datum object
        Datum datum = player.getDatum(objRef);
        if (datum == null || datum.isVoid()) {
            throw new ScriptError("Cannot set property of void");
        }
        com.dirplayer.player.bytecode.GetSetBytecodeHandler.playerSetObjProp(
            player, objRef, propName, valueRef);
    }

    private int callHandler(String handlerName, List<Integer> argRefs) throws ScriptError {
        // Call a global handler through the handler manager
        return com.dirplayer.player.handlers.HandlerManager.callHandler(player, handlerName, argRefs);
    }

    private int callObjHandler(int objRef, String handlerName, List<Integer> argRefs) throws ScriptError {
        // Call a handler on a datum object
        Datum datum = player.getDatum(objRef);
        if (datum == null || datum.isVoid()) {
            throw new ScriptError("Cannot call handler on void");
        }

        // Route to appropriate datum handler based on type
        DatumType type = datum.getType();
        switch (type) {
            case List:
            case ArgList:
            case ArgListNoRet:
                return com.dirplayer.player.handlers.datum.ListHandlers.call(player, objRef, handlerName, argRefs);
            case PropList:
                return com.dirplayer.player.handlers.datum.PropListHandlers.call(player, objRef, handlerName, argRefs);
            case String:
                return com.dirplayer.player.handlers.datum.StringDatumHandlers.call(player, objRef, handlerName, argRefs);
            case ScriptInstanceRef:
                return com.dirplayer.player.handlers.datum.ScriptInstanceHandlers.call(player, objRef, handlerName, argRefs);
            case ScriptRef:
                return com.dirplayer.player.handlers.datum.ScriptHandlers.call(player, objRef, handlerName, argRefs);
            case CastMemberRef:
                return com.dirplayer.player.handlers.datum.CastMemberRefHandlers.call(player, objRef, handlerName, argRefs);
            case SpriteRef:
                return com.dirplayer.player.handlers.datum.SpriteHandlers.call(player, objRef, handlerName, argRefs);
            default:
                throw new ScriptError("Cannot call handler '" + handlerName + "' on " + type);
        }
    }
}
