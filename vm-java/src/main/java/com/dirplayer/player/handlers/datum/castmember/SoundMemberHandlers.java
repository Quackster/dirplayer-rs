package com.dirplayer.player.handlers.datum.castmember;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.cast.SoundMember;

/**
 * Handlers for sound cast member datum operations.
 * Port of Rust SoundMemberHandlers.
 */
public class SoundMemberHandlers {

    /**
     * Get a property from a sound cast member.
     */
    public static int getProp(DirPlayer player, CastMemberRef memberRef, String prop) throws ScriptError {
        SoundMember sound = getSoundMember(player, memberRef);
        if (sound == null) {
            throw new ScriptError("Invalid sound member");
        }

        switch (prop.toLowerCase()) {
            case "duration":
                return player.allocDatum(Datum.ofInt(
                    sound.info != null ? (int) sound.info.duration : 0));

            case "samplerate":
                return player.allocDatum(Datum.ofInt(
                    sound.info != null ? (int) sound.info.sampleRate : 0));

            case "samplesize":
                return player.allocDatum(Datum.ofInt(
                    sound.info != null ? sound.info.sampleSize : 0));

            case "channelcount":
                return player.allocDatum(Datum.ofInt(
                    sound.info != null ? sound.info.channels : 0));

            case "samplecount":
                return player.allocDatum(Datum.ofInt(
                    sound.info != null ? (int) sound.info.sampleCount : 0));

            default:
                throw new ScriptError("Cannot get castMember property " + prop + " for sound");
        }
    }

    // Helper methods

    private static SoundMember getSoundMember(DirPlayer player, CastMemberRef memberRef) {
        com.dirplayer.player.CastMember member = player.movie.castManager.findMemberByRef(memberRef);
        if (member == null || member.specificData == null) {
            return null;
        }
        if (member.specificData instanceof SoundMember) {
            return (SoundMember) member.specificData;
        }
        return null;
    }
}
