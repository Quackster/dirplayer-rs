package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BitmapDrawing;
import com.dirplayer.player.bitmap.BitmapRef;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.ColorRef;

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
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);

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
                PaletteRef paletteRef = bitmap.paletteRef;
                if (paletteRef != null && paletteRef.isBuiltIn()) {
                    result = Datum.ofSymbol(paletteRef.getBuiltIn().toSymbol());
                } else if (paletteRef != null) {
                    result = Datum.ofCastMember(paletteRef.getMemberRef());
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
                    BuiltInPalette builtInPalette = BuiltInPalette.fromSymbol(symbol);
                    if (builtInPalette == null) {
                        throw new ScriptError("Invalid built-in palette symbol");
                    }
                    PaletteRef paletteRef = PaletteRef.ofBuiltIn(builtInPalette);
                    Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);
                    if (bitmap != null) {
                        bitmap.paletteRef = paletteRef;
                    }
                } else if (value.isCastMemberRef()) {
                    Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);
                    if (bitmap != null) {
                        bitmap.paletteRef = PaletteRef.ofMember(value.toMemberRef());
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
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);

        int x = player.getDatum(args.get(0)).intValue();
        int y = player.getDatum(args.get(1)).intValue();

        ColorRef color = BitmapDrawing.getPixelColorRef(bitmap, x, y);
        return player.allocDatum(Datum.ofColorRef(color));
    }

    /**
     * Trim whitespace from bitmap.
     */
    private static int trimWhitespace(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);

        if (bitmap != null) {
            BitmapDrawing.trimWhitespace(bitmap, player.movie.castManager.palettes());
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
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);

        if (bitmap != null) {
            bitmap.createMatte(player.movie.castManager.palettes());
            // createMatte modifies the bitmap in place, returns void
        }
        return 0; // Void
    }

    /**
     * Duplicate the bitmap.
     */
    private static int duplicate(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        BitmapRef bitmapRef = player.getDatum(datumRef).toBitmapRef();
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);

        if (bitmap == null) {
            throw new ScriptError("Invalid bitmap reference");
        }

        // Create a copy of the bitmap
        Bitmap copiedBitmap = bitmap.copy();

        // Store the new bitmap in the bitmap manager
        int newBitmapId = player.bitmapManager.addBitmap(copiedBitmap);

        // Create a new BitmapRef for the duplicated bitmap
        BitmapRef newBitmapRef = new BitmapRef(newBitmapId, copiedBitmap.width, copiedBitmap.height, copiedBitmap.bitDepth);

        // Return the new bitmap datum
        return player.allocDatum(Datum.ofBitmapRef(newBitmapRef));
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

        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);
        int[] resolvedColor = BitmapDrawing.resolveColorRef(player.movie.castManager.palettes(), colorRef, bitmap.paletteRef, bitmap.getBitDepth());

        if (shapeType.equalsIgnoreCase("rect")) {
            bitmap.strokeRect(x1, y1, x2, y2, resolvedColor[0], resolvedColor[1], resolvedColor[2], player.movie.castManager.palettes(), blend / 100.0f);
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
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);

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
            BitmapDrawing.setPixelColor(bitmap, x, y, new int[]{intValue, intValue, intValue}, player.movie.castManager.palettes());
        } else {
            ColorRef colorRef = colorDatum.toColorRef();
            int[] color = BitmapDrawing.resolveColorRef(player.movie.castManager.palettes(), colorRef, bitmap.paletteRef, bitmap.getBitDepth());
            BitmapDrawing.setPixelColor(bitmap, x, y, color, player.movie.castManager.palettes());
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

        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);
        int[] color = BitmapDrawing.resolveColorRef(player.movie.castManager.palettes(), colorRef, bitmap.paletteRef, bitmap.getBitDepth());
        bitmap.fillRect(x1, y1, x2, y2, color[0], color[1], color[2], player.movie.castManager.palettes(), 1.0f);

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
        Map<String, Object> paramListConcrete = new HashMap<>();
        if (args.size() > 3) {
            Datum paramList = player.getDatum(args.get(3));
            if (paramList.isPropList()) {
                for (int[] pair : paramList.toPropList()) {
                    String key = player.getDatum(pair[0]).stringValue();
                    Datum value = player.getDatum(pair[1]);
                    // Convert Datum to appropriate Java type
                    if (value.isInt()) {
                        paramListConcrete.put(key, value.intValue());
                    } else if (value.isFloat()) {
                        paramListConcrete.put(key, value.floatValue());
                    } else if (value.isColorRef()) {
                        paramListConcrete.put(key, value.toColorRef());
                    } else {
                        paramListConcrete.put(key, value);
                    }
                }
            }
        }

        // Parse destination rect or quad
        com.dirplayer.rendering.IntRect destRect;
        if (destRectOrQuad.isRect()) {
            int[] rectRefs = destRectOrQuad.toRect();
            int dx1 = player.getDatum(rectRefs[0]).intValue();
            int dy1 = player.getDatum(rectRefs[1]).intValue();
            int dx2 = player.getDatum(rectRefs[2]).intValue();
            int dy2 = player.getDatum(rectRefs[3]).intValue();
            destRect = com.dirplayer.rendering.IntRect.from(dx1, dy1, dx2, dy2);
        } else if (destRectOrQuad.isList()) {
            // Quad: list of 4 points - for now just get bounding rect
            List<Integer> list = destRectOrQuad.toList();
            int[] p1Refs = player.getDatum(list.get(0)).toPoint();
            int[] p2Refs = player.getDatum(list.get(1)).toPoint();
            int[] p3Refs = player.getDatum(list.get(2)).toPoint();
            int[] p4Refs = player.getDatum(list.get(3)).toPoint();

            int x1 = player.getDatum(p1Refs[0]).intValue();
            int y1 = player.getDatum(p1Refs[1]).intValue();
            int x2 = player.getDatum(p2Refs[0]).intValue();
            int y2 = player.getDatum(p2Refs[1]).intValue();
            int x3 = player.getDatum(p3Refs[0]).intValue();
            int y3 = player.getDatum(p3Refs[1]).intValue();
            int x4 = player.getDatum(p4Refs[0]).intValue();
            int y4 = player.getDatum(p4Refs[1]).intValue();

            // Get bounding box of the quad
            int minX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
            int minY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
            int maxX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
            int maxY = Math.max(Math.max(y1, y2), Math.max(y3, y4));

            destRect = com.dirplayer.rendering.IntRect.from(minX, minY, maxX, maxY);
        } else {
            throw new ScriptError("Invalid destRect for copyPixels");
        }

        Bitmap srcBitmap = player.bitmapManager.getBitmap(srcBitmapRef.bitmapId);
        Bitmap dstBitmap = player.bitmapManager.getBitmap(dstBitmapRef.bitmapId);

        BitmapDrawing.copyPixels(
            dstBitmap,
            player.movie.castManager.palettes(),
            srcBitmap,
            destRect,
            com.dirplayer.rendering.IntRect.from(sx1, sy1, sx2, sy2),
            paramListConcrete
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

        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);
        int[] targetRgb = BitmapDrawing.resolveColorRef(player.movie.castManager.palettes(), colorRef, bitmap.paletteRef, bitmap.getBitDepth());

        BitmapDrawing.floodFill(bitmap, x, y, targetRgb, player.movie.castManager.palettes());

        return 0; // Void
    }
}
