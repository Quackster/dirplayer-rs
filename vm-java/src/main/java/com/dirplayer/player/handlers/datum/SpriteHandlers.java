package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.score.Channel;
import com.dirplayer.player.IntRect;

import java.util.List;

/**
 * Handlers for sprite datum operations.
 * Port of Rust SpriteDatumHandlers and SpriteDatumUtils.
 */
public class SpriteHandlers {

    /**
     * Get a property from a sprite.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        int spriteNum = player.getDatum(datumRef).toSpriteRef();
        Sprite sprite = player.movie.score.getSprite(spriteNum);

        if (sprite == null) {
            throw new ScriptError("Sprite " + spriteNum + " not found");
        }

        return getSpriteProperty(player, sprite, spriteNum, prop);
    }

    /**
     * Set a property on a sprite.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        int spriteNum = player.getDatum(datumRef).toSpriteRef();
        Sprite sprite = player.movie.score.getSprite(spriteNum);

        if (sprite == null) {
            throw new ScriptError("Sprite " + spriteNum + " not found");
        }

        setSpriteProperty(player, sprite, prop, valueRef);
    }

    /**
     * Check if two sprites intersect.
     */
    public static int intersects(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("intersects requires 1 argument (sprite number)");
        }

        int spriteNum = player.getDatum(datumRef).toSpriteRef();
        int otherSpriteNum = player.getDatum(args.get(0)).intValue();

        Sprite sprite1 = player.movie.score.getSprite(spriteNum);
        Sprite sprite2 = player.movie.score.getSprite(otherSpriteNum);

        if (sprite1 == null || sprite2 == null) {
            return player.allocDatum(Datum.ofInt(0));
        }

        IntRect rect1 = getConcreteSpriteRect(player, sprite1);
        IntRect rect2 = getConcreteSpriteRect(player, sprite2);

        boolean intersects = !(rect1.right <= rect2.left ||
                               rect1.left >= rect2.right ||
                               rect1.bottom <= rect2.top ||
                               rect1.top >= rect2.bottom);

        return player.allocDatum(Datum.ofInt(intersects ? 1 : 0));
    }

    /**
     * Call handler on sprite datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "intersects":
                return intersects(player, datumRef, args);
            case "getprop": {
                if (args.isEmpty()) {
                    throw new ScriptError("getProp requires at least 1 argument");
                }
                String propName = player.getDatum(args.get(0)).stringValue();
                int result = getProp(player, datumRef, propName);

                // If there's a second argument, it's a sub-property access
                if (args.size() > 1) {
                    return TypeUtils.getSubProp(player, result, args.get(1));
                }
                return result;
            }
            case "setprop": {
                if (args.size() < 2) {
                    throw new ScriptError("setProp requires at least 2 arguments");
                }
                String propName = player.getDatum(args.get(0)).stringValue();
                int valueRef = args.get(1);
                setProp(player, datumRef, propName, valueRef);
                return 0; // Void
            }
            default:
                throw new ScriptError("No handler " + handlerName + " for sprite");
        }
    }

    // Helper methods

    private static int getSpriteProperty(DirPlayer player, Sprite sprite, int spriteNum, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("sprite"));
            case "spritenum":
            case "number":
                return player.allocDatum(Datum.ofInt(spriteNum));
            case "locz":
                return player.allocDatum(Datum.ofInt(sprite.locZ));
            case "loch":
                return player.allocDatum(Datum.ofInt(sprite.locH));
            case "locv":
                return player.allocDatum(Datum.ofInt(sprite.locV));
            case "loc": {
                int locH = player.allocDatum(Datum.ofInt(sprite.locH));
                int locV = player.allocDatum(Datum.ofInt(sprite.locV));
                return player.allocDatum(Datum.ofPoint(locH, locV));
            }
            case "width":
                return player.allocDatum(Datum.ofInt(sprite.width));
            case "height":
                return player.allocDatum(Datum.ofInt(sprite.height));
            case "rect": {
                IntRect r = getConcreteSpriteRect(player, sprite);
                int left = player.allocDatum(Datum.ofInt(r.left));
                int top = player.allocDatum(Datum.ofInt(r.top));
                int right = player.allocDatum(Datum.ofInt(r.right));
                int bottom = player.allocDatum(Datum.ofInt(r.bottom));
                return player.allocDatum(Datum.ofRect(left, top, right, bottom));
            }
            case "visible":
                return player.allocDatum(Datum.ofInt(sprite.visible ? 1 : 0));
            case "ink":
                return player.allocDatum(Datum.ofInt(sprite.ink));
            case "blend":
                return player.allocDatum(Datum.ofInt(sprite.blend));
            case "rotation":
                return player.allocDatum(Datum.ofFloat(sprite.rotation));
            case "skew":
                return player.allocDatum(Datum.ofFloat(sprite.skew));
            case "fliph":
                return player.allocDatum(Datum.ofInt(sprite.flipH ? 1 : 0));
            case "flipv":
                return player.allocDatum(Datum.ofInt(sprite.flipV ? 1 : 0));
            case "stretch":
                return player.allocDatum(Datum.ofInt(sprite.stretch ? 1 : 0));
            case "moveable":
                return player.allocDatum(Datum.ofInt(sprite.moveable ? 1 : 0));
            case "editable":
                return player.allocDatum(Datum.ofInt(sprite.editable ? 1 : 0));
            case "trails":
                return player.allocDatum(Datum.ofInt(sprite.trails ? 1 : 0));
            case "puppet":
                return player.allocDatum(Datum.ofInt(sprite.puppet ? 1 : 0));
            case "cursor": {
                if (sprite.cursor != null && sprite.cursor.cursorId >= 0) {
                    return player.allocDatum(Datum.ofInt(sprite.cursor.cursorId));
                }
                return 0; // Void
            }
            case "member":
                if (sprite.member != null && sprite.member.castMember > 0) {
                    return player.allocDatum(Datum.ofCastMember(sprite.member));
                }
                return 0; // Void
            case "membernum":
                if (sprite.member != null) {
                    return player.allocDatum(Datum.ofInt(sprite.member.castMember));
                }
                return player.allocDatum(Datum.ofInt(0));
            case "castnum":
            case "castlibnum":
                if (sprite.member != null) {
                    return player.allocDatum(Datum.ofInt(sprite.member.castLib));
                }
                return player.allocDatum(Datum.ofInt(0));
            case "forecolor":
            case "color":
                return player.allocDatum(Datum.ofColorRef(sprite.color));
            case "backcolor":
            case "bgcolor":
                return player.allocDatum(Datum.ofColorRef(sprite.bgColor));
            default:
                throw new ScriptError("Cannot get sprite property " + prop);
        }
    }

    private static void setSpriteProperty(DirPlayer player, Sprite sprite, String prop, int valueRef) throws ScriptError {
        Datum value = player.getDatum(valueRef);
        sprite.puppet = true; // Setting a property makes sprite a puppet

        switch (prop.toLowerCase()) {
            case "locz":
                sprite.locZ = value.intValue();
                break;
            case "loch":
                sprite.locH = value.intValue();
                break;
            case "locv":
                sprite.locV = value.intValue();
                break;
            case "loc":
                if (value.isPoint()) {
                    int[] pt = value.toPoint();
                    sprite.locH = player.getDatum(pt[0]).intValue();
                    sprite.locV = player.getDatum(pt[1]).intValue();
                } else {
                    throw new ScriptError("loc must be a point");
                }
                break;
            case "width":
                sprite.width = value.intValue();
                break;
            case "height":
                sprite.height = value.intValue();
                break;
            case "rect":
                if (value.isRect()) {
                    int[] r = value.toRect();
                    int left = player.getDatum(r[0]).intValue();
                    int top = player.getDatum(r[1]).intValue();
                    int right = player.getDatum(r[2]).intValue();
                    int bottom = player.getDatum(r[3]).intValue();
                    sprite.locH = left;
                    sprite.locV = top;
                    sprite.width = right - left;
                    sprite.height = bottom - top;
                } else {
                    throw new ScriptError("rect must be a rect");
                }
                break;
            case "visible":
                sprite.visible = value.intValue() != 0;
                break;
            case "ink":
                sprite.ink = value.intValue();
                break;
            case "blend":
                sprite.blend = value.intValue();
                break;
            case "rotation":
                sprite.rotation = value.floatValue();
                break;
            case "skew":
                sprite.skew = value.floatValue();
                break;
            case "fliph":
                sprite.flipH = value.intValue() != 0;
                break;
            case "flipv":
                sprite.flipV = value.intValue() != 0;
                break;
            case "stretch":
                sprite.stretch = value.intValue() != 0;
                break;
            case "moveable":
                sprite.moveable = value.intValue() != 0;
                break;
            case "editable":
                sprite.editable = value.intValue() != 0;
                break;
            case "trails":
                sprite.trails = value.intValue() != 0;
                break;
            case "puppet":
                sprite.puppet = value.intValue() != 0;
                break;
            case "member":
                if (value.isCastMemberRef()) {
                    sprite.member = value.toMemberRef();
                } else if (value.isInt()) {
                    int memberNum = value.intValue();
                    sprite.member = new CastMemberRef(1, memberNum);
                } else {
                    throw new ScriptError("member must be a cast member or integer");
                }
                break;
            case "membernum":
                if (sprite.member == null) {
                    sprite.member = new CastMemberRef(1, value.intValue());
                } else {
                    sprite.member = new CastMemberRef(sprite.member.castLib, value.intValue());
                }
                break;
            case "forecolor":
            case "color":
                sprite.color = value.toColorRef();
                break;
            case "backcolor":
            case "bgcolor":
                sprite.bgColor = value.toColorRef();
                break;
            default:
                throw new ScriptError("Cannot set sprite property " + prop);
        }
    }

    private static IntRect getConcreteSpriteRect(DirPlayer player, Sprite sprite) {
        // Get the sprite's bounding rect based on its properties
        int left = sprite.locH;
        int top = sprite.locV;
        int right = left + sprite.width;
        int bottom = top + sprite.height;

        // Apply registration point offset if member exists
        if (sprite.member != null && sprite.member.castMember > 0) {
            // TODO: Get reg point from member and adjust
        }

        return new IntRect(left, top, right, bottom);
    }

    /**
     * Type utilities for sub-property access.
     */
    private static class TypeUtils {
        public static int getSubProp(DirPlayer player, int datumRef, int keyRef) throws ScriptError {
            Datum datum = player.getDatum(datumRef);
            Datum key = player.getDatum(keyRef);
            String keyStr = key.stringValue();

            if (datum.isList()) {
                return ListHandlers.getAt(player, datumRef, List.of(keyRef));
            } else if (datum.isPropList()) {
                return PropListHandlers.getAProp(player, datumRef, List.of(keyRef));
            } else if (datum.isPoint()) {
                return PointHandlers.getProp(player, datumRef, keyStr);
            } else if (datum.isRect()) {
                return RectHandlers.getProp(player, datumRef, keyStr);
            } else {
                throw new ScriptError("Cannot get sub-property of " + datum.typeStr());
            }
        }
    }
}
