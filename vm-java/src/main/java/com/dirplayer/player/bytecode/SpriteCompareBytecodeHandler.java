package com.dirplayer.player.bytecode;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import java.util.List;

/**
 * Sprite comparison bytecode handlers - onto/into sprite operations.
 * Port of Rust SpriteCompareBytecodeHandler struct.
 */
public class SpriteCompareBytecodeHandler {

    /**
     * Helper to get sprite number from a datum reference.
     */
    private static int getSpriteNum(DirPlayer player, int datumRef) throws ScriptError {
        Datum datum = player.getDatum(datumRef);

        // Try to_sprite_ref first (proper sprite reference)
        if (datum.isSpriteRef()) {
            return datum.toSpriteRef();
        }

        // Fall back to int_value for plain integers
        if (datum.isInt()) {
            return datum.intValue();
        }

        throw new ScriptError("Expected sprite reference or integer, got " + player.formatDatum(datum));
    }

    /**
     * Helper to get rect bounds for a sprite.
     * Returns [left, top, right, bottom].
     */
    private static int[] getRectBounds(DirPlayer player, int spriteNum) throws ScriptError {
        Datum rectDatum = player.spriteGetProp(spriteNum, "rect");

        if (rectDatum.isRect()) {
            int[] coords = rectDatum.toRect();
            int left = player.getDatum(coords[0]).intValue();
            int top = player.getDatum(coords[1]).intValue();
            int right = player.getDatum(coords[2]).intValue();
            int bottom = player.getDatum(coords[3]).intValue();
            return new int[] { left, top, right, bottom };
        } else if (rectDatum.isList()) {
            List<Integer> coords = rectDatum.toList();
            if (coords.size() != 4) {
                throw new ScriptError("Sprite " + spriteNum + " rect has invalid format (length " + coords.size() + ")");
            }
            int left = player.getDatum(coords.get(0)).intValue();
            int top = player.getDatum(coords.get(1)).intValue();
            int right = player.getDatum(coords.get(2)).intValue();
            int bottom = player.getDatum(coords.get(3)).intValue();
            return new int[] { left, top, right, bottom };
        } else {
            throw new ScriptError("Sprite " + spriteNum + " rect is not a rect or list: " + player.formatDatum(rectDatum));
        }
    }

    /**
     * ontospr - Check if one sprite intersects with another sprite.
     * Pops two values from stack:
     * - First pop: target sprite (from sprite() call or sprite number)
     * - Second pop: source sprite number
     * Pushes 1 if sprites intersect, 0 if they don't.
     */
    public static HandlerExecutionResult ontoSprite(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);

        // Pop the target sprite (result from sprite() call)
        int targetSpriteRef = scope.stack.pop();

        // Pop the source sprite number
        int sourceSpriteRef = scope.stack.pop();

        int sourceSpriteNum, targetSpriteNum;
        try {
            sourceSpriteNum = getSpriteNum(player, sourceSpriteRef);
            targetSpriteNum = getSpriteNum(player, targetSpriteRef);
        } catch (ScriptError e) {
            // If sprite numbers can't be obtained, return 0
            int resultRef = player.allocDatum(Datum.ofInt(0));
            scope.stack.push(resultRef);
            return HandlerExecutionResult.ADVANCE;
        }

        // Get rectangles for both sprites
        int[] sourceRect, targetRect;
        try {
            sourceRect = getRectBounds(player, sourceSpriteNum);
        } catch (ScriptError e) {
            // Sprite doesn't exist or has no rect, return 0 (no collision)
            int resultRef = player.allocDatum(Datum.ofInt(0));
            scope.stack.push(resultRef);
            return HandlerExecutionResult.ADVANCE;
        }

        try {
            targetRect = getRectBounds(player, targetSpriteNum);
        } catch (ScriptError e) {
            // Sprite doesn't exist or has no rect, return 0 (no collision)
            int resultRef = player.allocDatum(Datum.ofInt(0));
            scope.stack.push(resultRef);
            return HandlerExecutionResult.ADVANCE;
        }

        // Check if rectangles intersect
        // Rectangles DON'T intersect if one is completely to the side of the other
        int srcLeft = sourceRect[0];
        int srcTop = sourceRect[1];
        int srcRight = sourceRect[2];
        int srcBottom = sourceRect[3];

        int tgtLeft = targetRect[0];
        int tgtTop = targetRect[1];
        int tgtRight = targetRect[2];
        int tgtBottom = targetRect[3];

        boolean intersects = !(
            srcRight <= tgtLeft ||   // source is completely to the left
            srcLeft >= tgtRight ||   // source is completely to the right
            srcBottom <= tgtTop ||   // source is completely above
            srcTop >= tgtBottom      // source is completely below
        );

        // Push result (1 for true, 0 for false)
        int result = intersects ? 1 : 0;
        int resultRef = player.allocDatum(Datum.ofInt(result));
        scope.stack.push(resultRef);

        return HandlerExecutionResult.ADVANCE;
    }

    /**
     * intospr - Check if one sprite is completely within another sprite.
     * Pops two values from stack:
     * - First pop: target sprite (the container)
     * - Second pop: source sprite number (the sprite to check if within)
     * Pushes 1 if source is completely within target, 0 otherwise.
     */
    public static HandlerExecutionResult intoSprite(DirPlayer player, BytecodeHandlerContext ctx) throws ScriptError {
        ScriptScope scope = player.scopes.get(ctx.scopeRef);

        // Pop the target sprite (the container)
        int targetSpriteRef = scope.stack.pop();

        // Pop the source sprite number (the one to check if within)
        int sourceSpriteRef = scope.stack.pop();

        int sourceSpriteNum, targetSpriteNum;
        try {
            sourceSpriteNum = getSpriteNum(player, sourceSpriteRef);
            targetSpriteNum = getSpriteNum(player, targetSpriteRef);
        } catch (ScriptError e) {
            // If sprite numbers can't be obtained, return 0
            int resultRef = player.allocDatum(Datum.ofInt(0));
            scope.stack.push(resultRef);
            return HandlerExecutionResult.ADVANCE;
        }

        // Get rectangles for both sprites
        int[] sourceRect, targetRect;
        try {
            sourceRect = getRectBounds(player, sourceSpriteNum);
        } catch (ScriptError e) {
            int resultRef = player.allocDatum(Datum.ofInt(0));
            scope.stack.push(resultRef);
            return HandlerExecutionResult.ADVANCE;
        }

        try {
            targetRect = getRectBounds(player, targetSpriteNum);
        } catch (ScriptError e) {
            int resultRef = player.allocDatum(Datum.ofInt(0));
            scope.stack.push(resultRef);
            return HandlerExecutionResult.ADVANCE;
        }

        // Check if source is completely within target
        // Source is within target if all edges of source are inside target
        int srcLeft = sourceRect[0];
        int srcTop = sourceRect[1];
        int srcRight = sourceRect[2];
        int srcBottom = sourceRect[3];

        int tgtLeft = targetRect[0];
        int tgtTop = targetRect[1];
        int tgtRight = targetRect[2];
        int tgtBottom = targetRect[3];

        boolean isWithin =
            srcLeft >= tgtLeft &&
            srcTop >= tgtTop &&
            srcRight <= tgtRight &&
            srcBottom <= tgtBottom;

        // Push result (1 for true, 0 for false)
        int result = isWithin ? 1 : 0;
        int resultRef = player.allocDatum(Datum.ofInt(result));
        scope.stack.push(resultRef);

        return HandlerExecutionResult.ADVANCE;
    }
}
