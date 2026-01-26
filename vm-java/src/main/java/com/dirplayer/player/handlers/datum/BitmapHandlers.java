package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BitmapRef;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.IntRect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handlers for bitmap datum operations.
 * Port of Rust BitmapDatumHandlers.
 */
public class BitmapHandlers {

    /**
     * Call handler on bitmap datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "fill":
                return fill(player, datumRef, args);
            case "draw":
                return draw(player, datumRef, args);
            case "setpixel":
                return setPixel(player, datumRef, args);
            case "duplicate":
                return duplicate(player, datumRef, args);
            case "copypixels":
                return copyPixels(player, datumRef, args);
            case "creatematte":
                return createMatte(player, datumRef, args);
            case "trimwhitespace":
                return trimWhitespace(player, datumRef, args);
            case "getpixel":
                return getPixel(player, datumRef, args);
            case "floodfill":
                return floodFill(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for bitmap datum");
        }
    }

    /**
     * Get a property from a bitmap.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);

        if (bitmap == null) {
            throw new ScriptError("Invalid bitmap reference");
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Datum result;
        switch (prop.toLowerCase()) {
            case "width":
                result = Datum.ofInt(width);
                break;
            case "height":
                result = Datum.ofInt(height);
                break;
            case "rect": {
                int x0 = player.allocDatum(Datum.ofInt(0));
                int y0 = player.allocDatum(Datum.ofInt(0));
                int w = player.allocDatum(Datum.ofInt(width));
                int h = player.allocDatum(Datum.ofInt(height));
                result = Datum.ofRect(x0, y0, w, h);
                break;
            }
            case "depth":
                result = Datum.ofInt(bitmap.getBitDepth());
                break;
            case "paletteref": {
                PaletteRef paletteRef = bitmap.getPaletteRef();
                if (paletteRef != null && paletteRef.isBuiltIn()) {
                    result = Datum.ofSymbol(paletteRef.getSymbolString());
                } else if (paletteRef != null) {
                    result = Datum.ofPaletteRef(paletteRef);
                } else {
                    result = Datum.ofSymbol("systemDefault");
                }
                break;
            }
            case "ilk":
                result = Datum.ofSymbol("image");
                break;
            default:
                throw new ScriptError("Cannot get bitmap property " + prop);
        }
        return player.allocDatum(result);
    }

    /**
     * Set a property on a bitmap.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();
        Datum value = player.getDatum(valueRef);

        switch (prop.toLowerCase()) {
            case "paletteref":
                if (value.isSymbol()) {
                    String symbol = value.symbolValue();
                    PaletteRef paletteRef = PaletteRef.fromSymbolString(symbol);
                    if (paletteRef == null) {
                        throw new ScriptError("Invalid built-in palette symbol");
                    }
                    Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);
                    if (bitmap != null) {
                        bitmap.setPaletteRef(paletteRef);
                    }
                } else if (value.isCastMemberRef()) {
                    Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);
                    if (bitmap != null) {
                        bitmap.setPaletteRef(PaletteRef.fromMember(value.toMemberRef()));
                    }
                } else {
                    throw new ScriptError("Cannot set paletteRef to datum of type " + value.typeStr());
                }
                break;
            default:
                throw new ScriptError("Cannot set bitmap property " + prop);
        }
    }

    /**
     * Get pixel color at position.
     */
    private static int getPixel(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);

        int x = player.getDatum(args.get(0)).intValue();
        int y = player.getDatum(args.get(1)).intValue();

        ColorRef color = bitmap.getPixelColorRef(x, y);
        return player.allocDatum(Datum.ofColorRef(color));
    }

    /**
     * Trim whitespace from bitmap.
     */
    private static int trimWhitespace(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);

        if (bitmap != null) {
            bitmap.trimWhitespace(player.movie.castManager.getPalettes());
        }
        return datumRef;
    }

    /**
     * Create a matte from the bitmap.
     */
    private static int createMatte(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        if (!args.isEmpty()) {
            throw new ScriptError("Invalid number of arguments for createMatte");
        }

        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);

        if (bitmap != null) {
            bitmap.createMatte(player.movie.castManager.getPalettes());
            Object matte = bitmap.getMatte();
            if (matte != null) {
                return player.allocDatum(Datum.ofMatte(matte));
            }
        }
        return 0; // Void
    }

    /**
     * Duplicate the bitmap.
     */
    private static int duplicate(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        return player.duplicateDatum(datumRef);
    }

    /**
     * Draw on the bitmap.
     */
    private static int draw(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();

        int[] rectRefs = player.getDatum(args.get(0)).toRect();
        int x1 = player.getDatum(rectRefs[0]).intValue();
        int y1 = player.getDatum(rectRefs[1]).intValue();
        int x2 = player.getDatum(rectRefs[2]).intValue();
        int y2 = player.getDatum(rectRefs[3]).intValue();

        List<int[]> drawMap = player.getDatum(args.get(1)).toPropList();

        // Get color property
        ColorRef colorRef = null;
        String shapeType = null;
        int blend = 100;

        for (int[] pair : drawMap) {
            Datum key = player.getDatum(pair[0]);
            Datum value = player.getDatum(pair[1]);
            String keyStr = key.isSymbol() ? key.symbolValue() : key.stringValue();

            if (keyStr.equalsIgnoreCase("color")) {
                colorRef = value.toColorRef();
            } else if (keyStr.equalsIgnoreCase("shapeType")) {
                shapeType = value.stringValue();
            } else if (keyStr.equalsIgnoreCase("blend")) {
                if (!value.isVoid()) {
                    blend = value.intValue();
                }
            }
        }

        if (colorRef == null) {
            throw new ScriptError("Missing color property for draw");
        }
        if (shapeType == null) {
            throw new ScriptError("Missing shapeType property for draw");
        }

        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);
        int[] resolvedColor = bitmap.resolveColorRef(colorRef, player.movie.castManager.getPalettes());

        if (shapeType.equalsIgnoreCase("rect")) {
            bitmap.strokeRect(x1, y1, x2, y2, resolvedColor, player.movie.castManager.getPalettes(), blend / 100.0f);
        } else {
            throw new ScriptError("Invalid shapeType for draw");
        }

        return datumRef;
    }

    /**
     * Set a pixel on the bitmap.
     */
    private static int setPixel(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);

        int x = player.getDatum(args.get(0)).intValue();
        int y = player.getDatum(args.get(1)).intValue();
        Datum colorDatum = player.getDatum(args.get(2));

        if (x < 0 || y < 0 || x >= bitmap.getWidth() || y >= bitmap.getHeight()) {
            return player.allocDatum(Datum.ofInt(0)); // false
        }

        if (colorDatum.isInt()) {
            if (bitmap.getBitDepth() != 8) {
                throw new ScriptError("Cannot set pixel with int color on non-8-bit bitmap");
            }
            int intValue = colorDatum.intValue();
            bitmap.setPixel(x, y, new int[]{intValue, intValue, intValue}, player.movie.castManager.getPalettes());
        } else {
            ColorRef colorRef = colorDatum.toColorRef();
            int[] color = bitmap.resolveColorRef(colorRef, player.movie.castManager.getPalettes());
            bitmap.setPixel(x, y, color, player.movie.castManager.getPalettes());
        }

        return player.allocDatum(Datum.ofInt(1)); // true
    }

    /**
     * Fill a rectangle on the bitmap.
     */
    private static int fill(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();

        int x1, y1, x2, y2;
        ColorRef colorRef;

        if (args.size() == 2) {
            int[] rectRefs = player.getDatum(args.get(0)).toRect();
            x1 = player.getDatum(rectRefs[0]).intValue();
            y1 = player.getDatum(rectRefs[1]).intValue();
            x2 = player.getDatum(rectRefs[2]).intValue();
            y2 = player.getDatum(rectRefs[3]).intValue();
            colorRef = player.getDatum(args.get(1)).toColorRef();
        } else if (args.size() == 5) {
            x1 = player.getDatum(args.get(0)).intValue();
            y1 = player.getDatum(args.get(1)).intValue();
            x2 = player.getDatum(args.get(2)).intValue();
            y2 = player.getDatum(args.get(3)).intValue();
            colorRef = player.getDatum(args.get(4)).toColorRef();
        } else {
            throw new ScriptError("Invalid number of arguments for fill");
        }

        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);
        int[] color = bitmap.resolveColorRef(colorRef, player.movie.castManager.getPalettes());
        bitmap.fillRect(x1, y1, x2, y2, color, player.movie.castManager.getPalettes(), 1.0f);

        return datumRef;
    }

    /**
     * Copy pixels from one bitmap to another.
     */
    private static int copyPixels(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        BitmapRef dstBitmapRef = player.getDatum(datumRef).toBitmapRef();
        Datum srcDatum = player.getDatum(args.get(0));

        // If source is void or 0, return datum unchanged
        if (srcDatum.isVoid() || (srcDatum.isNumber() && srcDatum.intValue() == 0)) {
            return datumRef;
        }

        BitmapRef srcBitmapRef = srcDatum.toBitmapRef();
        Datum destRectOrQuad = player.getDatum(args.get(1));

        int[] srcRectRefs = player.getDatum(args.get(2)).toRect();
        int sx1 = player.getDatum(srcRectRefs[0]).intValue();
        int sy1 = player.getDatum(srcRectRefs[1]).intValue();
        int sx2 = player.getDatum(srcRectRefs[2]).intValue();
        int sy2 = player.getDatum(srcRectRefs[3]).intValue();

        // Parse optional parameter list
        Map<String, Datum> paramListConcrete = new HashMap<>();
        if (args.size() > 3) {
            Datum paramList = player.getDatum(args.get(3));
            if (paramList.isPropList()) {
                for (int[] pair : paramList.toPropList()) {
                    String key = player.getDatum(pair[0]).stringValue();
                    Datum value = player.getDatum(pair[1]);
                    paramListConcrete.put(key, value);
                }
            }
        }

        // Parse destination rect or quad
        IntRect destRect;
        if (destRectOrQuad.isRect()) {
            int[] rectRefs = destRectOrQuad.toRect();
            int dx1 = player.getDatum(rectRefs[0]).intValue();
            int dy1 = player.getDatum(rectRefs[1]).intValue();
            int dx2 = player.getDatum(rectRefs[2]).intValue();
            int dy2 = player.getDatum(rectRefs[3]).intValue();
            destRect = new IntRect(dx1, dy1, dx2, dy2);
        } else if (destRectOrQuad.isList()) {
            // Quad: list of 4 points
            List<Integer> list = destRectOrQuad.toList();
            int[] p1 = player.getDatum(list.get(0)).toPoint();
            int[] p2 = player.getDatum(list.get(1)).toPoint();
            int[] p3 = player.getDatum(list.get(2)).toPoint();
            int[] p4 = player.getDatum(list.get(3)).toPoint();

            int x1 = player.getDatum(p1[0]).intValue();
            int y1 = player.getDatum(p1[1]).intValue();
            int x2 = player.getDatum(p2[0]).intValue();
            int y2 = player.getDatum(p2[1]).intValue();
            int x3 = player.getDatum(p3[0]).intValue();
            int y3 = player.getDatum(p3[1]).intValue();
            int x4 = player.getDatum(p4[0]).intValue();
            int y4 = player.getDatum(p4[1]).intValue();

            destRect = IntRect.fromQuad(x1, y1, x2, y2, x3, y3, x4, y4);
        } else {
            throw new ScriptError("Invalid destRect for copyPixels");
        }

        Bitmap srcBitmap = player.bitmapManager.getBitmap(srcBitmapRef);
        Bitmap dstBitmap = player.bitmapManager.getBitmap(dstBitmapRef);

        dstBitmap.copyPixels(
            player.movie.castManager.getPalettes(),
            srcBitmap,
            destRect,
            new IntRect(sx1, sy1, sx2, sy2),
            paramListConcrete,
            player.movie.score
        );

        return datumRef;
    }

    /**
     * Flood fill starting at a point.
     */
    private static int floodFill(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        if (args.size() != 2) {
            throw new ScriptError("floodFill requires 2 arguments");
        }

        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();

        int[] pointRefs = player.getDatum(args.get(0)).toPoint();
        int x = player.getDatum(pointRefs[0]).intValue();
        int y = player.getDatum(pointRefs[1]).intValue();

        ColorRef colorRef = player.getDatum(args.get(1)).toColorRef();

        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);
        int[] targetRgb = bitmap.resolveColorRef(colorRef, player.movie.castManager.getPalettes());

        bitmap.floodFill(x, y, targetRgb, player.movie.castManager.getPalettes());

        return 0; // Void
    }
}
