package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.sound.SoundChannel;
import com.dirplayer.player.sound.SoundStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Handlers for sound channel datum operations.
 * Port of Rust SoundChannelDatumHandlers.
 */
public class SoundChannelHandlers {

    /**
     * Call handler on sound channel datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        String handlerNameLower = handlerName.toLowerCase();

        switch (handlerNameLower) {
            case "play":
                if (args.isEmpty()) {
                    handlePlay(player, datumRef);
                } else {
                    handlePlayMember(player, datumRef, args.get(0));
                }
                return datumRef;

            case "playfile":
                if (args.isEmpty()) {
                    throw new ScriptError("playFile requires a member argument");
                }
                handlePlayFile(player, datumRef, args.get(0));
                return datumRef;

            case "playnext":
                handlePlayNext(player, datumRef);
                return datumRef;

            case "stop":
                handleStop(player, datumRef);
                return datumRef;

            case "pause":
                handlePause(player, datumRef);
                return datumRef;

            case "rewind":
                handleRewind(player, datumRef);
                return datumRef;

            case "queue":
                if (args.isEmpty()) {
                    throw new ScriptError("queue requires a member argument");
                }
                handleQueue(player, datumRef, args.get(0));
                return datumRef;

            case "breakloop":
                handleBreakLoop(player, datumRef);
                return datumRef;

            case "fadein": {
                int ticks = args.isEmpty() ? 60 : player.getDatum(args.get(0)).intValue();
                double toVolume = args.size() > 1 ? player.getDatum(args.get(1)).floatValue() : 255.0;
                handleFadeIn(player, datumRef, ticks, toVolume);
                return datumRef;
            }

            case "fadeout": {
                int ticks = args.isEmpty() ? 60 : player.getDatum(args.get(0)).intValue();
                handleFadeOut(player, datumRef, ticks);
                return datumRef;
            }

            case "fadeto": {
                if (args.size() < 2) {
                    throw new ScriptError("fadeTo requires ticks and volume arguments");
                }
                int ticks = player.getDatum(args.get(0)).intValue();
                double toVolume = player.getDatum(args.get(1)).floatValue();
                handleFadeTo(player, datumRef, ticks, toVolume);
                return datumRef;
            }

            case "setplaylist":
                if (args.isEmpty()) {
                    throw new ScriptError("setPlayList requires a list argument");
                }
                handleSetPlaylist(player, datumRef, args.get(0));
                return datumRef;

            case "getplaylist":
                return handleGetPlaylist(player, datumRef);

            case "isbusy": {
                boolean isBusy = handleIsBusy(player, datumRef);
                return player.allocDatum(Datum.ofInt(isBusy ? 1 : 0));
            }

            default:
                throw new ScriptError("No handler " + handlerName + " for sound channel");
        }
    }

    /**
     * Get a property from a sound channel.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);

        Datum result;
        switch (prop.toLowerCase()) {
            case "volume":
                result = Datum.ofFloat(channel.getVolume());
                break;
            case "duration":
                result = Datum.ofFloat(channel.getDuration());
                break;
            case "pan":
                result = Datum.ofFloat(channel.getPan());
                break;
            case "loopcount":
                result = Datum.ofInt(channel.getLoopCount());
                break;
            case "loopsremaining":
                result = Datum.ofInt(channel.getLoopsRemaining());
                break;
            case "starttime":
                result = Datum.ofFloat(channel.getStartTime());
                break;
            case "endtime":
                result = Datum.ofFloat(channel.getEndTime());
                break;
            case "loopstarttime":
                result = Datum.ofFloat(channel.getLoopStartTime());
                break;
            case "loopendtime":
                result = Datum.ofFloat(channel.getLoopEndTime());
                break;
            case "elapsedtime":
                result = Datum.ofFloat(channel.getElapsedTime());
                break;
            case "samplerate":
                result = Datum.ofInt(channel.getSampleRate());
                break;
            case "samplecount":
                result = Datum.ofInt(channel.getSampleCount());
                break;
            case "channelcount":
                result = Datum.ofInt(channel.getChannelCount());
                break;
            case "status":
                result = Datum.ofInt(channel.getStatus().ordinal());
                break;
            default:
                throw new ScriptError("Cannot get property " + prop + " for sound channel");
        }

        return player.allocDatum(result);
    }

    /**
     * Set a property on a sound channel.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        Datum value = player.getDatum(valueRef);

        switch (prop.toLowerCase()) {
            case "volume":
                channel.setVolume(value.floatValue());
                break;
            case "pan":
                channel.setPan(value.floatValue());
                break;
            case "loopcount":
                channel.setLoopCount(value.intValue());
                break;
            case "starttime":
                channel.setStartTime(Math.max(0.0, value.floatValue()));
                break;
            case "endtime": {
                double endTime = value.floatValue();
                if (endTime == 0.0) {
                    endTime = channel.getDuration();
                }
                channel.setEndTime(endTime);
                break;
            }
            case "loopstarttime":
                channel.setLoopStartTime(Math.max(0.0, value.floatValue()));
                break;
            case "loopendtime":
                channel.setLoopEndTime(value.floatValue());
                break;
            default:
                throw new ScriptError("Cannot set property " + prop + " for sound channel");
        }
    }

    // Helper methods

    private static int getChannelIndex(DirPlayer player, int datumRef) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (datum.getType() != DatumType.SoundChannel) {
            throw new ScriptError("Expected sound channel reference");
        }
        int channelNum = datum.toSoundChannel();
        if (channelNum == 0) {
            throw new ScriptError("Sound channel index must be >= 1");
        }
        return channelNum - 1; // Convert to 0-based index
    }

    private static SoundChannel getSoundChannel(DirPlayer player, int datumRef) throws ScriptError {
        int channelIdx = getChannelIndex(player, datumRef);
        SoundChannel channel = player.soundManager.getChannel(channelIdx);
        if (channel == null) {
            throw new ScriptError("Invalid sound channel " + (channelIdx + 1));
        }
        return channel;
    }

    private static void handlePlayMember(DirPlayer player, int datumRef, int memberRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.clearPlaylist();
        channel.setLoopCount(1);
        channel.setLoopsRemaining(1);
        channel.playMember(memberRef, player);
    }

    private static void handlePlay(DirPlayer player, int datumRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.play();
    }

    private static void handlePlayFile(DirPlayer player, int datumRef, int memberRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.playFile(memberRef, player);
    }

    private static void handlePlayNext(DirPlayer player, int datumRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.playNext();
    }

    private static void handleStop(DirPlayer player, int datumRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.stop();
    }

    private static void handlePause(DirPlayer player, int datumRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.pause();
    }

    private static void handleRewind(DirPlayer player, int datumRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.rewind();
    }

    private static void handleQueue(DirPlayer player, int datumRef, int memberRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.queue(memberRef, player);
    }

    private static void handleBreakLoop(DirPlayer player, int datumRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.breakLoop();
    }

    private static void handleFadeIn(DirPlayer player, int datumRef, int ticks, double toVolume) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.fadeIn(ticks, toVolume);
    }

    private static void handleFadeOut(DirPlayer player, int datumRef, int ticks) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.fadeOut(ticks);
    }

    private static void handleFadeTo(DirPlayer player, int datumRef, int ticks, double toVolume) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        channel.fadeTo(ticks, toVolume);
    }

    private static void handleSetPlaylist(DirPlayer player, int datumRef, int listRef) throws ScriptError {
        Datum listDatum = player.getDatum(listRef);
        List<Integer> lingoList;

        if (listDatum.isList()) {
            lingoList = listDatum.toList();
        } else if (listDatum.isPropList()) {
            // Empty proplist [:] means clear the playlist
            if (listDatum.toMap().isEmpty()) {
                lingoList = new ArrayList<>();
            } else {
                throw new ScriptError("setPlayList expects a list, not a non-empty proplist");
            }
        } else {
            throw new ScriptError("setPlayList expects a list or empty proplist, got " + listDatum.typeStr());
        }

        SoundChannel channel = getSoundChannel(player, datumRef);

        // If the playlist is empty, clear and exit
        if (lingoList.isEmpty()) {
            channel.clearPlaylist();
            return;
        }

        // Build the playlist from prop lists
        List<SoundChannel.SoundSegment> segments = new ArrayList<>();
        List<Integer> playlist = new ArrayList<>();

        for (Integer segmentRef : lingoList) {
            Datum segmentDatum = player.getDatum(segmentRef);

            if (segmentDatum.isPropList()) {
                Integer memberValue = null;
                Integer loopCountValue = null;

                for (int[] pair : segmentDatum.toPropList()) {
                    Datum key = player.getDatum(pair[0]);
                    Datum value = player.getDatum(pair[1]);

                    String keyStr;
                    if (key.isSymbol()) {
                        keyStr = key.symbolValue().toLowerCase();
                    } else {
                        keyStr = key.stringValue().toLowerCase();
                    }

                    if (keyStr.equals("member") || keyStr.equals("#member")) {
                        memberValue = pair[1];
                    } else if (keyStr.equals("loopcount") || keyStr.equals("#loopcount")) {
                        if (value.isInt()) {
                            loopCountValue = value.intValue();
                        }
                    }
                }

                // Validate and add segment
                if (memberValue != null && loopCountValue != null && loopCountValue > 0) {
                    SoundChannel.SoundSegment segment = new SoundChannel.SoundSegment(
                        memberValue, loopCountValue, loopCountValue
                    );
                    segments.add(segment);
                    playlist.add(segmentRef);
                }
                // Skip invalid segments silently (matching Rust behavior)
            }
        }

        channel.setPlaylistSegments(segments, playlist);
    }

    private static int handleGetPlaylist(DirPlayer player, int datumRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        List<Integer> playlist = channel.getPlaylist();
        return player.allocDatum(Datum.ofList(DatumType.List, playlist, false));
    }

    private static boolean handleIsBusy(DirPlayer player, int datumRef) throws ScriptError {
        SoundChannel channel = getSoundChannel(player, datumRef);
        return channel.isBusy();
    }
}
