package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.List;

/**
 * Handlers for color datum operations.
 * Port of Rust ColorDatumHandlers.
 */
public class ColorHandlers {

    /**
     * Get a property from a color.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        ColorRef colorRef = player.getDatum(datumRef).toColorRef();

        switch (prop.toLowerCase()) {
            case "red":
                if (colorRef.isRgb()) {
                    return player.allocDatum(Datum.ofInt(colorRef.getRed()));
                } else {
                    // Palette index - resolve to RGB
                    int[] rgb = player.resolvePaletteColor(colorRef.getPaletteIndex());
                    return player.allocDatum(Datum.ofInt(rgb[0]));
                }
            case "green":
                if (colorRef.isRgb()) {
                    return player.allocDatum(Datum.ofInt(colorRef.getGreen()));
                } else {
                    int[] rgb = player.resolvePaletteColor(colorRef.getPaletteIndex());
                    return player.allocDatum(Datum.ofInt(rgb[1]));
                }
            case "blue":
                if (colorRef.isRgb()) {
                    return player.allocDatum(Datum.ofInt(colorRef.getBlue()));
                } else {
                    int[] rgb = player.resolvePaletteColor(colorRef.getPaletteIndex());
                    return player.allocDatum(Datum.ofInt(rgb[2]));
                }
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("color"));
            case "paletteindex":
                if (colorRef.isPaletteIndex()) {
                    return player.allocDatum(Datum.ofInt(colorRef.getPaletteIndex()));
                } else {
                    return player.allocDatum(Datum.ofInt(-1)); // RGB colors don't have palette index
                }
            case "colortype":
                if (colorRef.isRgb()) {
                    return player.allocDatum(Datum.ofSymbol("rgb"));
                } else {
                    return player.allocDatum(Datum.ofSymbol("paletteIndex"));
                }
            default:
                throw new ScriptError("Cannot get color property " + prop);
        }
    }

    /**
     * Set a property on a color.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        ColorRef colorRef = player.getDatum(datumRef).toColorRef();
        int value = player.getDatum(valueRef).intValue() & 0xFF;

        switch (prop.toLowerCase()) {
            case "red":
                if (colorRef.isRgb()) {
                    colorRef.setRed(value);
                } else {
                    throw new ScriptError("Cannot set red on palette index color");
                }
                break;
            case "green":
                if (colorRef.isRgb()) {
                    colorRef.setGreen(value);
                } else {
                    throw new ScriptError("Cannot set green on palette index color");
                }
                break;
            case "blue":
                if (colorRef.isRgb()) {
                    colorRef.setBlue(value);
                } else {
                    throw new ScriptError("Cannot set blue on palette index color");
                }
                break;
            default:
                throw new ScriptError("Cannot set color property " + prop);
        }
    }

    /**
     * Call handler on color datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "getprop":
                String prop = player.getDatum(args.get(0)).stringValue();
                return getProp(player, datumRef, prop);
            default:
                throw new ScriptError("No handler " + handlerName + " for color");
        }
    }
}
