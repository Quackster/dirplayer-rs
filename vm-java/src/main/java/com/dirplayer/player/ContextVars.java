package com.dirplayer.player;

import com.dirplayer.director.chunks.HandlerDef;
import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.bytecode.BytecodeHandlerContext;
import com.dirplayer.player.bytecode.PutType;
import com.dirplayer.SimpleLogger;


/**
 * Context variable handling for bytecode execution.
 * Port of Rust context_vars.rs
 */
public class ContextVars {
    private static final SimpleLogger logger = SimpleLogger.getLogger(ContextVars.class);

    /**
     * Context variable types.
     */
    public static final int VAR_TYPE_GLOBAL1 = 0x1;
    public static final int VAR_TYPE_GLOBAL2 = 0x2;
    public static final int VAR_TYPE_PROPERTY = 0x3;
    public static final int VAR_TYPE_ARG = 0x4;
    public static final int VAR_TYPE_LOCAL = 0x5;
    public static final int VAR_TYPE_FIELD = 0x6;

    /**
     * Read context variable arguments from stack.
     */
    public static int[] readContextVarArgs(DirPlayer player, int varType, int scopeRef) {
        ScriptScope scope = player.scopes.get(scopeRef);

        Integer castId = null;
        if (varType == VAR_TYPE_FIELD && player.movie.dirVersion >= 500) {
            castId = scope.stack.pop();
        }
        int id = scope.stack.pop();

        return castId != null ? new int[] { id, castId } : new int[] { id };
    }

    /**
     * Get a context variable value.
     */
    public static int getContextVar(
            DirPlayer player,
            int idRef,
            Integer castIdRef,
            int varType,
            BytecodeHandlerContext ctx
    ) throws ScriptError {
        int variableMultiplier = getVariableMultiplier(player, ctx);
        Datum idDatum = player.allocator.get(idRef);
        HandlerDef handler = getCurrentHandlerDef(player, ctx);

        switch (varType) {
            case VAR_TYPE_GLOBAL1:
            case VAR_TYPE_GLOBAL2:
            case VAR_TYPE_PROPERTY:
                throw new ScriptError("readVar global/prop/instance not implemented");

            case VAR_TYPE_ARG:
                int argIndex = idDatum.intValue() / variableMultiplier;
                ScriptScope scope = player.scopes.get(ctx.scopeRef);
                return scope.args.get(argIndex);

            case VAR_TYPE_LOCAL:
                int localIndex = idDatum.intValue() / variableMultiplier;
                String localName = getName(player, ctx, handler.localNameIds.get(localIndex));
                scope = player.scopes.get(ctx.scopeRef);
                Integer local = scope.locals.get(localName);
                return local != null ? local : 0;  // 0 = Void

            case VAR_TYPE_FIELD:
                // Field variable - get field text
                String text = player.movie.castManager.getFieldValueByIdentifiers(
                    idDatum,
                    castIdRef != null ? player.allocator.get(castIdRef) : null
                );
                return player.allocator.alloc(Datum.ofString(text));

            default:
                throw new ScriptError("Invalid context var type: " + varType);
        }
    }

    /**
     * Set a context variable value.
     */
    public static void setContextVar(
            DirPlayer player,
            int idRef,
            Integer castIdRef,
            int varType,
            int valueRef,
            PutType putType,
            BytecodeHandlerContext ctx
    ) throws ScriptError {
        int variableMultiplier = getVariableMultiplier(player, ctx);
        Datum idDatum = player.allocator.get(idRef);
        HandlerDef handler = getCurrentHandlerDef(player, ctx);

        switch (varType) {
            case VAR_TYPE_GLOBAL1:
            case VAR_TYPE_GLOBAL2:
            case VAR_TYPE_PROPERTY:
                throw new ScriptError("set readVar global/prop/instance not implemented");

            case VAR_TYPE_ARG:
                int argIndex = idDatum.intValue() / variableMultiplier;
                ScriptScope scope = player.scopes.get(ctx.scopeRef);
                scope.args.set(argIndex, valueRef);
                break;

            case VAR_TYPE_LOCAL:
                int localIndex = idDatum.intValue() / variableMultiplier;
                String localName = getName(player, ctx, handler.localNameIds.get(localIndex));
                scope = player.scopes.get(ctx.scopeRef);
                scope.locals.put(localName, valueRef);
                break;

            case VAR_TYPE_FIELD:
                // Field variable - set field text
                String newValue = player.allocator.get(valueRef).stringValue();
                CastMemberRef memberRef = player.movie.castManager.findMemberRefByIdentifiers(
                    idDatum,
                    castIdRef != null ? player.allocator.get(castIdRef) : null
                );

                if (memberRef == null) {
                    throw new ScriptError("Field member not found");
                }

                CastMember member = player.movie.castManager.findMemberByRef(memberRef);
                if (member == null || !member.isField()) {
                    throw new ScriptError("Member is not a Field");
                }

                // TODO: Apply put type (into, before, after)
                // For now just set the text
                logger.debug("Setting field text: {}", newValue);
                break;

            default:
                throw new ScriptError("Invalid context var type: " + varType);
        }
    }

    private static int getVariableMultiplier(DirPlayer player, BytecodeHandlerContext ctx) {
        // TODO: Get from script context
        if (player.movie.dirVersion >= 500) {
            return 8;
        }
        return 6;
    }

    private static HandlerDef getCurrentHandlerDef(DirPlayer player, BytecodeHandlerContext ctx) {
        // TODO: Get current handler from script
        return null;
    }

    private static String getName(DirPlayer player, BytecodeHandlerContext ctx, int nameId) {
        // TODO: Get name from script context
        return "var_" + nameId;
    }
}
