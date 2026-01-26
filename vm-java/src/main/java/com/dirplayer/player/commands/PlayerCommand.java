package com.dirplayer.player.commands;

import com.dirplayer.player.CastMemberRef;

import java.util.Map;

/**
 * Player VM commands.
 * Port of Rust PlayerVMCommand enum.
 */
public abstract class PlayerCommand {

    public abstract String format();

    // Command types

    public static class LoadMovieFromFile extends PlayerCommand {
        public final String path;
        public final boolean autoplay;

        public LoadMovieFromFile(String path, boolean autoplay) {
            this.path = path;
            this.autoplay = autoplay;
        }

        @Override
        public String format() {
            return "LoadMovieFromFile(" + path + ", " + autoplay + ")";
        }
    }

    public static class SetExternalParams extends PlayerCommand {
        public final Map<String, String> params;

        public SetExternalParams(Map<String, String> params) {
            this.params = params;
        }

        @Override
        public String format() {
            return "SetExternalParams(" + params.keySet() + ")";
        }
    }

    public static class SetBasePath extends PlayerCommand {
        public final String path;

        public SetBasePath(String path) {
            this.path = path;
        }

        @Override
        public String format() {
            return "SetBasePath(" + path + ")";
        }
    }

    public static class SetSystemFontPath extends PlayerCommand {
        public final String path;

        public SetSystemFontPath(String path) {
            this.path = path;
        }

        @Override
        public String format() {
            return "SetSystemFontPath(" + path + ")";
        }
    }

    public static class SetStageSize extends PlayerCommand {
        public final int width;
        public final int height;

        public SetStageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public String format() {
            return "SetStageSize(" + width + ", " + height + ")";
        }
    }

    public static class TimeoutTriggered extends PlayerCommand {
        public final String timeoutRef;

        public TimeoutTriggered(String timeoutRef) {
            this.timeoutRef = timeoutRef;
        }

        @Override
        public String format() {
            return "TimeoutTriggered(" + timeoutRef + ")";
        }
    }

    public static class PrintMemberBitmapHex extends PlayerCommand {
        public final CastMemberRef memberRef;

        public PrintMemberBitmapHex(CastMemberRef memberRef) {
            this.memberRef = memberRef;
        }

        @Override
        public String format() {
            return "PrintMemberBitmapHex(" + memberRef + ")";
        }
    }

    public static class MouseDown extends PlayerCommand {
        public final int x;
        public final int y;

        public MouseDown(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String format() {
            return "MouseDown(" + x + ", " + y + ")";
        }
    }

    public static class MouseUp extends PlayerCommand {
        public final int x;
        public final int y;

        public MouseUp(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String format() {
            return "MouseUp(" + x + ", " + y + ")";
        }
    }

    public static class MouseMove extends PlayerCommand {
        public final int x;
        public final int y;

        public MouseMove(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String format() {
            return "MouseMove(" + x + ", " + y + ")";
        }
    }

    public static class KeyDown extends PlayerCommand {
        public final String key;
        public final int keyCode;

        public KeyDown(String key, int keyCode) {
            this.key = key;
            this.keyCode = keyCode;
        }

        @Override
        public String format() {
            return "KeyDown(" + key + ")";
        }
    }

    public static class KeyUp extends PlayerCommand {
        public final String key;
        public final int keyCode;

        public KeyUp(String key, int keyCode) {
            this.key = key;
            this.keyCode = keyCode;
        }

        @Override
        public String format() {
            return "KeyUp(" + key + ")";
        }
    }

    public static class TriggerAlertHook extends PlayerCommand {
        @Override
        public String format() {
            return "TriggerAlertHook";
        }
    }

    public static class GoToFrame extends PlayerCommand {
        public final int frame;

        public GoToFrame(int frame) {
            this.frame = frame;
        }

        @Override
        public String format() {
            return "GoToFrame(" + frame + ")";
        }
    }

    public static class Play extends PlayerCommand {
        @Override
        public String format() {
            return "Play";
        }
    }

    public static class Stop extends PlayerCommand {
        @Override
        public String format() {
            return "Stop";
        }
    }

    public static class CallHandler extends PlayerCommand {
        public final String handlerName;
        public final Object[] args;

        public CallHandler(String handlerName, Object[] args) {
            this.handlerName = handlerName;
            this.args = args;
        }

        @Override
        public String format() {
            return "CallHandler(" + handlerName + ")";
        }
    }
}
