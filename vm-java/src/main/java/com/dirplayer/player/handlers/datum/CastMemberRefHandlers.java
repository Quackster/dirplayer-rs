package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.StringChunkType;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.cast.CastMember;
import com.dirplayer.player.cast.CastMemberType;

import java.util.List;

/**
 * Handlers for cast member reference datum operations.
 * Port of Rust CastMemberRefHandlers.
 */
public class CastMemberRefHandlers {

    /**
     * Get slot number from cast lib and member number.
     */
    public static int getCastSlotNumber(int castLib, int castMember) {
        return (castLib << 16) | (castMember & 0xFFFF);
    }

    /**
     * Get member ref from slot number.
     */
    public static CastMemberRef memberRefFromSlotNumber(int slotNumber) {
        int castLib = slotNumber >> 16;
        int castMember = slotNumber & 0xFFFF;
        return new CastMemberRef(castLib, castMember);
    }

    /**
     * Get a property from a cast member.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        CastMemberRef memberRef = player.getDatum(datumRef).toMemberRef();
        return getMemberProp(player, memberRef, prop);
    }

    /**
     * Set a property on a cast member.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        CastMemberRef memberRef = player.getDatum(datumRef).toMemberRef();
        setMemberProp(player, memberRef, prop, valueRef);
    }

    /**
     * Get property value from member reference.
     */
    public static int getMemberProp(DirPlayer player, CastMemberRef memberRef, String prop) throws ScriptError {
        // Handle invalid member refs
        if (memberRef.castLib < 0 || memberRef.castMember < 0) {
            return getInvalidMemberProp(player, memberRef, prop);
        }

        CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            return getInvalidMemberProp(player, memberRef, prop);
        }

        switch (prop.toLowerCase()) {
            case "name":
                return player.allocDatum(Datum.ofString(member.name != null ? member.name : ""));
            case "membernum":
                return player.allocDatum(Datum.ofInt(member.number));
            case "number":
                return player.allocDatum(Datum.ofInt(getCastSlotNumber(memberRef.castLib, memberRef.castMember)));
            case "type":
                return player.allocDatum(Datum.ofSymbol(getMemberTypeSymbol(member)));
            case "castlibnum":
                return player.allocDatum(Datum.ofInt(memberRef.castLib));
            case "color":
                if (member.color != null) {
                    return player.allocDatum(Datum.ofColorRef(member.color));
                }
                return 0; // Void
            case "bgcolor":
                if (member.bgColor != null) {
                    return player.allocDatum(Datum.ofColorRef(member.bgColor));
                }
                return 0; // Void
            case "mediaready":
                return player.allocDatum(Datum.ofInt(1));
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("member"));
            default:
                // Try member-type specific properties
                return getMemberTypeProp(player, memberRef, member, prop);
        }
    }

    /**
     * Set property value on member reference.
     */
    public static void setMemberProp(DirPlayer player, CastMemberRef memberRef, String prop, int valueRef) throws ScriptError {
        if (memberRef.castLib < 0 || memberRef.castMember < 0) {
            throw new ScriptError("Cannot set property " + prop + " on invalid cast member");
        }

        CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            // Silently ignore setting props on non-existent members
            return;
        }

        Datum value = player.getDatum(valueRef);

        switch (prop.toLowerCase()) {
            case "name":
                member.name = value.stringValue();
                break;
            case "color":
                member.color = value.toColorRef();
                break;
            case "bgcolor":
                member.bgColor = value.toColorRef();
                break;
            default:
                // Try member-type specific properties
                setMemberTypeProp(player, memberRef, member, prop, valueRef);
                break;
        }
    }

    /**
     * Call handler on cast member ref datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "duplicate":
                return duplicate(player, datumRef, args);
            case "erase":
                return erase(player, datumRef, args);
            case "charpostoloc":
                return charPosToLoc(player, datumRef, args);
            case "getprop": {
                String prop = player.getDatum(args.get(0)).stringValue();
                int result = getProp(player, datumRef, prop);
                if (args.size() > 1) {
                    return SpriteHandlers.call(player, result, "getProp", args.subList(1, args.size()));
                }
                return result;
            }
            case "count":
                return count(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for cast member");
        }
    }

    // Handler implementations

    private static int duplicate(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        CastMemberRef memberRef = player.getDatum(datumRef).toMemberRef();
        if (args.isEmpty()) {
            throw new ScriptError("duplicate requires a destination slot number");
        }

        int destSlotNumber = player.getDatum(args.get(0)).intValue();
        CastMemberRef destRef = memberRefFromSlotNumber(destSlotNumber);

        CastMember srcMember = player.movie.castManager.findMemberByRef(memberRef);
        if (srcMember == null) {
            throw new ScriptError("Cannot duplicate non-existent cast member");
        }

        CastMember newMember = srcMember.clone();
        newMember.number = destRef.castMember;
        player.movie.castManager.insertMember(destRef.castLib, destRef.castMember, newMember);

        return player.allocDatum(Datum.ofInt(destSlotNumber));
    }

    private static int erase(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        CastMemberRef memberRef = player.getDatum(datumRef).toMemberRef();
        player.movie.castManager.removeMember(memberRef);
        return 0; // Void
    }

    private static int charPosToLoc(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        CastMemberRef memberRef = player.getDatum(datumRef).toMemberRef();
        CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null || member.memberType != CastMemberType.Text) {
            throw new ScriptError("charPosToLoc requires a text member");
        }

        int charPos = player.getDatum(args.get(0)).intValue();
        int charWidth = 7;  // Approximate
        int lineHeight = 16;  // Approximate

        String text = member.getText();
        int x, y;
        if (text == null || text.isEmpty() || charPos <= 0) {
            x = 0;
            y = 0;
        } else if (charPos > text.length()) {
            x = charWidth * text.length();
            y = lineHeight;
        } else {
            x = charWidth * (charPos - 1);
            y = lineHeight;
        }

        int xRef = player.allocDatum(Datum.ofInt(x));
        int yRef = player.allocDatum(Datum.ofInt(y));
        return player.allocDatum(Datum.ofPoint(xRef, yRef));
    }

    private static int count(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("count requires 1 argument");
        }

        CastMemberRef memberRef = player.getDatum(datumRef).toMemberRef();
        String countOf = player.getDatum(args.get(0)).stringValue();

        CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            throw new ScriptError("Cast member not found");
        }

        String text = member.getText();
        if (text == null) {
            throw new ScriptError("Member type does not support count operation");
        }

        char delimiter = player.getItemDelimiter();
        StringChunkType chunkType = StringChunkType.fromString(countOf);
        int count = StringChunkHandlers.resolveChunkCount(text, chunkType, delimiter);

        return player.allocDatum(Datum.ofInt(count));
    }

    // Helper methods

    private static int getInvalidMemberProp(DirPlayer player, CastMemberRef memberRef, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "name":
                return player.allocDatum(Datum.ofString(""));
            case "number":
            case "membernum":
            case "castlibnum":
                return player.allocDatum(Datum.ofInt(-1));
            case "type":
                return player.allocDatum(Datum.ofString("empty"));
            case "width":
            case "height":
            case "rect":
            case "duration":
            case "image":
                return 0; // Void
            case "regpoint": {
                int x = player.allocDatum(Datum.ofInt(0));
                int y = player.allocDatum(Datum.ofInt(0));
                return player.allocDatum(Datum.ofPoint(x, y));
            }
            default:
                throw new ScriptError("Cannot get property " + prop + " of invalid cast member");
        }
    }

    private static String getMemberTypeSymbol(CastMember member) {
        if (member.memberType == null) {
            return "empty";
        }
        switch (member.memberType) {
            case Bitmap:
                return "bitmap";
            case Field:
                return "field";
            case Text:
                return "text";
            case Script:
                return "script";
            case Sound:
                return "sound";
            case Shape:
                return "shape";
            case Button:
                return "button";
            case DigitalVideo:
                return "digitalVideo";
            case FilmLoop:
                return "filmLoop";
            case Palette:
                return "palette";
            case Picture:
                return "picture";
            case Transition:
                return "transition";
            case Xtra:
                return "xtra";
            default:
                return "empty";
        }
    }

    private static int getMemberTypeProp(DirPlayer player, CastMemberRef memberRef, CastMember member, String prop) throws ScriptError {
        switch (member.memberType) {
            case Bitmap:
                return getBitmapMemberProp(player, member, prop);
            case Text:
            case Field:
                return getTextMemberProp(player, member, prop);
            case Sound:
                return getSoundMemberProp(player, member, prop);
            default:
                throw new ScriptError("Cannot get property " + prop + " for member type " + member.memberType);
        }
    }

    private static void setMemberTypeProp(DirPlayer player, CastMemberRef memberRef, CastMember member, String prop, int valueRef) throws ScriptError {
        switch (member.memberType) {
            case Bitmap:
                setBitmapMemberProp(player, member, prop, valueRef);
                break;
            case Text:
            case Field:
                setTextMemberProp(player, member, prop, valueRef);
                break;
            default:
                throw new ScriptError("Cannot set property " + prop + " for member type " + member.memberType);
        }
    }

    private static int getBitmapMemberProp(DirPlayer player, CastMember member, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "width":
                return player.allocDatum(Datum.ofInt(member.width));
            case "height":
                return player.allocDatum(Datum.ofInt(member.height));
            case "rect": {
                int left = player.allocDatum(Datum.ofInt(0));
                int top = player.allocDatum(Datum.ofInt(0));
                int right = player.allocDatum(Datum.ofInt(member.width));
                int bottom = player.allocDatum(Datum.ofInt(member.height));
                return player.allocDatum(Datum.ofRect(left, top, right, bottom));
            }
            case "regpoint": {
                int x = player.allocDatum(Datum.ofInt(member.regPointX));
                int y = player.allocDatum(Datum.ofInt(member.regPointY));
                return player.allocDatum(Datum.ofPoint(x, y));
            }
            case "image":
                if (member.bitmap != null) {
                    return player.allocDatum(Datum.ofBitmapRef(member.bitmap));
                }
                return 0; // Void
            case "depth":
                return player.allocDatum(Datum.ofInt(member.depth));
            case "usepalette":
                return player.allocDatum(Datum.ofInt(member.usePalette ? 1 : 0));
            default:
                throw new ScriptError("Cannot get bitmap property " + prop);
        }
    }

    private static void setBitmapMemberProp(DirPlayer player, CastMember member, String prop, int valueRef) throws ScriptError {
        Datum value = player.getDatum(valueRef);

        switch (prop.toLowerCase()) {
            case "regpoint":
                if (value.isPoint()) {
                    int[] pt = value.toPoint();
                    member.regPointX = player.getDatum(pt[0]).intValue();
                    member.regPointY = player.getDatum(pt[1]).intValue();
                } else {
                    throw new ScriptError("regPoint must be a point");
                }
                break;
            case "image":
                if (value.getType() == com.dirplayer.director.lingo.DatumType.BitmapRef) {
                    member.bitmap = value.toBitmapRef();
                    member.width = member.bitmap.getWidth();
                    member.height = member.bitmap.getHeight();
                }
                break;
            default:
                throw new ScriptError("Cannot set bitmap property " + prop);
        }
    }

    private static int getTextMemberProp(DirPlayer player, CastMember member, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "text":
                return player.allocDatum(Datum.ofString(member.getText() != null ? member.getText() : ""));
            case "width":
                return player.allocDatum(Datum.ofInt(member.width));
            case "height":
                return player.allocDatum(Datum.ofInt(member.height));
            case "rect": {
                int left = player.allocDatum(Datum.ofInt(0));
                int top = player.allocDatum(Datum.ofInt(0));
                int right = player.allocDatum(Datum.ofInt(member.width));
                int bottom = player.allocDatum(Datum.ofInt(member.height));
                return player.allocDatum(Datum.ofRect(left, top, right, bottom));
            }
            case "font":
                return player.allocDatum(Datum.ofString(member.font != null ? member.font : ""));
            case "fontsize":
                return player.allocDatum(Datum.ofInt(member.fontSize));
            case "fontstyle":
                return player.allocDatum(Datum.ofString(member.fontStyle != null ? member.fontStyle : "plain"));
            case "alignment":
                return player.allocDatum(Datum.ofString(member.alignment != null ? member.alignment : "left"));
            default:
                throw new ScriptError("Cannot get text property " + prop);
        }
    }

    private static void setTextMemberProp(DirPlayer player, CastMember member, String prop, int valueRef) throws ScriptError {
        Datum value = player.getDatum(valueRef);

        switch (prop.toLowerCase()) {
            case "text":
                member.setText(value.stringValue());
                break;
            case "width":
                member.width = value.intValue();
                break;
            case "height":
                member.height = value.intValue();
                break;
            case "font":
                member.font = value.stringValue();
                break;
            case "fontsize":
                member.fontSize = value.intValue();
                break;
            case "fontstyle":
                member.fontStyle = value.stringValue();
                break;
            case "alignment":
                member.alignment = value.stringValue();
                break;
            default:
                throw new ScriptError("Cannot set text property " + prop);
        }
    }

    private static int getSoundMemberProp(DirPlayer player, CastMember member, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "duration":
                return player.allocDatum(Datum.ofInt(member.duration));
            case "samplerate":
                return player.allocDatum(Datum.ofInt(member.sampleRate));
            case "samplesize":
                return player.allocDatum(Datum.ofInt(member.sampleSize));
            case "channelcount":
                return player.allocDatum(Datum.ofInt(member.channelCount));
            default:
                throw new ScriptError("Cannot get sound property " + prop);
        }
    }
}
