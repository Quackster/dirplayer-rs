package com.dirplayer.player.handlers.datum.castmember;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.cast.FilmLoopMember;

/**
 * Handlers for film loop cast member datum operations.
 * Port of Rust FilmLoopMemberHandlers.
 */
public class FilmLoopMemberHandlers {

    /**
     * Get a property from a film loop cast member.
     */
    public static int getProp(DirPlayer player, CastMemberRef memberRef, String prop) throws ScriptError {
        FilmLoopMember filmLoop = getFilmLoopMember(player, memberRef);
        if (filmLoop == null) {
            throw new ScriptError("Invalid film loop member");
        }

        // The filmloop's rect is stored in info as:
        // - reg_point = (left, top) coordinates
        // - width = right coordinate
        // - height = bottom coordinate
        // So rect = (reg_point.x, reg_point.y, width, height)
        // And actual dimensions = (width - reg_point.x, height - reg_point.y)
        int rectLeft = filmLoop.info != null ? filmLoop.info.regPointX : 0;
        int rectTop = filmLoop.info != null ? filmLoop.info.regPointY : 0;
        int rectRight = filmLoop.info != null ? filmLoop.info.width : 0;
        int rectBottom = filmLoop.info != null ? filmLoop.info.height : 0;
        int rectWidth = rectRight - rectLeft;
        int rectHeight = rectBottom - rectTop;

        switch (prop.toLowerCase()) {
            case "rect": {
                int leftRef = player.allocDatum(Datum.ofInt(rectLeft));
                int topRef = player.allocDatum(Datum.ofInt(rectTop));
                int rightRef = player.allocDatum(Datum.ofInt(rectRight));
                int bottomRef = player.allocDatum(Datum.ofInt(rectBottom));
                return player.allocDatum(Datum.ofRect(leftRef, topRef, rightRef, bottomRef));
            }

            case "width":
                return player.allocDatum(Datum.ofInt(rectWidth));

            case "height":
                return player.allocDatum(Datum.ofInt(rectHeight));

            default:
                throw new ScriptError("Cannot get castMember property " + prop + " for film loop");
        }
    }

    // Helper methods

    private static FilmLoopMember getFilmLoopMember(DirPlayer player, CastMemberRef memberRef) {
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null || member.specificData == null) {
            return null;
        }
        if (member.specificData instanceof FilmLoopMember) {
            return (FilmLoopMember) member.specificData;
        }
        return null;
    }
}
