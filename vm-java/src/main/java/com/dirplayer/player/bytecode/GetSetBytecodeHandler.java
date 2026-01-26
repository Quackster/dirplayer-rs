package com.dirplayer.player.bytecode;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.director.lingo.LingoConstants;
import com.dirplayer.director.lingo.StringChunkType;
import com.dirplayer.director.lingo.StringChunkExpr;
import com.dirplayer.director.chunks.HandlerDef;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import com.dirplayer.player.handlers.datum.SoundChannelHandlers;
import com.dirplayer.player.handlers.datum.XmlHandlers;
import com.dirplayer.player.score.SpritePropertyUtils;
import com.dirplayer.player.script.ScriptInstance;
import com.dirplayer.player.script.ScriptInstanceRef;
import com.dirplayer.player.script.ScriptUtils;

import java.util.List;

/**
 * Get/set bytecode handlers - variable and property access operations.
 * Port of Rust GetSetBytecodeHandler struct.
 */
public class GetSetBytecodeHandler {

    /**
     * Get a built-in "the" property.
     */
    public static int getTheBuiltInProp(DirPlayer player, BytecodeHandlerContext ctx, String propName) throws ScriptError {
        switch (propName) {
            case "paramCount":
                ScriptScope scope = player.scopes.get(ctx.scopeRef);
                return player.allocDatum(Datum.ofInt(scope.args != null ? scope.args.size() : 0));
            case "result":
                return player.lastHandlerResult;
            default:
                return player.getMovieProp(propName);
        }
    }

    /**
     * Set a built-in "the" property.
     */
    public static void setTheBuiltInProp(DirPlayer player, BytecodeHandlerContext ctx, String propName, Datum value) throws ScriptError {
        player.setMovieProp(propName, value);
    }

    /**
     * Get a top-level property like _player or _movie.
     */
    public static Datum getTopLevelPropValue(DirPlayer player, String propName) throws ScriptError {
        switch (propName) {
            case "_player":
                return new Datum(DatumType.PlayerRef);
            case "_movie":
                return new Datum(DatumType.MovieRef);
            default:
                throw new ScriptError("Invalid top level prop: " + propName);
        }
    }

    public static HandlerExecutionResult getProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        ScriptInstanceRef receiver = scope.receiver;
        CastMemberRef scriptRef = scope.scriptMemberRef;

        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        int result;
        if (receiver != null) {
            result = ScriptUtils.scriptGetProp(player, receiver, propName);
        } else {
            result = ScriptUtils.scriptGetStaticProp(player, scriptRef, propName);
        }

        scope.stack.push(result);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult setProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueRef = scope.stack.pop();
        ScriptInstanceRef receiver = scope.receiver;
        CastMemberRef scriptRef = scope.scriptMemberRef;

        if (receiver != null) {
            if (receiver.instanceId == 0) {
                throw new ScriptError("Can't set prop " + propName + " of Void");
            }
            ScriptUtils.scriptSetProp(player, receiver, propName, valueRef, false);
        } else {
            ScriptUtils.scriptSetStaticProp(player, scriptRef, propName, valueRef, true);
        }

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult setObjProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueRef = scope.stack.pop();
        int objDatumRef = scope.stack.pop();
        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        playerSetObjProp(player, objDatumRef, propName, valueRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult getObjProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int objDatumRef = scope.stack.pop();

        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        Datum objDatum = player.getDatum(objDatumRef);

        // Handle XML refs specially
        if (objDatum.getType() == DatumType.XmlRef) {
            int resultRef = XmlHandlers.getProp(player, objDatumRef, propName);
            scope.stack.push(resultRef);
            return HandlerExecutionResult.ADVANCE;
        }

        int resultRef = getObjPropInternal(player, objDatumRef, propName);
        scope.stack.push(resultRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult getMovieProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);
        int resultRef = player.getMovieProp(propName);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.push(resultRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult set(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int propertyIdRef = scope.stack.pop();
        int propertyId = player.getDatum(propertyIdRef).intValue();
        int valueRef = scope.stack.pop();
        Datum value = player.getDatum(valueRef).clone();

        long propertyType = player.getCtxCurrentBytecode(ctx).obj;

        switch ((int) propertyType) {
            case 0x00:
                if (propertyId <= 0x0b) {
                    // Movie prop
                    String propName = LingoConstants.getMoviePropName(propertyId);
                    setTheBuiltInProp(player, ctx, propName, value);
                    return HandlerExecutionResult.ADVANCE;
                } else {
                    throw new ScriptError("Invalid propertyType/propertyID for kOpSet: " + propertyType);
                }

            case 0x04: {
                // Sound channel properties
                String propName = LingoConstants.getSoundPropName(propertyId);
                int channelNumRef = scope.stack.pop();
                int channelNum = player.getDatum(channelNumRef).intValue();
                int soundChannelDatum = player.allocDatum(Datum.ofSoundChannel(channelNum));
                SoundChannelHandlers.setProp(player, soundChannelDatum, propName, valueRef);
                return HandlerExecutionResult.ADVANCE;
            }

            case 0x06: {
                // Sprite properties
                String propName = LingoConstants.getSpritePropName(propertyId);
                int spriteRef = scope.stack.pop();
                int spriteNum = player.getDatum(spriteRef).intValue();
                SpritePropertyUtils.spriteSetProp(player, spriteNum, propName, value);
                return HandlerExecutionResult.ADVANCE;
            }

            case 0x07: {
                // Animation properties
                String propName = LingoConstants.getAnimPropName(propertyId);
                player.setMovieProp(propName, value);
                return HandlerExecutionResult.ADVANCE;
            }

            default:
                throw new ScriptError("Invalid propertyType/propertyID for kOpSet: " + propertyType);
        }
    }

    public static HandlerExecutionResult getGlobal(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);
        Integer valueRef = player.globals.get(propName);
        if (valueRef == null) {
            valueRef = 0; // Void
        }

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.push(valueRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult setGlobal(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueRef = scope.stack.pop();
        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);
        player.globals.put(propName, valueRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult getField(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);

        Integer castIdRef = null;
        if (player.movie.dirVersion >= 500) {
            castIdRef = scope.stack.pop();
        }

        int fieldNameOrNumRef = scope.stack.pop();

        Datum castId = castIdRef != null ? player.getDatum(castIdRef) : Datum.ofInt(0);
        Datum fieldNameOrNum = player.getDatum(fieldNameOrNumRef);

        String fieldValue = player.movie.castManager.getFieldValueByIdentifiers(
            fieldNameOrNum, castId
        );

        int resultId = player.allocDatum(Datum.ofString(fieldValue));
        scope.stack.push(resultId);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult getLocal(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        int variableMultiplier = player.movie.dirVersion >= 500 ? 8 : 6;
        int nameInt = (int) (player.getCtxCurrentBytecode(ctx).obj / variableMultiplier);
        HandlerDef handler = player.getCurrentHandlerDef(ctx);
        int nameId = handler.localNameIds.get(nameInt);

        String varName = player.getName(ctx, nameId);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        Integer valueRef = scope.locals.get(varName);
        if (valueRef == null) {
            valueRef = 0; // Void
        }

        scope.stack.push(valueRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult setLocal(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        int variableMultiplier = player.movie.dirVersion >= 500 ? 8 : 6;
        int nameInt = (int) (player.getCtxCurrentBytecode(ctx).obj / variableMultiplier);
        HandlerDef handler = player.getCurrentHandlerDef(ctx);
        int nameId = handler.localNameIds.get(nameInt);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueRef = scope.stack.pop();

        String varName = player.getName(ctx, nameId);
        scope.locals.put(varName, valueRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult getParam(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        int variableMultiplier = player.movie.dirVersion >= 500 ? 8 : 6;
        int paramNumber = (int) (player.getCtxCurrentBytecode(ctx).obj / variableMultiplier);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int result = 0; // Void
        if (scope.args != null && paramNumber < scope.args.size()) {
            result = scope.args.get(paramNumber);
        }

        scope.stack.push(result);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult setParam(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        int variableMultiplier = player.movie.dirVersion >= 500 ? 8 : 6;
        int bytecodeObj = (int) (player.getCtxCurrentBytecode(ctx).obj / variableMultiplier);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueRef = scope.stack.pop();

        if (scope.args != null && bytecodeObj < scope.args.size()) {
            scope.args.set(bytecodeObj, valueRef);
        } else {
            // Extend args list if needed
            while (scope.args.size() <= bytecodeObj) {
                scope.args.add(0); // Void
            }
            scope.args.set(bytecodeObj, valueRef);
        }

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult setMovieProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueRef = scope.stack.pop();
        Datum value = player.getDatum(valueRef).clone();

        player.setMovieProp(propName, value);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult theBuiltIn(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);
        int resultId = getTheBuiltInProp(player, ctx, propName);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.pop(); // Empty arglist
        scope.stack.push(resultId);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult getChainedProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int objRef = scope.stack.isEmpty() ? 0 : scope.stack.pop();

        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        Datum objDatum = player.getDatum(objRef);
        DatumType objType = objDatum.getType();

        // Check if prop_name is a numeric index
        boolean isNumericIndex = false;
        int numericIndexValue = 0;
        try {
            numericIndexValue = Integer.parseInt(propName);
            isNumericIndex = true;
        } catch (NumberFormatException e) {
            // Not numeric
        }

        int resultRef;

        switch (objType) {
            case SpriteRef: {
                int spriteNum = objDatum.toSpriteRef();
                Datum datum = SpritePropertyUtils.spriteGetProp(player, spriteNum, propName);
                resultRef = player.allocDatum(datum);
                break;
            }

            case XmlRef:
                resultRef = XmlHandlers.getProp(player, objRef, propName);
                break;

            case String:
                if (propName.equals("length")) {
                    int len = objDatum.stringValue().length();
                    resultRef = player.allocDatum(Datum.ofInt(len));
                } else if (propName.equals("char")) {
                    String s = objDatum.stringValue();
                    StringChunkExpr chunkExpr = new StringChunkExpr(
                        StringChunkType.CHAR,
                        1,
                        s.length(),
                        player.movie.itemDelimiter
                    );
                    resultRef = player.allocDatum(Datum.ofStringChunk(
                        objRef,
                        chunkExpr,
                        s
                    ));
                } else {
                    resultRef = getObjPropInternal(player, objRef, propName);
                }
                break;

            case List:
                if (isNumericIndex) {
                    List<Integer> list = objDatum.toList();
                    // Lingo uses 1-based indexing
                    int zeroBasedIndex = numericIndexValue - 1;
                    if (zeroBasedIndex >= 0 && zeroBasedIndex < list.size()) {
                        resultRef = list.get(zeroBasedIndex);
                    } else {
                        throw new ScriptError("List index " + numericIndexValue + " out of bounds (list has " + list.size() + " items)");
                    }
                } else {
                    resultRef = getObjPropInternal(player, objRef, propName);
                }
                break;

            case PropList:
                resultRef = getObjPropInternal(player, objRef, propName);
                break;

            case ScriptInstanceRef:
                if (isNumericIndex) {
                    throw new ScriptError("Cannot use numeric index '" + propName + "' on script instance");
                } else {
                    resultRef = getObjPropInternal(player, objRef, propName);
                }
                break;

            default:
                resultRef = getObjPropInternal(player, objRef, propName);
                break;
        }

        scope.stack.push(resultRef);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult get(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int propIdRef = scope.stack.pop();
        int propId = player.getDatum(propIdRef).intValue();
        long propType = player.getCtxCurrentBytecode(ctx).obj;
        int maxMoviePropId = 0x0b;

        if (propType == 0 && propId <= maxMoviePropId) {
            // Movie prop
            String propName = LingoConstants.getMoviePropName(propId);
            int result = getTheBuiltInProp(player, ctx, propName);
            scope.stack.push(result);
            return HandlerExecutionResult.ADVANCE;
        } else if (propType == 0) {
            // Last chunk
            int stringIdRef = scope.stack.pop();
            String string = player.getDatum(stringIdRef).stringValue();
            StringChunkType chunkType = StringChunkType.fromId(propId - 0x0b);
            String lastChunk = resolveLastChunk(player, string, chunkType);
            int result = player.allocDatum(Datum.ofString(lastChunk));
            scope.stack.push(result);
            return HandlerExecutionResult.ADVANCE;
        } else if (propType == 0x06) {
            // Sprite prop
            String propName = LingoConstants.getSpritePropName(propId);
            int datumRef = scope.stack.pop();
            int spriteNum = player.getDatum(datumRef).intValue();
            Datum resultDatum = SpritePropertyUtils.spriteGetProp(player, spriteNum, propName);
            int result = player.allocDatum(resultDatum);
            scope.stack.push(result);
            return HandlerExecutionResult.ADVANCE;
        } else if (propType == 0x07) {
            // Anim prop
            Datum datum = player.getAnimProp(propId);
            int result = player.allocDatum(datum);
            scope.stack.push(result);
            return HandlerExecutionResult.ADVANCE;
        } else if (propType == 0x08) {
            // Anim2 prop
            Datum datum;
            if (propId == 0x02 && player.movie.dirVersion >= 500) {
                // the number of castMembers supports castLib selection
                int castLibIdRef = scope.stack.pop();
                Datum castLibId = player.getDatum(castLibIdRef);
                boolean bypassCastLibSelection = castLibId.isInt() && castLibId.intValue() == 0;
                if (bypassCastLibSelection) {
                    datum = player.getAnim2Prop(propId);
                } else {
                    datum = player.getCastMemberCount(castLibId);
                }
            } else {
                datum = player.getAnim2Prop(propId);
            }
            int result = player.allocDatum(datum);
            scope.stack.push(result);
            return HandlerExecutionResult.ADVANCE;
        } else if (propType == 0x09) {
            // Anim prop (alternate)
            Datum datum = player.getAnimProp(propId);
            int result = player.allocDatum(datum);
            scope.stack.push(result);
            return HandlerExecutionResult.ADVANCE;
        } else if (propType == 0x0b) {
            // Sound properties
            if (propId == 2) {
                // Number of sounds
                scope.stack.pop();
                int result = player.allocDatum(Datum.ofInt(player.soundManager.getNumChannels()));
                scope.stack.push(result);
                return HandlerExecutionResult.ADVANCE;
            } else {
                String propName = LingoConstants.getSoundPropName(propId);
                int datumRef = scope.stack.pop();
                int channelNum = player.getDatum(datumRef).intValue();

                if (channelNum == 0) {
                    throw new ScriptError("Sound channel index must be >= 1 for property '" + propName + "'");
                }

                int soundChannelDatum = player.allocDatum(Datum.ofSoundChannel(channelNum));
                int result = SoundChannelHandlers.getProp(player, soundChannelDatum, propName);
                scope.stack.push(result);
                return HandlerExecutionResult.ADVANCE;
            }
        } else if (propType == 0x01) {
            // Number of chunks
            int stringIdRef = scope.stack.pop();
            String string = player.getDatum(stringIdRef).stringValue();
            StringChunkType chunkType = StringChunkType.fromId(propId);
            List<String> chunks = resolveChunkList(player, string, chunkType);
            int result = player.allocDatum(Datum.ofInt(chunks.size()));
            scope.stack.push(result);
            return HandlerExecutionResult.ADVANCE;
        } else {
            throw new ScriptError("OpCode.kOpGet call not implemented propertyID=" + propId + " propertyType=" + propType);
        }
    }

    public static HandlerExecutionResult getTopLevelProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);
        Datum result = getTopLevelPropValue(player, propName);
        int resultId = player.allocDatum(result);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        scope.stack.push(resultId);
        return HandlerExecutionResult.ADVANCE;
    }

    // Helper method: Get object property
    public static int getObjPropInternal(DirPlayer player, int objDatumRef, String propName) throws ScriptError {
        Datum objDatum = player.getDatum(objDatumRef);
        DatumType objType = objDatum.getType();

        switch (objType) {
            case ScriptInstanceRef: {
                ScriptInstanceRef instanceRef = objDatum.toScriptInstanceRef();
                return ScriptUtils.scriptGetProp(player, instanceRef, propName);
            }

            case SpriteRef: {
                int spriteNum = objDatum.toSpriteRef();
                Datum datum = SpritePropertyUtils.spriteGetProp(player, spriteNum, propName);
                return player.allocDatum(datum);
            }

            case PropList: {
                List<Datum.PropListPair> pairs = objDatum.toMap();
                for (Datum.PropListPair pair : pairs) {
                    Datum keyDatum = player.getDatum(pair.key);
                    if (keyDatum.isSymbol() && keyDatum.symbolValue().equalsIgnoreCase(propName)) {
                        return pair.value;
                    } else if (keyDatum.isString() && keyDatum.stringValue().equalsIgnoreCase(propName)) {
                        return pair.value;
                    }
                }
                throw new ScriptError("Property '" + propName + "' not found in proplist");
            }

            case List: {
                // Try to parse propName as a number for indexed access
                try {
                    int index = Integer.parseInt(propName);
                    List<Integer> list = objDatum.toList();
                    // Lingo is 1-based
                    int zeroBasedIndex = index - 1;
                    if (zeroBasedIndex >= 0 && zeroBasedIndex < list.size()) {
                        return list.get(zeroBasedIndex);
                    }
                    throw new ScriptError("Index " + index + " out of bounds for list");
                } catch (NumberFormatException e) {
                    throw new ScriptError("Cannot get property '" + propName + "' from list");
                }
            }

            case Point:
                switch (propName.toLowerCase()) {
                    case "loch":
                    case "x":
                        return objDatum.toPoint()[0];
                    case "locv":
                    case "y":
                        return objDatum.toPoint()[1];
                    default:
                        throw new ScriptError("Unknown point property: " + propName);
                }

            case Rect:
                switch (propName.toLowerCase()) {
                    case "left":
                        return objDatum.toRect()[0];
                    case "top":
                        return objDatum.toRect()[1];
                    case "right":
                        return objDatum.toRect()[2];
                    case "bottom":
                        return objDatum.toRect()[3];
                    case "width":
                        return player.allocDatum(Datum.ofInt(
                            player.getDatum(objDatum.toRect()[2]).intValue() -
                            player.getDatum(objDatum.toRect()[0]).intValue()
                        ));
                    case "height":
                        return player.allocDatum(Datum.ofInt(
                            player.getDatum(objDatum.toRect()[3]).intValue() -
                            player.getDatum(objDatum.toRect()[1]).intValue()
                        ));
                    default:
                        throw new ScriptError("Unknown rect property: " + propName);
                }

            case ColorRef:
                switch (propName.toLowerCase()) {
                    case "red":
                        return player.allocDatum(Datum.ofInt(objDatum.toColorRef().getRed()));
                    case "green":
                        return player.allocDatum(Datum.ofInt(objDatum.toColorRef().getGreen()));
                    case "blue":
                        return player.allocDatum(Datum.ofInt(objDatum.toColorRef().getBlue()));
                    default:
                        throw new ScriptError("Unknown color property: " + propName);
                }

            case CastMemberRef:
                return player.getMemberProp(objDatum.toCastMemberRef(), propName);

            case DateRef:
                return player.getDateProp(objDatumRef, propName);

            case Vector:
                switch (propName.toLowerCase()) {
                    case "x":
                        return player.allocDatum(Datum.ofFloat(objDatum.toVector()[0]));
                    case "y":
                        return player.allocDatum(Datum.ofFloat(objDatum.toVector()[1]));
                    case "z":
                        return player.allocDatum(Datum.ofFloat(objDatum.toVector()[2]));
                    default:
                        throw new ScriptError("Unknown vector property: " + propName);
                }

            case XmlRef:
                return XmlHandlers.getProp(player, objDatumRef, propName);

            default:
                throw new ScriptError("Cannot get property '" + propName + "' from " + objType);
        }
    }

    // Helper method: Set object property
    public static void playerSetObjProp(DirPlayer player, int objDatumRef, String propName, int valueRef) throws ScriptError {
        Datum objDatum = player.getDatum(objDatumRef);
        Datum value = player.getDatum(valueRef);
        DatumType objType = objDatum.getType();

        switch (objType) {
            case ScriptInstanceRef: {
                ScriptInstanceRef instanceRef = objDatum.toScriptInstanceRef();
                ScriptUtils.scriptSetProp(player, instanceRef, propName, valueRef, false);
                break;
            }

            case SpriteRef: {
                int spriteNum = objDatum.toSpriteRef();
                SpritePropertyUtils.spriteSetProp(player, spriteNum, propName, value);
                break;
            }

            case PropList: {
                List<Datum.PropListPair> pairs = objDatum.toMap();
                for (Datum.PropListPair pair : pairs) {
                    Datum keyDatum = player.getDatum(pair.key);
                    if (keyDatum.isSymbol() && keyDatum.symbolValue().equalsIgnoreCase(propName)) {
                        pair.value = valueRef;
                        return;
                    } else if (keyDatum.isString() && keyDatum.stringValue().equalsIgnoreCase(propName)) {
                        pair.value = valueRef;
                        return;
                    }
                }
                // Add new property
                int keyRef = player.allocDatum(Datum.ofSymbol(propName));
                pairs.add(new Datum.PropListPair(keyRef, valueRef));
                break;
            }

            case List: {
                // Try to parse propName as a number for indexed access
                try {
                    int index = Integer.parseInt(propName);
                    List<Integer> list = objDatum.toListMut();
                    // Lingo is 1-based
                    int zeroBasedIndex = index - 1;
                    if (zeroBasedIndex >= 0 && zeroBasedIndex < list.size()) {
                        list.set(zeroBasedIndex, valueRef);
                        return;
                    }
                    throw new ScriptError("Index " + index + " out of bounds for list");
                } catch (NumberFormatException e) {
                    throw new ScriptError("Cannot set property '" + propName + "' on list");
                }
            }

            case Point: {
                int[] point = objDatum.toPoint();
                switch (propName.toLowerCase()) {
                    case "loch":
                    case "x":
                        point[0] = valueRef;
                        break;
                    case "locv":
                    case "y":
                        point[1] = valueRef;
                        break;
                    default:
                        throw new ScriptError("Unknown point property: " + propName);
                }
                break;
            }

            case Rect: {
                int[] rect = objDatum.toRect();
                switch (propName.toLowerCase()) {
                    case "left":
                        rect[0] = valueRef;
                        break;
                    case "top":
                        rect[1] = valueRef;
                        break;
                    case "right":
                        rect[2] = valueRef;
                        break;
                    case "bottom":
                        rect[3] = valueRef;
                        break;
                    default:
                        throw new ScriptError("Unknown rect property: " + propName);
                }
                break;
            }

            case CastMemberRef:
                player.setMemberProp(objDatum.toCastMemberRef(), propName, value);
                break;

            case XmlRef:
                XmlHandlers.setProp(player, objDatumRef, propName, valueRef);
                break;

            default:
                throw new ScriptError("Cannot set property '" + propName + "' on " + objType);
        }
    }

    // Helper: resolve last chunk
    private static String resolveLastChunk(DirPlayer player, String string, StringChunkType chunkType) throws ScriptError {
        List<String> chunks = resolveChunkList(player, string, chunkType);
        if (chunks.isEmpty()) {
            return "";
        }
        return chunks.get(chunks.size() - 1);
    }

    // Helper: resolve chunk list
    private static List<String> resolveChunkList(DirPlayer player, String string, StringChunkType chunkType) throws ScriptError {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();

        switch (chunkType) {
            case CHAR:
                for (int i = 0; i < string.length(); i++) {
                    result.add(String.valueOf(string.charAt(i)));
                }
                break;

            case WORD:
                String[] words = string.split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        result.add(word);
                    }
                }
                break;

            case ITEM:
                String delimiter = String.valueOf(player.movie.itemDelimiter);
                String[] items = string.split(java.util.regex.Pattern.quote(delimiter));
                for (String item : items) {
                    result.add(item);
                }
                break;

            case LINE:
                String[] lines = string.split("\r\n|\r|\n");
                for (String line : lines) {
                    result.add(line);
                }
                break;

            default:
                throw new ScriptError("Unknown chunk type: " + chunkType);
        }

        return result;
    }
}
