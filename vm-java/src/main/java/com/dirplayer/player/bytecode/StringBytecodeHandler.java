package com.dirplayer.player.bytecode;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.director.lingo.StringChunkExpr;
import com.dirplayer.director.lingo.StringChunkType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import java.util.ArrayList;
import java.util.List;

/**
 * String bytecode handlers - string operations.
 * Port of Rust StringBytecodeHandler struct.
 */
public class StringBytecodeHandler {

    private static String getDatumConcatValue(Datum datum, DirPlayer player) throws ScriptError {
        if (datum.isString()) {
            return datum.stringValue();
        } else if (datum.isStringChunk()) {
            return datum.stringValue();
        } else if (datum.isInt()) {
            return String.valueOf(datum.intValue());
        } else if (datum.isSymbol()) {
            return datum.stringValue();
        } else if (datum.isVoid()) {
            return "";
        } else {
            return player.formatDatum(datum);
        }
    }

    public static int concatDatums(int leftRef, int rightRef, DirPlayer player, boolean pad) throws ScriptError {
        Datum right = player.getDatum(rightRef);
        Datum left = player.getDatum(leftRef);

        String rightStr = getDatumConcatValue(right, player);
        String leftStr = getDatumConcatValue(left, player);

        String result;
        if (pad) {
            result = leftStr + " " + rightStr;
        } else {
            result = leftStr + rightStr;
        }
        return player.allocDatum(Datum.ofString(result));
    }

    public static HandlerExecutionResult containsStr(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int searchStrRef = scope.stack.pop();
        int searchInRef = scope.stack.pop();

        String searchStr = player.getDatum(searchStrRef).stringValue();
        Datum searchIn = player.getDatum(searchInRef);

        boolean contains;
        if (searchIn.isList()) {
            List<Integer> searchList = searchIn.toList();
            contains = false;
            for (int itemRef : searchList) {
                Datum item = player.getDatum(itemRef);
                if (item.isString()) {
                    String itemStr = item.stringValue();
                    if (itemStr.contains(searchStr)) {
                        contains = true;
                        break;
                    }
                }
            }
        } else if (searchIn.isString()) {
            String searchInStr = searchIn.stringValue();
            contains = searchInStr.toLowerCase().contains(searchStr.toLowerCase());
        } else if (searchIn.isSymbol() || searchIn.isNumber()) {
            contains = false;
        } else {
            throw new ScriptError("kOpContainsStr invalid search subject");
        }

        int resultId = player.allocDatum(CompareBytecodeHandler.datumBool(contains));
        scope.stack.push(resultId);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult joinPadStr(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        int resultId = concatDatums(leftRef, rightRef, player, true);
        scope.stack.push(resultId);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult joinStr(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int rightRef = scope.stack.pop();
        int leftRef = scope.stack.pop();

        Datum left = player.getDatum(leftRef);
        Datum right = player.getDatum(rightRef);

        String leftStr = player.datumToStringForConcat(left);
        String rightStr = player.datumToStringForConcat(right);

        Datum result = Datum.ofString(leftStr + rightStr);
        int resultRef = player.allocDatum(result);

        scope.stack.push(resultRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult put(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        long bytecodeObj = player.getCtxCurrentBytecode(ctx).obj;
        PutType putType = PutType.fromValue((int) ((bytecodeObj >> 4) & 0xF));
        int varType = (int) (bytecodeObj & 0xF);

        ContextVarArgs contextVarArgs = player.readContextVarArgs(varType, ctx.scopeRef);
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueRef = scope.stack.pop();

        switch (putType) {
            case INTO:
                player.playerSetContextVar(
                    contextVarArgs.idRef,
                    contextVarArgs.castIdRef,
                    varType,
                    valueRef,
                    putType,
                    ctx
                );
                break;

            case BEFORE: {
                int currStringId = player.playerGetContextVar(
                    contextVarArgs.idRef,
                    contextVarArgs.castIdRef,
                    varType,
                    ctx
                );
                String currString = player.getDatum(currStringId).stringValue();
                String valueStr = player.getDatum(valueRef).stringValue();

                String newString = valueStr + currString;
                int newStringRef = player.allocDatum(Datum.ofString(newString));
                player.playerSetContextVar(
                    contextVarArgs.idRef,
                    contextVarArgs.castIdRef,
                    varType,
                    newStringRef,
                    putType,
                    ctx
                );
                break;
            }

            case AFTER: {
                int currStringId = player.playerGetContextVar(
                    contextVarArgs.idRef,
                    contextVarArgs.castIdRef,
                    varType,
                    ctx
                );
                String currString = player.getDatum(currStringId).stringValue();
                String valueStr = player.getDatum(valueRef).stringValue();

                String newString = currString + valueStr;
                int newStringRef = player.allocDatum(Datum.ofString(newString));
                player.playerSetContextVar(
                    contextVarArgs.idRef,
                    contextVarArgs.castIdRef,
                    varType,
                    newStringRef,
                    putType,
                    ctx
                );
                break;
            }
        }

        return HandlerExecutionResult.ADVANCE;
    }

    private static StringChunkExpr readSingleChunkRef(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);

        int lastLineRef = scope.stack.pop();
        int firstLineRef = scope.stack.pop();
        int lastItemRef = scope.stack.pop();
        int firstItemRef = scope.stack.pop();
        int lastWordRef = scope.stack.pop();
        int firstWordRef = scope.stack.pop();
        int lastCharRef = scope.stack.pop();
        int firstCharRef = scope.stack.pop();

        int lastLine = player.getDatum(lastLineRef).intValue();
        int firstLine = player.getDatum(firstLineRef).intValue();
        int lastItem = player.getDatum(lastItemRef).intValue();
        int firstItem = player.getDatum(firstItemRef).intValue();
        int lastWord = player.getDatum(lastWordRef).intValue();
        int firstWord = player.getDatum(firstWordRef).intValue();
        int lastChar = player.getDatum(lastCharRef).intValue();
        int firstChar = player.getDatum(firstCharRef).intValue();

        if (firstLine != 0 || lastLine != 0) {
            return new StringChunkExpr(StringChunkType.LINE, firstLine, lastLine, player.movie.itemDelimiter);
        } else if (firstItem != 0 || lastItem != 0) {
            return new StringChunkExpr(StringChunkType.ITEM, firstItem, lastItem, player.movie.itemDelimiter);
        } else if (firstWord != 0 || lastWord != 0) {
            return new StringChunkExpr(StringChunkType.WORD, firstWord, lastWord, player.movie.itemDelimiter);
        } else if (firstChar != 0 || lastChar != 0) {
            return new StringChunkExpr(StringChunkType.CHAR, firstChar, lastChar, player.movie.itemDelimiter);
        } else {
            throw new ScriptError("getChunk: invalid chunk range");
        }
    }

    private static List<StringChunkExpr> readAllChunks(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);

        int lastLineRef = scope.stack.pop();
        int firstLineRef = scope.stack.pop();
        int lastItemRef = scope.stack.pop();
        int firstItemRef = scope.stack.pop();
        int lastWordRef = scope.stack.pop();
        int firstWordRef = scope.stack.pop();
        int lastCharRef = scope.stack.pop();
        int firstCharRef = scope.stack.pop();

        int lastLine = player.getDatum(lastLineRef).intValue();
        int firstLine = player.getDatum(firstLineRef).intValue();
        int lastItem = player.getDatum(lastItemRef).intValue();
        int firstItem = player.getDatum(firstItemRef).intValue();
        int lastWord = player.getDatum(lastWordRef).intValue();
        int firstWord = player.getDatum(firstWordRef).intValue();
        int lastChar = player.getDatum(lastCharRef).intValue();
        int firstChar = player.getDatum(firstCharRef).intValue();

        List<StringChunkExpr> chunks = new ArrayList<>();

        // Add chunks in the order they should be applied
        if (firstLine != 0 || lastLine != 0) {
            chunks.add(new StringChunkExpr(StringChunkType.LINE, firstLine, lastLine, player.movie.itemDelimiter));
        }
        if (firstItem != 0 || lastItem != 0) {
            chunks.add(new StringChunkExpr(StringChunkType.ITEM, firstItem, lastItem, player.movie.itemDelimiter));
        }
        if (firstWord != 0 || lastWord != 0) {
            chunks.add(new StringChunkExpr(StringChunkType.WORD, firstWord, lastWord, player.movie.itemDelimiter));
        }
        if (firstChar != 0 || lastChar != 0) {
            chunks.add(new StringChunkExpr(StringChunkType.CHAR, firstChar, lastChar, player.movie.itemDelimiter));
        }

        if (chunks.isEmpty()) {
            throw new ScriptError("getChunk: no valid chunks specified");
        }

        return chunks;
    }

    public static HandlerExecutionResult getChunk(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int stringRef = scope.stack.pop();

        // Read all chunk parameters
        List<StringChunkExpr> chunks = readAllChunks(player, ctx);

        // Apply chunks sequentially
        String result = player.getDatum(stringRef).stringValue();
        for (StringChunkExpr chunkExpr : chunks) {
            result = player.resolveChunkExprString(result, chunkExpr);
        }

        Datum resultDatum = Datum.ofString(result);
        int resultRef = player.allocDatum(resultDatum);
        scope.stack.push(resultRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult deleteChunk(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        long bytecodeObj = player.getCtxCurrentBytecode(ctx).obj;
        ContextVarArgs contextVarArgs = player.readContextVarArgs((int) bytecodeObj, ctx.scopeRef);

        int stringRef = player.playerGetContextVar(
            contextVarArgs.idRef,
            contextVarArgs.castIdRef,
            (int) bytecodeObj,
            ctx
        );

        StringChunkExpr chunkExpr = readSingleChunkRef(player, ctx);
        player.deleteChunk(stringRef, chunkExpr);

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult contains0Str(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int searchStrRef = scope.stack.pop();
        int searchInRef = scope.stack.pop();

        Datum searchIn = player.getDatum(searchInRef);

        boolean result;
        if (searchIn.isVoid()) {
            result = false;
        } else {
            String searchStr = player.getDatum(searchStrRef).stringValue();
            String searchInStr = searchIn.stringValue();
            result = searchInStr.toLowerCase().startsWith(searchStr.toLowerCase());
        }

        int resultRef = player.allocDatum(CompareBytecodeHandler.datumBool(result));
        scope.stack.push(resultRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult putChunk(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        long bytecodeObj = player.getCtxCurrentBytecode(ctx).obj;
        PutType putType = PutType.fromValue((int) ((bytecodeObj >> 4) & 0xF));
        int varType = (int) (bytecodeObj & 0xF);

        // Read the target variable
        ContextVarArgs contextVarArgs = player.readContextVarArgs(varType, ctx.scopeRef);

        // Pop the value to put from the stack
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueRef = scope.stack.pop();

        // Read the chunk expression from the stack
        StringChunkExpr chunkExpr = readSingleChunkRef(player, ctx);

        // Get the current value of the variable
        int stringRef = player.playerGetContextVar(
            contextVarArgs.idRef,
            contextVarArgs.castIdRef,
            varType,
            ctx
        );

        String currentString = player.getDatum(stringRef).stringValue();
        String valueString = player.getDatum(valueRef).stringValue();

        // Apply the chunk operation based on put type
        String newString;
        switch (putType) {
            case INTO:
                newString = player.stringByPuttingIntoChunk(currentString, chunkExpr, valueString);
                break;
            case BEFORE:
                newString = player.stringByPuttingBeforeChunk(currentString, chunkExpr, valueString);
                break;
            case AFTER:
                newString = player.stringByPuttingAfterChunk(currentString, chunkExpr, valueString);
                break;
            default:
                throw new ScriptError("Unknown put type");
        }

        int newStringRef = player.allocDatum(Datum.ofString(newString));
        player.playerSetContextVar(
            contextVarArgs.idRef,
            contextVarArgs.castIdRef,
            varType,
            newStringRef,
            putType,
            ctx
        );

        return HandlerExecutionResult.ADVANCE;
    }
}

/**
 * Helper class to hold context variable arguments.
 */
class ContextVarArgs {
    public int idRef;
    public Integer castIdRef;

    public ContextVarArgs(int idRef, Integer castIdRef) {
        this.idRef = idRef;
        this.castIdRef = castIdRef;
    }
}
