package com.dirplayer.player.score;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.CursorRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.IntRect;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.script.ScriptInstanceRef;
import com.dirplayer.player.script.ScriptUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for sprite property access.
 * Port of Rust score.rs sprite_get_prop and sprite_set_prop functions.
 */
public class SpritePropertyUtils {

    /**
     * Get a property from a sprite.
     */
    public static Datum spriteGetProp(DirPlayer player, int spriteId, String propName) throws ScriptError {
        Sprite sprite = player.movie.score.getSprite((short) spriteId);

        switch (propName) {
            case "ilk":
                return Datum.ofSymbol("sprite");

            case "spriteNum":
            case "spriteNumber":
                return Datum.ofInt(sprite != null ? sprite.number : spriteId);

            case "loc": {
                int x = sprite != null ? sprite.locH : 0;
                int y = sprite != null ? sprite.locV : 0;
                int xRef = player.allocDatum(Datum.ofInt(x));
                int yRef = player.allocDatum(Datum.ofInt(y));
                return Datum.ofPoint(xRef, yRef);
            }

            case "width":
                return Datum.ofInt(sprite != null ? sprite.width : 0);

            case "height":
                return Datum.ofInt(sprite != null ? sprite.height : 0);

            case "blend":
                return Datum.ofInt(sprite != null ? sprite.blend : 0);

            case "ink":
                return Datum.ofInt(sprite != null ? sprite.ink : 0);

            case "left":
                if (sprite != null) {
                    return Datum.ofInt(sprite.getLeft());
                }
                return Datum.ofInt(0);

            case "top":
                if (sprite != null) {
                    return Datum.ofInt(sprite.getTop());
                }
                return Datum.ofInt(0);

            case "right":
                if (sprite != null) {
                    return Datum.ofInt(sprite.getRight());
                }
                return Datum.ofInt(0);

            case "bottom":
                if (sprite != null) {
                    return Datum.ofInt(sprite.getBottom());
                }
                return Datum.ofInt(0);

            case "rect":
                if (sprite != null) {
                    IntRect rect = sprite.getRect();
                    int leftRef = player.allocDatum(Datum.ofInt(rect.left));
                    int topRef = player.allocDatum(Datum.ofInt(rect.top));
                    int rightRef = player.allocDatum(Datum.ofInt(rect.right));
                    int bottomRef = player.allocDatum(Datum.ofInt(rect.bottom));
                    return Datum.ofRect(leftRef, topRef, rightRef, bottomRef);
                }
                return Datum.ofRect(
                    player.allocDatum(Datum.ofInt(0)),
                    player.allocDatum(Datum.ofInt(0)),
                    player.allocDatum(Datum.ofInt(0)),
                    player.allocDatum(Datum.ofInt(0))
                );

            case "color":
                if (sprite != null && sprite.getColor() != null) {
                    return Datum.ofColorRef(sprite.getColor());
                }
                return Datum.ofColorRef(ColorRef.paletteIndex(255));

            case "bgColor":
                if (sprite != null && sprite.getBgColor() != null) {
                    return Datum.ofColorRef(sprite.getBgColor());
                }
                return Datum.ofColorRef(ColorRef.paletteIndex(0));

            case "skew":
                return Datum.ofFloat(sprite != null ? sprite.skew : 0.0f);

            case "locH":
                return Datum.ofInt(sprite != null ? sprite.locH : 0);

            case "locV":
                return Datum.ofInt(sprite != null ? sprite.locV : 0);

            case "locZ":
                return Datum.ofInt(sprite != null ? sprite.locZ : 0);

            case "member":
                if (sprite != null && sprite.memberRef != null) {
                    return Datum.ofCastMember(sprite.memberRef);
                }
                return Datum.ofCastMember(CastMemberRef.INVALID);

            case "flipH":
                return Datum.ofBool(sprite != null && sprite.flipH);

            case "flipV":
                return Datum.ofBool(sprite != null && sprite.flipV);

            case "rotation":
                return Datum.ofFloat(sprite != null ? sprite.rotationFloat : 0.0f);

            case "scriptInstanceList": {
                List<Integer> instanceRefs = new ArrayList<>();
                if (sprite != null && sprite.scriptInstanceList != null) {
                    for (Integer instanceId : sprite.scriptInstanceList) {
                        instanceRefs.add(player.allocDatum(Datum.ofScriptInstanceRef(new ScriptInstanceRef(instanceId))));
                    }
                }
                return Datum.ofList(instanceRefs, false);
            }

            case "castNum":
                if (sprite != null && sprite.memberRef != null) {
                    // Calculate slot number
                    int castSlot = getCastSlotNumber(sprite.memberRef.castLib, sprite.memberRef.castMember);
                    return Datum.ofInt(castSlot);
                }
                return Datum.ofInt(0);

            case "scriptNum":
                if (sprite != null && !sprite.scriptInstanceList.isEmpty()) {
                    Integer firstInstanceId = sprite.scriptInstanceList.get(0);
                    com.dirplayer.player.script.ScriptInstance instance =
                        player.allocator.getScriptInstance(new ScriptInstanceRef(firstInstanceId));
                    if (instance != null && instance.script != null) {
                        return Datum.ofInt(instance.script.castMember);
                    }
                }
                return Datum.ofInt(0);

            case "visible":
                return Datum.ofBool(sprite == null || sprite.visible);

            case "puppet":
                return Datum.ofBool(sprite != null && sprite.puppet);

            case "foreColor":
                return Datum.ofInt(sprite != null ? sprite.foreColor : 255);

            case "backColor":
                return Datum.ofInt(sprite != null ? sprite.backColor : 0);

            case "cursor":
                if (sprite != null && sprite.cursor != null) {
                    if (sprite.cursor.isSystem()) {
                        return Datum.ofInt(sprite.cursor.cursorId);
                    } else if (sprite.cursor.memberList != null && !sprite.cursor.memberList.isEmpty()) {
                        List<Integer> ids = new ArrayList<>();
                        for (int id : sprite.cursor.memberList) {
                            ids.add(player.allocDatum(Datum.ofInt(id)));
                        }
                        return Datum.ofList(ids, false);
                    }
                }
                return Datum.ofInt(0);

            case "startFrame": {
                int currentFrame = player.movie.currentFrame;
                for (ScoreSpriteSpan span : player.movie.score.spriteSpans) {
                    if (span.channelNumber == spriteId &&
                        currentFrame >= span.startFrame &&
                        currentFrame <= span.endFrame) {
                        return Datum.ofInt(span.startFrame);
                    }
                }
                return Datum.ofInt(0);
            }

            case "endFrame": {
                int currentFrame = player.movie.currentFrame;
                for (ScoreSpriteSpan span : player.movie.score.spriteSpans) {
                    if (span.channelNumber == spriteId &&
                        currentFrame >= span.startFrame &&
                        currentFrame <= span.endFrame) {
                        return Datum.ofInt(span.endFrame);
                    }
                }
                return Datum.ofInt(0);
            }

            case "stretch":
                return Datum.ofInt(sprite != null ? sprite.stretch : 0);

            case "trails":
                return Datum.ofBool(sprite != null && sprite.trails);

            case "moveable":
            case "moveableSprite":
                return Datum.ofBool(sprite != null && sprite.moveable);

            case "editableText":
                return Datum.ofBool(sprite != null && sprite.editableText);

            case "constraint":
                return Datum.ofInt(sprite != null ? sprite.constraint : 0);

            case "lineSize":
                return Datum.ofInt(sprite != null ? sprite.lineSize : 1);

            case "pattern":
                return Datum.ofInt(sprite != null ? sprite.pattern : 0);

            default:
                // Try to get from behavior scripts
                if (sprite != null && sprite.scriptInstanceList != null) {
                    for (Integer instanceId : sprite.scriptInstanceList) {
                        ScriptInstanceRef ref = new ScriptInstanceRef(instanceId);
                        Integer propRef = ScriptUtils.scriptGetPropOpt(player, ref, propName);
                        if (propRef != null) {
                            return player.getDatum(propRef).clone();
                        }
                    }
                }
                throw new ScriptError("Cannot get prop " + propName + " of sprite");
        }
    }

    /**
     * Set a property on a sprite.
     */
    public static void spriteSetProp(DirPlayer player, int spriteId, String propName, Datum value) throws ScriptError {
        Sprite sprite = player.movie.score.getSprite((short) spriteId);
        if (sprite == null) {
            throw new ScriptError("Sprite " + spriteId + " not found");
        }

        switch (propName) {
            case "visible":
                sprite.visible = value.toBool();
                break;

            case "stretch":
                sprite.stretch = value.intValue();
                break;

            case "locH":
                sprite.locH = value.intValue();
                break;

            case "locV":
                sprite.locV = value.intValue();
                break;

            case "locZ":
                sprite.locZ = value.intValue();
                break;

            case "width":
                sprite.width = value.intValue();
                sprite.hasSizeChanged = true;
                break;

            case "height":
                sprite.height = value.intValue();
                sprite.hasSizeChanged = true;
                break;

            case "ink":
                sprite.ink = value.intValue();
                break;

            case "blend":
                sprite.blend = value.intValue();
                break;

            case "rotation":
                if (value.isNumber()) {
                    sprite.rotationFloat = (float) value.floatValue();
                } else {
                    sprite.rotationFloat = 0.0f;
                }
                break;

            case "skew":
                if (value.isNumber()) {
                    sprite.skew = (float) value.floatValue();
                } else {
                    sprite.skew = 0.0f;
                }
                break;

            case "flipH":
                if (value.isNumber()) {
                    sprite.flipH = value.toBool();
                } else {
                    sprite.flipH = false;
                }
                break;

            case "flipV":
                if (value.isNumber()) {
                    sprite.flipV = value.toBool();
                } else {
                    sprite.flipV = false;
                }
                break;

            case "backColor":
                sprite.backColor = value.intValue();
                sprite.setBgColor(ColorRef.paletteIndex(value.intValue()));
                sprite.hasBackColor = true;
                break;

            case "bgColor":
                sprite.setBgColor(value.toColorRef());
                sprite.backColor = sprite.getBgColor().getPaletteIndex();
                sprite.hasBackColor = true;
                break;

            case "foreColor":
                sprite.foreColor = value.intValue();
                sprite.setColor(ColorRef.paletteIndex(value.intValue()));
                sprite.hasForeColor = true;
                break;

            case "color":
                sprite.setColor(value.toColorRef());
                sprite.foreColor = sprite.getColor().getPaletteIndex();
                sprite.hasForeColor = true;
                break;

            case "member":
                if (value.getType() == DatumType.CastMember || value.getType() == DatumType.CastMemberRef) {
                    sprite.memberRef = value.toCastMemberRef();
                } else if (value.isString()) {
                    CastMemberRef ref = player.movie.castManager.findMemberRefByName(value.stringValue());
                    if (ref != null) {
                        sprite.memberRef = ref;
                    }
                } else if (value.isNumber()) {
                    CastMemberRef ref = player.movie.castManager.findMemberRefByNumber(value.intValue());
                    if (ref != null) {
                        sprite.memberRef = ref;
                    }
                }
                break;

            case "loc":
                if (value.getType() == DatumType.Point) {
                    int[] coords = value.toPointCoords(player);
                    sprite.locH = coords[0];
                    sprite.locV = coords[1];
                }
                break;

            case "rect":
                if (value.getType() == DatumType.Rect) {
                    int[] rectCoords = value.toRectCoords(player);
                    int left = rectCoords[0];
                    int top = rectCoords[1];
                    int right = rectCoords[2];
                    int bottom = rectCoords[3];
                    sprite.width = right - left;
                    sprite.height = bottom - top;
                    sprite.locH = left + sprite.width / 2;
                    sprite.locV = top + sprite.height / 2;
                }
                break;

            case "puppet":
                sprite.puppet = value.toBool();
                break;

            case "moveable":
            case "moveableSprite":
                sprite.moveable = value.toBool();
                break;

            case "editableText":
                sprite.editableText = value.toBool();
                break;

            case "constraint":
                sprite.constraint = value.intValue();
                break;

            case "trails":
                sprite.trails = value.toBool();
                break;

            case "lineSize":
                sprite.lineSize = value.intValue();
                break;

            case "pattern":
                sprite.pattern = value.intValue();
                break;

            case "cursor":
                if (value.isInt()) {
                    sprite.cursor = CursorRef.system(value.intValue());
                } else if (value.isList()) {
                    // Member cursor
                    List<Integer> list = value.toList();
                    List<Integer> memberIdList = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        memberIdList.add(player.getDatum(list.get(i)).intValue());
                    }
                    sprite.cursor = CursorRef.memberList(memberIdList);
                }
                break;

            default:
                // Try to set on behavior scripts
                if (sprite.scriptInstanceList != null && !sprite.scriptInstanceList.isEmpty()) {
                    for (Integer instanceId : sprite.scriptInstanceList) {
                        ScriptInstanceRef ref = new ScriptInstanceRef(instanceId);
                        try {
                            int valueRef = player.allocDatum(value);
                            ScriptUtils.scriptSetProp(player, ref, propName, valueRef, false);
                            return;
                        } catch (ScriptError e) {
                            // Try next behavior
                        }
                    }
                }
                throw new ScriptError("Cannot set prop " + propName + " of sprite");
        }
    }

    /**
     * Calculate the cast slot number (global member number).
     */
    private static int getCastSlotNumber(int castLib, int castMember) {
        // Standard Director cast slot calculation
        return (castLib - 1) * 512 + castMember;
    }
}
