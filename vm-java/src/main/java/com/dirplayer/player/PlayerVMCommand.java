package com.dirplayer.player;

/**
 * Commands that can be sent to the player VM.
 * Port of Rust PlayerVMCommand enum.
 */
public class PlayerVMCommand {
    public enum Type {
        LoadMovieFromFile,
        SetBasePath,
        SetSystemFontPath,
        SetExternalParams,
        SetStageSize,
        MouseDown,
        MouseUp,
        MouseMove,
        KeyDown,
        KeyUp,
        TimeoutTriggered,
        PrintMemberBitmapHex,
        TriggerAlertHook
    }

    public Type type;
    public String stringArg1;
    public String stringArg2;
    public int intArg1;
    public int intArg2;
    public boolean boolArg;
    public java.util.Map<String, String> mapArg;
    public CastMemberRef memberRefArg;

    private PlayerVMCommand(Type type) {
        this.type = type;
    }

    public static PlayerVMCommand loadMovieFromFile(String path, boolean autoplay) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.LoadMovieFromFile);
        cmd.stringArg1 = path;
        cmd.boolArg = autoplay;
        return cmd;
    }

    public static PlayerVMCommand setBasePath(String path) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.SetBasePath);
        cmd.stringArg1 = path;
        return cmd;
    }

    public static PlayerVMCommand setSystemFontPath(String path) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.SetSystemFontPath);
        cmd.stringArg1 = path;
        return cmd;
    }

    public static PlayerVMCommand setExternalParams(java.util.Map<String, String> params) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.SetExternalParams);
        cmd.mapArg = params;
        return cmd;
    }

    public static PlayerVMCommand setStageSize(int width, int height) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.SetStageSize);
        cmd.intArg1 = width;
        cmd.intArg2 = height;
        return cmd;
    }

    public static PlayerVMCommand mouseDown(int x, int y) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.MouseDown);
        cmd.intArg1 = x;
        cmd.intArg2 = y;
        return cmd;
    }

    public static PlayerVMCommand mouseUp(int x, int y) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.MouseUp);
        cmd.intArg1 = x;
        cmd.intArg2 = y;
        return cmd;
    }

    public static PlayerVMCommand mouseMove(int x, int y) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.MouseMove);
        cmd.intArg1 = x;
        cmd.intArg2 = y;
        return cmd;
    }

    public static PlayerVMCommand keyDown(String key, int code) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.KeyDown);
        cmd.stringArg1 = key;
        cmd.intArg1 = code;
        return cmd;
    }

    public static PlayerVMCommand keyUp(String key, int code) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.KeyUp);
        cmd.stringArg1 = key;
        cmd.intArg1 = code;
        return cmd;
    }

    public static PlayerVMCommand timeoutTriggered(String name) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.TimeoutTriggered);
        cmd.stringArg1 = name;
        return cmd;
    }

    public static PlayerVMCommand printMemberBitmapHex(CastMemberRef memberRef) {
        PlayerVMCommand cmd = new PlayerVMCommand(Type.PrintMemberBitmapHex);
        cmd.memberRefArg = memberRef;
        return cmd;
    }

    public static PlayerVMCommand triggerAlertHook() {
        return new PlayerVMCommand(Type.TriggerAlertHook);
    }
}
