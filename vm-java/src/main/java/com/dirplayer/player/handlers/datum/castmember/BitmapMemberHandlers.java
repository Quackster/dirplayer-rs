package com.dirplayer.player.handlers.datum.castmember;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.cast.BitmapMember;

/**
 * Handlers for bitmap cast member datum operations.
 * Port of Rust BitmapMemberHandlers.
 */
public class BitmapMemberHandlers {

    /**
     * Get a property from a bitmap cast member.
     */
    public static int getProp(DirPlayer player, CastMemberRef memberRef, String prop) throws ScriptError {
        BitmapMember bitmapMember = getBitmapMember(player, memberRef);
        if (bitmapMember == null) {
            throw new ScriptError("Cannot get prop of invalid bitmap ref");
        }

        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapMember.imageRef);
        if (bitmap == null) {
            throw new ScriptError("Cannot get prop of invalid bitmap ref");
        }

        int regX = bitmapMember.regPointX;
        int regY = bitmapMember.regPointY;

        switch (prop.toLowerCase()) {
            case "width":
                return player.allocDatum(Datum.ofInt(bitmap.getWidth()));

            case "height":
                return player.allocDatum(Datum.ofInt(bitmap.getHeight()));

            case "image":
                // Return bitmap ref (the imageRef id)
                return player.allocDatum(Datum.ofBitmapRef(bitmapMember.imageRef));

            case "paletteref": {
                PaletteRef paletteRef = bitmap.paletteRef;
                if (paletteRef != null) {
                    // TODO: Return proper palette ref datum when supported
                    return player.allocDatum(Datum.ofInt(0));
                }
                return 0; // Void
            }

            case "regpoint": {
                int xRef = player.allocDatum(Datum.ofInt(regX));
                int yRef = player.allocDatum(Datum.ofInt(regY));
                return player.allocDatum(Datum.ofPoint(xRef, yRef));
            }

            case "rect": {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int leftRef = player.allocDatum(Datum.ofInt(0));
                int topRef = player.allocDatum(Datum.ofInt(0));
                int rightRef = player.allocDatum(Datum.ofInt(width));
                int bottomRef = player.allocDatum(Datum.ofInt(height));
                return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
            }

            case "depth":
                return player.allocDatum(Datum.ofInt(bitmap.getBitDepth()));

            default:
                throw new ScriptError("Cannot get castMember property " + prop + " for bitmap");
        }
    }

    /**
     * Set a property on a bitmap cast member.
     */
    public static void setProp(DirPlayer player, CastMemberRef memberRef, String prop, int valueRef) throws ScriptError {
        Datum value = player.getDatum(valueRef);

        switch (prop.toLowerCase()) {
            case "image": {
                // Get source bitmap ref from value
                com.dirplayer.player.bitmap.BitmapRef srcBitmapRef = value.toBitmapRef();
                if (srcBitmapRef == null) {
                    throw new ScriptError("Invalid bitmap ref for image property");
                }
                int srcBitmapId = srcBitmapRef.bitmapId;
                Bitmap srcBitmap = player.bitmapManager.getBitmap(srcBitmapId);
                if (srcBitmap == null) {
                    throw new ScriptError("Cannot find source bitmap");
                }

                int newWidth = srcBitmap.getWidth();
                int newHeight = srcBitmap.getHeight();

                // Clone the source bitmap
                Bitmap clonedBitmap = srcBitmap.copy();

                // Get the member's current image ref and replace it
                BitmapMember bitmapMember = getBitmapMemberMut(player, memberRef);
                if (bitmapMember != null) {
                    // Store the cloned bitmap
                    player.bitmapManager.storeBitmap(bitmapMember.imageRef, clonedBitmap);
                    // Update member dimensions
                    if (bitmapMember.info != null) {
                        bitmapMember.info.width = newWidth;
                        bitmapMember.info.height = newHeight;
                    }
                }
                break;
            }

            case "regpoint": {
                if (!value.isPoint()) {
                    throw new ScriptError("regPoint must be a Point");
                }
                int[] pointRefs = value.toPoint();
                int x = player.getDatum(pointRefs[0]).intValue();
                int y = player.getDatum(pointRefs[1]).intValue();

                BitmapMember bitmapMember = getBitmapMemberMut(player, memberRef);
                if (bitmapMember != null) {
                    bitmapMember.regPointX = x;
                    bitmapMember.regPointY = y;
                }
                break;
            }

            case "paletteref": {
                BitmapMember bitmapMember = getBitmapMember(player, memberRef);
                if (bitmapMember == null) {
                    throw new ScriptError("Invalid bitmap member");
                }
                Bitmap bitmap = player.bitmapManager.getBitmap(bitmapMember.imageRef);
                if (bitmap == null) {
                    throw new ScriptError("Invalid bitmap ref");
                }

                if (value.isSymbol()) {
                    String symbolName = value.symbolValue();
                    BuiltInPalette builtIn = BuiltInPalette.fromSymbol(symbolName);
                    if (builtIn != null) {
                        bitmap.paletteRef = PaletteRef.ofBuiltIn(builtIn);
                    }
                } else if (value.isCastMemberRef()) {
                    CastMemberRef paletteMemRef = value.toMemberRef();
                    bitmap.paletteRef = PaletteRef.ofMember(paletteMemRef);
                } else {
                    throw new ScriptError("Cannot set bitmap member paletteRef to type " + value.typeStr());
                }
                break;
            }

            case "palette": {
                BitmapMember bitmapMember = getBitmapMember(player, memberRef);
                if (bitmapMember == null) {
                    throw new ScriptError("Invalid bitmap member");
                }
                Bitmap bitmap = player.bitmapManager.getBitmap(bitmapMember.imageRef);
                if (bitmap == null) {
                    throw new ScriptError("Invalid bitmap ref");
                }

                if (value.isInt()) {
                    int paletteNum = value.intValue();
                    if (paletteNum < 0) {
                        // Built-in palette
                        BuiltInPalette builtIn = BuiltInPalette.fromValue((short) paletteNum);
                        if (builtIn != null) {
                            bitmap.paletteRef = PaletteRef.ofBuiltIn(builtIn);
                        }
                    } else {
                        // Cast member palette
                        CastMemberRef paletteMemRef = memberRefFromSlotNumber(paletteNum);
                        bitmap.paletteRef = PaletteRef.ofMember(paletteMemRef);
                    }
                } else if (value.isCastMemberRef()) {
                    CastMemberRef paletteMemRef = value.toMemberRef();
                    bitmap.paletteRef = PaletteRef.ofMember(paletteMemRef);
                } else {
                    throw new ScriptError("Cannot set bitmap member palette to type " + value.typeStr());
                }
                break;
            }

            default:
                throw new ScriptError("Cannot set castMember prop " + prop + " for bitmap");
        }
    }

    // Helper methods

    private static BitmapMember getBitmapMember(DirPlayer player, CastMemberRef memberRef) {
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null || member.specificData == null) {
            return null;
        }
        if (member.specificData instanceof BitmapMember) {
            return (BitmapMember) member.specificData;
        }
        return null;
    }

    private static BitmapMember getBitmapMemberMut(DirPlayer player, CastMemberRef memberRef) {
        return getBitmapMember(player, memberRef);
    }

    private static CastMemberRef memberRefFromSlotNumber(int slotNumber) {
        int castLib = (slotNumber >> 16) & 0xFFFF;
        int castMember = slotNumber & 0xFFFF;
        if (castLib == 0) {
            castLib = 1; // Default to first cast
        }
        return new CastMemberRef(castLib, castMember);
    }
}
