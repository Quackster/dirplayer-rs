package com.dirplayer.player.handlers.datum.castmember;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.StringChunkType;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.FontManager;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.bitmap.PaletteMap;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.cast.FieldMember;
import com.dirplayer.player.handlers.datum.StringChunkHandlers;
import com.dirplayer.rendering.CopyPixelsParams;

import java.util.List;

/**
 * Handlers for field cast member datum operations.
 * Port of Rust FieldMemberHandlers.
 */
public class FieldMemberHandlers {

    /**
     * Call a handler on a field cast member.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        CastMemberRef memberRef = player.getDatum(datumRef).toMemberRef();

        switch (handlerName.toLowerCase()) {
            case "count": {
                if (args.size() != 1) {
                    throw new ScriptError("count requires 1 argument");
                }
                FieldMember field = getFieldMember(player, memberRef);
                if (field == null) {
                    throw new ScriptError("Invalid field member");
                }
                String countOf = player.getDatum(args.get(0)).stringValue();
                char delimiter = player.getItemDelimiter();
                StringChunkType chunkType = StringChunkType.fromString(countOf);
                int count = StringChunkHandlers.resolveChunkCount(field.text, chunkType, delimiter);
                return player.allocDatum(Datum.ofInt(count));
            }

            case "setcontents": {
                if (args.size() != 1) {
                    throw new ScriptError("setContents requires 1 argument");
                }
                String newContents = player.getDatum(args.get(0)).stringValue();
                FieldMember field = getFieldMemberMut(player, memberRef);
                if (field != null) {
                    field.text = newContents;
                }
                return 0; // Void
            }

            default:
                throw new ScriptError("No handler " + handlerName + " for field member type");
        }
    }

    /**
     * Get a property from a field cast member.
     */
    public static int getProp(DirPlayer player, CastMemberRef memberRef, String prop) throws ScriptError {
        FieldMember field = getFieldMember(player, memberRef);
        if (field == null) {
            throw new ScriptError("Invalid field member");
        }

        switch (prop.toLowerCase()) {
            case "text":
                return player.allocDatum(Datum.ofString(field.text));

            case "font":
                return player.allocDatum(Datum.ofString(field.font));

            case "fontsize":
                return player.allocDatum(Datum.ofInt(field.fontSize));

            case "fontstyle":
                return player.allocDatum(Datum.ofString(field.fontStyle));

            case "width":
                return player.allocDatum(Datum.ofInt(field.width));

            case "alignment":
                return player.allocDatum(Datum.ofString(field.alignment));

            case "wordwrap":
                return player.allocDatum(Datum.ofBool(field.wordWrap));

            case "fixedlinespace":
                return player.allocDatum(Datum.ofInt(field.fixedLineSpace));

            case "topspacing":
                return player.allocDatum(Datum.ofInt(field.topSpacing));

            case "boxtype":
                return player.allocDatum(Datum.ofString(field.boxType));

            case "antialias":
                return player.allocDatum(Datum.ofBool(field.antiAlias));

            case "autotab":
                return player.allocDatum(Datum.ofBool(field.autoTab));

            case "editable":
                return player.allocDatum(Datum.ofBool(field.editable));

            case "border":
                return player.allocDatum(Datum.ofInt(field.border));

            case "backcolor":
                return player.allocDatum(Datum.ofInt(field.backColor));

            case "rect":
            case "height":
            case "image": {
                // Calculate dimensions based on text and font
                // For now, use simplified calculations
                int width = field.width > 0 ? field.width : 100;
                int height = calculateTextHeight(field);

                switch (prop.toLowerCase()) {
                    case "rect": {
                        int leftRef = player.allocDatum(Datum.ofInt(0));
                        int topRef = player.allocDatum(Datum.ofInt(0));
                        int rightRef = player.allocDatum(Datum.ofInt(width));
                        int bottomRef = player.allocDatum(Datum.ofInt(height));
                        return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
                    }
                    case "height":
                        return player.allocDatum(Datum.ofInt(height));
                    case "image":
                        return generateFieldImage(player, field, width, height);
                    default:
                        throw new ScriptError("Unexpected property: " + prop);
                }
            }

            default:
                throw new ScriptError("Cannot get castMember property " + prop + " for field");
        }
    }

    /**
     * Set a property on a field cast member.
     */
    public static void setProp(DirPlayer player, CastMemberRef memberRef, String prop, int valueRef) throws ScriptError {
        Datum value = player.getDatum(valueRef);
        FieldMember field = getFieldMemberMut(player, memberRef);
        if (field == null) {
            throw new ScriptError("Invalid field member");
        }

        switch (prop.toLowerCase()) {
            case "text":
                field.text = value.stringValue();
                break;

            case "rect": {
                if (!value.isRect()) {
                    throw new ScriptError("rect must be a Rect");
                }
                int[] rectRefs = value.toRect();
                int x2 = player.getDatum(rectRefs[2]).intValue();
                field.width = x2;
                break;
            }

            case "alignment":
                field.alignment = value.stringValue();
                break;

            case "wordwrap":
                field.wordWrap = value.boolValue();
                break;

            case "width":
                field.width = value.intValue();
                break;

            case "font":
                field.font = value.stringValue();
                break;

            case "fontsize":
                field.fontSize = value.intValue();
                break;

            case "fontstyle":
                field.fontStyle = value.stringValue();
                break;

            case "fixedlinespace":
                field.fixedLineSpace = value.intValue();
                break;

            case "topspacing":
                field.topSpacing = value.intValue();
                break;

            case "boxtype":
                field.boxType = value.stringValue();
                break;

            case "antialias":
                field.antiAlias = value.boolValue();
                break;

            case "autotab":
                field.autoTab = value.boolValue();
                break;

            case "editable":
                field.editable = value.boolValue();
                break;

            case "border":
                field.border = value.intValue();
                break;

            case "backcolor":
                field.backColor = value.intValue();
                break;

            default:
                throw new ScriptError("Cannot set castMember prop " + prop + " for field");
        }
    }

    // Helper methods

    private static FieldMember getFieldMember(DirPlayer player, CastMemberRef memberRef) {
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null || member.specificData == null) {
            return null;
        }
        if (member.specificData instanceof FieldMember) {
            return (FieldMember) member.specificData;
        }
        return null;
    }

    private static FieldMember getFieldMemberMut(DirPlayer player, CastMemberRef memberRef) {
        return getFieldMember(player, memberRef);
    }

    private static int calculateTextHeight(FieldMember field) {
        if (field.text == null || field.text.isEmpty()) {
            return field.fontSize > 0 ? field.fontSize : 12;
        }
        // Approximate line count
        int lineCount = 1;
        for (char c : field.text.toCharArray()) {
            if (c == '\n' || c == '\r') {
                lineCount++;
            }
        }
        int lineHeight = field.fixedLineSpace > 0 ? field.fixedLineSpace : (field.fontSize > 0 ? field.fontSize + 4 : 16);
        return lineCount * lineHeight + field.topSpacing;
    }

    /**
     * Generate a bitmap image from field text using the font manager.
     */
    private static int generateFieldImage(DirPlayer player, FieldMember field, int width, int height) throws ScriptError {
        // Create a new bitmap for the field text
        com.dirplayer.player.bitmap.Bitmap bitmap = new com.dirplayer.player.bitmap.Bitmap(
            width, height, 32, 8, 0,
            com.dirplayer.player.bitmap.PaletteRef.ofBuiltIn(com.dirplayer.player.bitmap.BuiltInPalette.SystemWin)
        );

        // Fill with transparent background
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = (y * width + x) * 4;
                if (index + 3 < bitmap.getData().length) {
                    bitmap.getData()[index] = 0;     // R
                    bitmap.getData()[index + 1] = 0; // G
                    bitmap.getData()[index + 2] = 0; // B
                    bitmap.getData()[index + 3] = 0; // A (transparent)
                }
            }
        }

        // Try to get the font for rendering text
        String fontName = field.font != null && !field.font.isEmpty() ? field.font : "system";
        com.dirplayer.player.FontManager.BitmapFont font = null;

        if (player.fontManager != null) {
            font = player.fontManager.getFont(fontName);

            if (font == null) {
                font = player.fontManager.getSystemFont();
            }
        }

        // If we have a font, render the text
        if (font != null && field.text != null && !field.text.isEmpty()) {
            com.dirplayer.player.bitmap.Bitmap fontBitmap = player.bitmapManager.getBitmap(font.bitmapRef);
            if (fontBitmap != null) {
                com.dirplayer.player.bitmap.PaletteMap palettes = player.movie.castManager.palettes();

                com.dirplayer.rendering.CopyPixelsParams params = new com.dirplayer.rendering.CopyPixelsParams();
                params.blend = 100;
                params.ink = 36;
                params.color = bitmap.getFgColorRef();
                params.bgColor = com.dirplayer.player.ColorRef.paletteIndex(0);
                params.maskImage = null;
                params.isTextRendering = true;
                params.rotation = 0.0f;
                params.sprite = null;
                params.originalDstRect = null;

                bitmap.drawText(
                    field.text,
                    font,
                    fontBitmap,
                    0,
                    field.topSpacing,
                    params,
                    palettes,
                    field.fixedLineSpace,
                    field.topSpacing
                );
            }
        }

        // Store the bitmap and return a reference
        int bitmapRef = player.bitmapManager.addBitmap(bitmap);
        return player.allocDatum(Datum.ofBitmapRef(bitmapRef));
    }
}
