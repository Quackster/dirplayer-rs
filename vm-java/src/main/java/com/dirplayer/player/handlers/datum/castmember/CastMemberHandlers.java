package com.dirplayer.player.handlers.datum.castmember;

import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.cast.CastMemberType;
import com.dirplayer.player.cast.BitmapMember;
import com.dirplayer.player.cast.FieldMember;
import com.dirplayer.player.cast.FilmLoopMember;
import com.dirplayer.player.cast.FontMember;
import com.dirplayer.player.cast.SoundMember;
import com.dirplayer.player.cast.TextMember;

import java.util.List;

/**
 * Main dispatcher for cast member-specific property handlers.
 * Routes property get/set operations to the appropriate type-specific handler.
 * Port of Rust cast_member module dispatcher.
 */
public class CastMemberHandlers {

    /**
     * Get a property from a cast member, dispatching to the appropriate type handler.
     *
     * @param player The DirPlayer instance
     * @param memberRef The cast member reference
     * @param prop The property name to get
     * @return DatumRef for the property value
     * @throws ScriptError if the property cannot be retrieved
     */
    public static int getTypeProp(DirPlayer player, CastMemberRef memberRef, String prop) throws ScriptError {
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            throw new ScriptError("Cast member not found");
        }

        CastMemberType memberType = getMemberType(member);
        if (memberType == null) {
            throw new ScriptError("Cannot get property " + prop + " for unknown member type");
        }

        switch (memberType) {
            case Bitmap:
                return BitmapMemberHandlers.getProp(player, memberRef, prop);

            case Field:
                return FieldMemberHandlers.getProp(player, memberRef, prop);

            case Text:
                return TextMemberHandlers.getProp(player, memberRef, prop);

            case FilmLoop:
                return FilmLoopMemberHandlers.getProp(player, memberRef, prop);

            case Sound:
                return SoundMemberHandlers.getProp(player, memberRef, prop);

            case Font:
                return FontMemberHandlers.getProp(player, memberRef, prop);

            default:
                throw new ScriptError("Cannot get property " + prop + " for member type " + memberType);
        }
    }

    /**
     * Set a property on a cast member, dispatching to the appropriate type handler.
     *
     * @param player The DirPlayer instance
     * @param memberRef The cast member reference
     * @param prop The property name to set
     * @param valueRef DatumRef for the value to set
     * @throws ScriptError if the property cannot be set
     */
    public static void setTypeProp(DirPlayer player, CastMemberRef memberRef, String prop, int valueRef) throws ScriptError {
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            throw new ScriptError("Cast member not found");
        }

        CastMemberType memberType = getMemberType(member);
        if (memberType == null) {
            throw new ScriptError("Cannot set property " + prop + " for unknown member type");
        }

        switch (memberType) {
            case Bitmap:
                BitmapMemberHandlers.setProp(player, memberRef, prop, valueRef);
                break;

            case Field:
                FieldMemberHandlers.setProp(player, memberRef, prop, valueRef);
                break;

            case Text:
                TextMemberHandlers.setProp(player, memberRef, prop, valueRef);
                break;

            case Font:
                FontMemberHandlers.setProp(player, memberRef, prop, valueRef);
                break;

            case FilmLoop:
            case Sound:
                // These types don't support setting properties
                throw new ScriptError("Cannot set property " + prop + " for member type " + memberType);

            default:
                throw new ScriptError("Cannot set property " + prop + " for member type " + memberType);
        }
    }

    /**
     * Call a handler on a cast member, dispatching to the appropriate type handler.
     *
     * @param player The DirPlayer instance
     * @param datumRef DatumRef for the cast member
     * @param handlerName The handler name to call
     * @param args List of argument DatumRefs
     * @return DatumRef for the result
     * @throws ScriptError if the handler cannot be called
     */
    public static int callTypeHandler(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        CastMemberRef memberRef = player.getDatum(datumRef).toMemberRef();
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            throw new ScriptError("Cast member not found");
        }

        CastMemberType memberType = getMemberType(member);
        if (memberType == null) {
            throw new ScriptError("No handler " + handlerName + " for unknown member type");
        }

        switch (memberType) {
            case Field:
                return FieldMemberHandlers.call(player, datumRef, handlerName, args);

            case Text:
                return TextMemberHandlers.call(player, datumRef, handlerName, args);

            case Font:
                return FontMemberHandlers.call(player, datumRef, handlerName, args);

            default:
                throw new ScriptError("No handler " + handlerName + " for member type " + memberType);
        }
    }

    /**
     * Check if a member type supports a given property for getting.
     *
     * @param memberType The cast member type
     * @param prop The property name
     * @return true if the property is supported
     */
    public static boolean supportsGetProp(CastMemberType memberType, String prop) {
        if (memberType == null) return false;

        String propLower = prop.toLowerCase();

        switch (memberType) {
            case Bitmap:
                return isBitmapProp(propLower);
            case Field:
                return isFieldProp(propLower);
            case Text:
                return isTextProp(propLower);
            case FilmLoop:
                return isFilmLoopProp(propLower);
            case Sound:
                return isSoundProp(propLower);
            case Font:
                return isFontProp(propLower);
            default:
                return false;
        }
    }

    /**
     * Check if a member type supports a given property for setting.
     *
     * @param memberType The cast member type
     * @param prop The property name
     * @return true if the property can be set
     */
    public static boolean supportsSetProp(CastMemberType memberType, String prop) {
        if (memberType == null) return false;

        String propLower = prop.toLowerCase();

        switch (memberType) {
            case Bitmap:
                return isBitmapSettableProp(propLower);
            case Field:
                return isFieldSettableProp(propLower);
            case Text:
                return isTextSettableProp(propLower);
            case Font:
                return isFontSettableProp(propLower);
            default:
                return false;
        }
    }

    // Helper methods

    private static CastMemberType getMemberType(com.dirplayer.player.CastMember member) {
        if (member.specificData instanceof BitmapMember) {
            return CastMemberType.Bitmap;
        }
        if (member.specificData instanceof FieldMember) {
            return CastMemberType.Field;
        }
        if (member.specificData instanceof TextMember) {
            return CastMemberType.Text;
        }
        if (member.specificData instanceof FilmLoopMember) {
            return CastMemberType.FilmLoop;
        }
        if (member.specificData instanceof SoundMember) {
            return CastMemberType.Sound;
        }
        if (member.specificData instanceof FontMember) {
            return CastMemberType.Font;
        }
        return null;
    }

    private static boolean isBitmapProp(String prop) {
        switch (prop) {
            case "width":
            case "height":
            case "image":
            case "paletteref":
            case "regpoint":
            case "rect":
            case "depth":
                return true;
            default:
                return false;
        }
    }

    private static boolean isBitmapSettableProp(String prop) {
        switch (prop) {
            case "image":
            case "regpoint":
            case "paletteref":
            case "palette":
                return true;
            default:
                return false;
        }
    }

    private static boolean isFieldProp(String prop) {
        switch (prop) {
            case "text":
            case "font":
            case "fontsize":
            case "fontstyle":
            case "width":
            case "alignment":
            case "wordwrap":
            case "fixedlinespace":
            case "topspacing":
            case "boxtype":
            case "antialias":
            case "autotab":
            case "editable":
            case "border":
            case "backcolor":
            case "rect":
            case "height":
            case "image":
                return true;
            default:
                return false;
        }
    }

    private static boolean isFieldSettableProp(String prop) {
        switch (prop) {
            case "text":
            case "rect":
            case "alignment":
            case "wordwrap":
            case "width":
            case "font":
            case "fontsize":
            case "fontstyle":
            case "fixedlinespace":
            case "topspacing":
            case "boxtype":
            case "antialias":
            case "autotab":
            case "editable":
            case "border":
            case "backcolor":
                return true;
            default:
                return false;
        }
    }

    private static boolean isTextProp(String prop) {
        switch (prop) {
            case "text":
            case "alignment":
            case "wordwrap":
            case "width":
            case "font":
            case "fontsize":
            case "fontstyle":
            case "fixedlinespace":
            case "topspacing":
            case "boxtype":
            case "antialias":
            case "rect":
            case "height":
            case "image":
                return true;
            default:
                return false;
        }
    }

    private static boolean isTextSettableProp(String prop) {
        switch (prop) {
            case "text":
            case "alignment":
            case "wordwrap":
            case "width":
            case "font":
            case "fontsize":
            case "fontstyle":
            case "fixedlinespace":
            case "topspacing":
            case "boxtype":
            case "antialias":
            case "rect":
                return true;
            default:
                return false;
        }
    }

    private static boolean isFilmLoopProp(String prop) {
        switch (prop) {
            case "rect":
            case "width":
            case "height":
                return true;
            default:
                return false;
        }
    }

    private static boolean isSoundProp(String prop) {
        switch (prop) {
            case "duration":
            case "samplerate":
            case "samplesize":
            case "channelcount":
            case "samplecount":
                return true;
            default:
                return false;
        }
    }

    private static boolean isFontProp(String prop) {
        switch (prop) {
            case "text":
            case "previewtext":
            case "previewhtml":
            case "fontstyle":
            case "name":
            case "size":
            case "alignment":
            case "wordwrap":
            case "width":
            case "font":
            case "fontsize":
            case "fixedlinespace":
            case "topspacing":
            case "boxtype":
            case "antialias":
            case "rect":
            case "height":
            case "image":
                return true;
            default:
                return false;
        }
    }

    private static boolean isFontSettableProp(String prop) {
        switch (prop) {
            case "text":
            case "html":
            case "fixedlinespace":
            case "alignment":
                return true;
            default:
                return false;
        }
    }
}
