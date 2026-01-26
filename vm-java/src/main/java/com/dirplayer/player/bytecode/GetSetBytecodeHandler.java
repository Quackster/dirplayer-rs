package com.dirplayer.player.bytecode;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.director.lingo.StringChunkType;
import com.dirplayer.director.lingo.StringChunkExpr;
import com.dirplayer.director.chunks.HandlerDef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;

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
        // TODO: These methods don't exist yet - need to be implemented
        // ScriptInstanceRef receiver = scope.receiver;
        // CastMemberRef scriptRef = scope.scriptMemberRef;

        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        // int result;
        // if (receiver != null) {
        //     result = player.scriptGetProp(receiver, propName);
        // } else {
        //     result = player.scriptGetStaticProp(scriptRef, propName);
        // }

        // scope.stack.push(result);
        // For now, push a void value
        scope.stack.push(0);
        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult setProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueRef = scope.stack.pop();
        // TODO: These methods don't exist yet - need to be implemented
        // ScriptInstanceRef receiver = scope.receiver;
        // CastMemberRef scriptRef = scope.scriptMemberRef;

        // if (receiver != null) {
        //     player.scriptSetProp(receiver, propName, valueRef, false);
        // } else {
        //     player.scriptSetStaticProp(scriptRef, propName, valueRef, true);
        // }

        return HandlerExecutionResult.ADVANCE;
    }

    public static HandlerExecutionResult setObjProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int valueRef = scope.stack.pop();
        int objDatumRef = scope.stack.pop();
        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        // TODO: This method doesn't exist yet - need to be implemented
        // player.playerSetObjProp(objDatumRef, propName, valueRef);
        // return HandlerExecutionResult.ADVANCE;
        throw new ScriptError("setObjProp not implemented yet");
    }

    public static HandlerExecutionResult getObjProp(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);
        int objDatumRef = scope.stack.pop();

        String propName = player.getName(ctx, (int) player.getCtxCurrentBytecode(ctx).obj);

        Datum objDatum = player.getDatum(objDatumRef);

        // Handle XML refs specially
        if (objDatum.getType() == DatumType.XmlRef) {
            // TODO: This method doesn't exist yet
            // int resultRef = player.xmlGetProp(objDatumRef, propName);
            // scope.stack.push(resultRef);
            // return HandlerExecutionResult.ADVANCE;
            throw new ScriptError("xmlGetProp not implemented yet");
        }

        // TODO: This method doesn't exist yet
        // int resultRef = player.getObjProp(objDatumRef, propName);
        // scope.stack.push(resultRef);
        scope.stack.push(0); // Push void for now
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
                    // TODO: getMoviePropNameById not implemented
                    // String propName = player.getMoviePropNameById(propertyId);
                    // setTheBuiltInProp(player, ctx, propName, value);
                    // return HandlerExecutionResult.ADVANCE;
                    throw new ScriptError("set movie prop by ID not implemented yet");
                } else {
                    throw new ScriptError("Invalid propertyType/propertyID for kOpSet: " + propertyType);
                }

            case 0x04:
                // Sound channel properties
                // TODO: getSoundPropName and soundChannelSetProp not implemented
                // String soundPropName = player.getSoundPropName(propertyId);
                // int channelNumRef = scope.stack.pop();
                // int channelNum = player.getDatum(channelNumRef).intValue();
                // int soundChannelDatum = player.allocDatum(Datum.ofSoundChannel(channelNum));
                // player.soundChannelSetProp(soundChannelDatum, soundPropName, valueRef);
                // return HandlerExecutionResult.ADVANCE;
                scope.stack.pop(); // Pop channelNumRef
                throw new ScriptError("set sound channel prop not implemented yet");

            case 0x06:
                // TODO: getSpritePropName and spriteSetProp not implemented
                // String spritePropName = player.getSpritePropName(propertyId);
                // int spriteNum = player.getDatum(spriteRef).intValue();
                // player.spriteSetProp(spriteNum, spritePropName, value);
                // return HandlerExecutionResult.ADVANCE;
                int spriteRef = scope.stack.pop();
                throw new ScriptError("set sprite prop not implemented yet");

            case 0x07:
                // TODO: getAnimPropName not implemented
                // String animPropName = player.getAnimPropName(propertyId);
                // player.setMovieProp(animPropName, value);
                // return HandlerExecutionResult.ADVANCE;
                throw new ScriptError("set anim prop not implemented yet");

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
        try {
            Integer.parseInt(propName);
            isNumericIndex = true;
        } catch (NumberFormatException e) {
            // Not numeric
        }

        int resultRef;

        switch (objType) {
            case SpriteRef:
                // Handle sprite references
                int spriteNum = objDatum.toSpriteRef();
                // TODO: spriteGetProp not implemented
                // try {
                //     Datum datum = player.spriteGetProp(spriteNum, propName);
                //     resultRef = player.allocDatum(datum);
                // } catch (ScriptError e) {
                //     // Not a built-in property, try script instances
                //     resultRef = 0; // Void
                // }
                resultRef = 0; // Return void for now
                break;

            case XmlRef:
                // TODO: xmlGetProp not implemented
                // resultRef = player.xmlGetProp(objRef, propName);
                resultRef = 0; // Return void for now
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
                    // TODO: getObjProp not implemented
                    // resultRef = player.getObjProp(objRef, propName);
                    resultRef = 0; // Return void for now
                }
                break;

            case List:
                if (isNumericIndex) {
                    int index = Integer.parseInt(propName);
                    java.util.List<Integer> list = objDatum.toList();
                    // Lingo uses 1-based indexing
                    int zeroBasedIndex = index - 1;
                    if (zeroBasedIndex >= 0 && zeroBasedIndex < list.size()) {
                        resultRef = list.get(zeroBasedIndex);
                    } else {
                        throw new ScriptError("List index " + index + " out of bounds (list has " + list.size() + " items)");
                    }
                } else {
                    // TODO: listGetProp not implemented
                    // resultRef = player.listGetProp(objRef, propName);
                    resultRef = 0; // Return void for now
                }
                break;

            case PropList:
                // TODO: getObjProp not implemented
                // resultRef = player.getObjProp(objRef, propName);
                resultRef = 0; // Return void for now
                break;

            case ScriptInstanceRef:
                if (isNumericIndex) {
                    // Try to find indexable property
                    throw new ScriptError("Cannot use numeric index '" + propName + "' on script instance");
                } else {
                    // TODO: getObjProp not implemented
                    // resultRef = player.getObjProp(objRef, propName);
                    resultRef = 0; // Return void for now
                }
                break;

            default:
                // TODO: getObjProp not implemented
                // resultRef = player.getObjProp(objRef, propName);
                resultRef = 0; // Return void for now
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
            // TODO: getMoviePropNameById not implemented
            // String propName = player.getMoviePropNameById(propId);
            // int result = getTheBuiltInProp(player, ctx, propName);
            // scope.stack.push(result);
            // return HandlerExecutionResult.ADVANCE;
            throw new ScriptError("get movie prop by ID not implemented yet");
        } else if (propType == 0) {
            // Last chunk
            int stringIdRef = scope.stack.pop();
            String string = player.getDatum(stringIdRef).stringValue();
            StringChunkType chunkType = StringChunkType.fromId(propId - 0x0b);
            // TODO: resolveLastChunk not implemented
            // String lastChunk = player.resolveLastChunk(string, chunkType);
            // int result = player.allocDatum(Datum.ofString(lastChunk));
            // scope.stack.push(result);
            // return HandlerExecutionResult.ADVANCE;
            throw new ScriptError("resolveLastChunk not implemented yet");
        } else if (propType == 0x06) {
            // Sprite prop
            // TODO: getSpritePropNameById and spriteGetProp not implemented
            // String propName = player.getSpritePropNameById(propId);
            // if (propName != null) {
            //     int datumRef = scope.stack.pop();
            //     int spriteNum = player.getDatum(datumRef).intValue();
            //     Datum resultDatum = player.spriteGetProp(spriteNum, propName);
            //     int result = player.allocDatum(resultDatum);
            //     scope.stack.push(result);
            //     return HandlerExecutionResult.ADVANCE;
            // } else {
            //     throw new ScriptError("kOpGet sprite prop " + propId + " not implemented");
            // }
            int datumRef = scope.stack.pop();
            throw new ScriptError("kOpGet sprite prop " + propId + " not implemented");
        } else if (propType == 0x07) {
            // Anim prop
            // TODO: getAnimProp not implemented
            // int result = player.allocDatum(player.getAnimProp(propId));
            // scope.stack.push(result);
            // return HandlerExecutionResult.ADVANCE;
            throw new ScriptError("getAnimProp not implemented yet");
        } else if (propType == 0x08) {
            // Anim2 prop
            // TODO: getAnim2Prop and getCastMemberCount not implemented
            // Datum datum;
            if (propId == 0x02 && player.movie.dirVersion >= 500) {
                // the number of castMembers supports castLib selection
                int castLibIdRef = scope.stack.pop();
            //     Datum castLibId = player.getDatum(castLibIdRef);
            //     boolean bypassCastLibSelection = castLibId.isInt() && castLibId.intValue() == 0;
            //     if (bypassCastLibSelection) {
            //         datum = player.getAnim2Prop(propId);
            //     } else {
            //         // Get cast member count for specific cast lib
            //         datum = player.getCastMemberCount(castLibId);
            //     }
            // } else {
            //     datum = player.getAnim2Prop(propId);
            }
            // int result = player.allocDatum(datum);
            // scope.stack.push(result);
            // return HandlerExecutionResult.ADVANCE;
            throw new ScriptError("getAnim2Prop not implemented yet");
        } else if (propType == 0x09) {
            // Anim prop (alternate)
            // TODO: getAnimProp not implemented
            // int result = player.allocDatum(player.getAnimProp(propId));
            // scope.stack.push(result);
            // return HandlerExecutionResult.ADVANCE;
            throw new ScriptError("getAnimProp not implemented yet");
        } else if (propType == 0x0b) {
            // Sound properties
            if (propId == 2) {
                // Number of sounds
                scope.stack.pop();
                // TODO: soundManager.getNumChannels not implemented
                // int result = player.allocDatum(Datum.ofInt(player.soundManager.getNumChannels()));
                // scope.stack.push(result);
                // return HandlerExecutionResult.ADVANCE;
                throw new ScriptError("soundManager.getNumChannels not implemented yet");
            } else {
                // TODO: getSoundPropName and soundChannelGetProp not implemented
                // String propName = player.getSoundPropName(propId);
                int datumRef = scope.stack.pop();
                // int channelNum = player.getDatum(datumRef).intValue();
                //
                // if (channelNum == 0) {
                //     throw new ScriptError("Sound channel index must be >= 1 for property '" + propName + "'");
                // }
                //
                // int soundChannelDatum = player.allocDatum(Datum.ofSoundChannel(channelNum));
                // Datum resultDatum = player.soundChannelGetProp(soundChannelDatum, propName);
                // int result = player.allocDatum(resultDatum);
                // scope.stack.push(result);
                // return HandlerExecutionResult.ADVANCE;
                throw new ScriptError("soundChannelGetProp not implemented yet");
            }
        } else if (propType == 0x01) {
            // Number of chunks
            int stringIdRef = scope.stack.pop();
            // TODO: resolveChunkList not implemented
            // String string = player.getDatum(stringIdRef).stringValue();
            // StringChunkType chunkType = StringChunkType.fromId(propId);
            // java.util.List<String> chunks = player.resolveChunkList(string, chunkType);
            // int result = player.allocDatum(Datum.ofInt(chunks.size()));
            // scope.stack.push(result);
            // return HandlerExecutionResult.ADVANCE;
            throw new ScriptError("resolveChunkList not implemented yet");
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
}
