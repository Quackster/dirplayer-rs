package com.dirplayer.player;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.bitmap.PaletteRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stage property accessor for the main window/canvas.
 * Port of Rust stage.rs.
 */
public class Stage {
    private static final Logger logger = LoggerFactory.getLogger(Stage.class);

    /**
     * Get a stage property.
     *
     * @param player The player instance
     * @param prop The property name to get
     * @return The property value as a Datum
     * @throws ScriptError if the property is invalid
     */
    public static Datum getStageProp(DirPlayer player, String prop) throws ScriptError {
        switch (prop) {
            case "rect": {
                int width = player.movie.rect.width();
                int height = player.movie.rect.height();
                int[] refs = new int[] {
                    player.allocDatum(Datum.ofInt(0)),
                    player.allocDatum(Datum.ofInt(0)),
                    player.allocDatum(Datum.ofInt(width)),
                    player.allocDatum(Datum.ofInt(height))
                };
                return Datum.ofRect(refs);
            }
            case "sourceRect": {
                // TODO where does this come from?
                int width = player.movie.rect.width();
                int height = player.movie.rect.height();
                int[] refs = new int[] {
                    player.allocDatum(Datum.ofInt(0)),
                    player.allocDatum(Datum.ofInt(0)),
                    player.allocDatum(Datum.ofInt(width)),
                    player.allocDatum(Datum.ofInt(height))
                };
                return Datum.ofRect(refs);
            }
            case "bgColor":
                return Datum.ofColorRef(player.bgColor);
            case "image": {
                logger.warn("TODO get stage image");
                int width = player.movie.rect.width();
                int height = player.movie.rect.height();
                Bitmap newBitmap = new Bitmap(
                    width,
                    height,
                    32,
                    32,
                    0,
                    PaletteRef.ofBuiltIn(getSystemDefaultPalette())
                );
                int bitmapId = player.bitmapManager.addBitmap(newBitmap);
                return Datum.ofBitmapRef(bitmapId);
            }
            default:
                throw new ScriptError("Invalid stage property " + prop);
        }
    }

    /**
     * Set a stage property.
     *
     * @param player The player instance
     * @param prop The property name to set
     * @param value The value reference to set
     * @throws ScriptError if the property cannot be set
     */
    public static void setStageProp(DirPlayer player, String prop, int valueRef) throws ScriptError {
        switch (prop) {
            case "title":
                player.title = "title";
                break;
            default:
                throw new ScriptError("Cannot set stage property " + prop);
        }
    }

    /**
     * Get the system default palette.
     *
     * @return The default built-in palette
     */
    public static BuiltInPalette getSystemDefaultPalette() {
        return BuiltInPalette.SystemWin;
    }
}
