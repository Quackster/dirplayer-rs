package com.dirplayer.director.lingo;

import java.util.HashMap;
import java.util.Map;

/**
 * Lingo constants and property name mappings.
 * Port of Rust constants.rs
 */
public class LingoConstants {

    private static final Map<OpCode, String> OPCODE_NAMES = new HashMap<>();
    private static final Map<Integer, String> ANIM_PROP_NAMES = new HashMap<>();
    private static final Map<Integer, String> ANIM2_PROP_NAMES = new HashMap<>();
    private static final Map<Integer, String> MOVIE_PROP_NAMES = new HashMap<>();
    private static final Map<Integer, String> SPRITE_PROP_NAMES = new HashMap<>();

    static {
        // Single-byte opcodes
        OPCODE_NAMES.put(OpCode.Ret, "ret");
        OPCODE_NAMES.put(OpCode.RetFactory, "retfactory");
        OPCODE_NAMES.put(OpCode.Mul, "mul");
        OPCODE_NAMES.put(OpCode.PushZero, "pushzero");
        OPCODE_NAMES.put(OpCode.Add, "add");
        OPCODE_NAMES.put(OpCode.Sub, "sub");
        OPCODE_NAMES.put(OpCode.Div, "div");
        OPCODE_NAMES.put(OpCode.Mod, "mod");
        OPCODE_NAMES.put(OpCode.Inv, "inv");
        OPCODE_NAMES.put(OpCode.JoinStr, "joinstr");
        OPCODE_NAMES.put(OpCode.JoinPadStr, "joinpadstr");
        OPCODE_NAMES.put(OpCode.Lt, "lt");
        OPCODE_NAMES.put(OpCode.LtEq, "lteq");
        OPCODE_NAMES.put(OpCode.NtEq, "nteq");
        OPCODE_NAMES.put(OpCode.Eq, "eq");
        OPCODE_NAMES.put(OpCode.Gt, "gt");
        OPCODE_NAMES.put(OpCode.GtEq, "gteq");
        OPCODE_NAMES.put(OpCode.And, "and");
        OPCODE_NAMES.put(OpCode.Or, "or");
        OPCODE_NAMES.put(OpCode.Not, "not");
        OPCODE_NAMES.put(OpCode.ContainsStr, "containsstr");
        OPCODE_NAMES.put(OpCode.Contains0Str, "contains0str");
        OPCODE_NAMES.put(OpCode.GetChunk, "getchunk");
        OPCODE_NAMES.put(OpCode.HiliteChunk, "hilitechunk");
        OPCODE_NAMES.put(OpCode.OntoSpr, "ontospr");
        OPCODE_NAMES.put(OpCode.IntoSpr, "intospr");
        OPCODE_NAMES.put(OpCode.GetField, "getfield");
        OPCODE_NAMES.put(OpCode.StartTell, "starttell");
        OPCODE_NAMES.put(OpCode.EndTell, "endtell");
        OPCODE_NAMES.put(OpCode.PushList, "pushlist");
        OPCODE_NAMES.put(OpCode.PushPropList, "pushproplist");
        OPCODE_NAMES.put(OpCode.Swap, "swap");
        OPCODE_NAMES.put(OpCode.CallJavaScript, "calljavascript");

        // Multi-byte opcodes
        OPCODE_NAMES.put(OpCode.PushInt8, "pushint8");
        OPCODE_NAMES.put(OpCode.PushArgListNoRet, "pusharglistnoret");
        OPCODE_NAMES.put(OpCode.PushArgList, "pusharglist");
        OPCODE_NAMES.put(OpCode.PushCons, "pushcons");
        OPCODE_NAMES.put(OpCode.PushSymb, "pushsymb");
        OPCODE_NAMES.put(OpCode.PushVarRef, "pushvarref");
        OPCODE_NAMES.put(OpCode.GetGlobal2, "getglobal2");
        OPCODE_NAMES.put(OpCode.GetGlobal, "getglobal");
        OPCODE_NAMES.put(OpCode.GetProp, "getprop");
        OPCODE_NAMES.put(OpCode.GetParam, "getparam");
        OPCODE_NAMES.put(OpCode.GetLocal, "getlocal");
        OPCODE_NAMES.put(OpCode.SetGlobal2, "setglobal2");
        OPCODE_NAMES.put(OpCode.SetGlobal, "setglobal");
        OPCODE_NAMES.put(OpCode.SetProp, "setprop");
        OPCODE_NAMES.put(OpCode.SetParam, "setparam");
        OPCODE_NAMES.put(OpCode.SetLocal, "setlocal");
        OPCODE_NAMES.put(OpCode.Jmp, "jmp");
        OPCODE_NAMES.put(OpCode.EndRepeat, "endrepeat");
        OPCODE_NAMES.put(OpCode.JmpIfZ, "jmpifz");
        OPCODE_NAMES.put(OpCode.LocalCall, "localcall");
        OPCODE_NAMES.put(OpCode.ExtCall, "extcall");
        OPCODE_NAMES.put(OpCode.ObjCallV4, "objcallv4");
        OPCODE_NAMES.put(OpCode.Put, "put");
        OPCODE_NAMES.put(OpCode.PutChunk, "putchunk");
        OPCODE_NAMES.put(OpCode.DeleteChunk, "deletechunk");
        OPCODE_NAMES.put(OpCode.Get, "get");
        OPCODE_NAMES.put(OpCode.Set, "set");
        OPCODE_NAMES.put(OpCode.GetMovieProp, "getmovieprop");
        OPCODE_NAMES.put(OpCode.SetMovieProp, "setmovieprop");
        OPCODE_NAMES.put(OpCode.GetObjProp, "getobjprop");
        OPCODE_NAMES.put(OpCode.SetObjProp, "setobjprop");
        OPCODE_NAMES.put(OpCode.TellCall, "tellcall");
        OPCODE_NAMES.put(OpCode.Peek, "peek");
        OPCODE_NAMES.put(OpCode.Pop, "pop");
        OPCODE_NAMES.put(OpCode.TheBuiltin, "thebuiltin");
        OPCODE_NAMES.put(OpCode.ObjCall, "objcall");
        OPCODE_NAMES.put(OpCode.PushChunkVarRef, "pushchunkvarref");
        OPCODE_NAMES.put(OpCode.PushInt16, "pushint16");
        OPCODE_NAMES.put(OpCode.PushInt32, "pushint32");
        OPCODE_NAMES.put(OpCode.GetChainedProp, "getchainedprop");
        OPCODE_NAMES.put(OpCode.PushFloat32, "pushfloat32");
        OPCODE_NAMES.put(OpCode.GetTopLevelProp, "gettoplevelprop");
        OPCODE_NAMES.put(OpCode.NewObj, "newobj");

        // Animation properties
        ANIM_PROP_NAMES.put(0x01, "beepOn");
        ANIM_PROP_NAMES.put(0x02, "buttonStyle");
        ANIM_PROP_NAMES.put(0x03, "centerStage");
        ANIM_PROP_NAMES.put(0x04, "checkBoxAccess");
        ANIM_PROP_NAMES.put(0x05, "checkboxType");
        ANIM_PROP_NAMES.put(0x06, "colorDepth");
        ANIM_PROP_NAMES.put(0x07, "colorQD");
        ANIM_PROP_NAMES.put(0x08, "exitLock");
        ANIM_PROP_NAMES.put(0x09, "fixStageSize");
        ANIM_PROP_NAMES.put(0x0a, "fullColorPermit");
        ANIM_PROP_NAMES.put(0x0b, "imageDirect");
        ANIM_PROP_NAMES.put(0x0c, "doubleClick");
        ANIM_PROP_NAMES.put(0x0d, "key");
        ANIM_PROP_NAMES.put(0x0e, "lastClick");
        ANIM_PROP_NAMES.put(0x0f, "lastEvent");
        ANIM_PROP_NAMES.put(0x10, "keyCode");
        ANIM_PROP_NAMES.put(0x11, "lastKey");
        ANIM_PROP_NAMES.put(0x12, "lastRoll");
        ANIM_PROP_NAMES.put(0x13, "timeoutLapsed");
        ANIM_PROP_NAMES.put(0x14, "multiSound");
        ANIM_PROP_NAMES.put(0x15, "pauseState");
        ANIM_PROP_NAMES.put(0x16, "quickTimePresent");
        ANIM_PROP_NAMES.put(0x17, "selEnd");
        ANIM_PROP_NAMES.put(0x18, "selStart");
        ANIM_PROP_NAMES.put(0x19, "soundEnabled");
        ANIM_PROP_NAMES.put(0x1a, "soundLevel");
        ANIM_PROP_NAMES.put(0x1b, "stageColor");
        ANIM_PROP_NAMES.put(0x1d, "switchColorDepth");
        ANIM_PROP_NAMES.put(0x1e, "timeoutKeyDown");
        ANIM_PROP_NAMES.put(0x1f, "timeoutLength");
        ANIM_PROP_NAMES.put(0x20, "timeoutMouse");
        ANIM_PROP_NAMES.put(0x21, "timeoutPlay");
        ANIM_PROP_NAMES.put(0x22, "timer");
        ANIM_PROP_NAMES.put(0x23, "preLoadRAM");
        ANIM_PROP_NAMES.put(0x24, "videoForWindowsPresent");
        ANIM_PROP_NAMES.put(0x25, "netPresent");
        ANIM_PROP_NAMES.put(0x26, "safePlayer");
        ANIM_PROP_NAMES.put(0x27, "soundKeepDevice");
        ANIM_PROP_NAMES.put(0x28, "soundMixMedia");

        // Animation2 properties
        ANIM2_PROP_NAMES.put(0x01, "perFrameHook");
        ANIM2_PROP_NAMES.put(0x02, "number of castMembers");
        ANIM2_PROP_NAMES.put(0x03, "number of menus");
        ANIM2_PROP_NAMES.put(0x04, "number of castLibs");
        ANIM2_PROP_NAMES.put(0x05, "number of xtras");

        // Movie properties
        MOVIE_PROP_NAMES.put(0x00, "floatPrecision");
        MOVIE_PROP_NAMES.put(0x01, "mouseDownScript");
        MOVIE_PROP_NAMES.put(0x02, "mouseUpScript");
        MOVIE_PROP_NAMES.put(0x03, "keyDownScript");
        MOVIE_PROP_NAMES.put(0x04, "keyUpScript");
        MOVIE_PROP_NAMES.put(0x05, "timeoutScript");
        MOVIE_PROP_NAMES.put(0x06, "short time");
        MOVIE_PROP_NAMES.put(0x07, "abbr time");
        MOVIE_PROP_NAMES.put(0x08, "long time");
        MOVIE_PROP_NAMES.put(0x09, "short date");
        MOVIE_PROP_NAMES.put(0x0a, "abbr date");
        MOVIE_PROP_NAMES.put(0x0b, "long date");

        // Sprite properties
        SPRITE_PROP_NAMES.put(0x01, "type");
        SPRITE_PROP_NAMES.put(0x02, "backColor");
        SPRITE_PROP_NAMES.put(0x03, "bottom");
        SPRITE_PROP_NAMES.put(0x04, "castNum");
        SPRITE_PROP_NAMES.put(0x05, "constraint");
        SPRITE_PROP_NAMES.put(0x06, "cursor");
        SPRITE_PROP_NAMES.put(0x07, "foreColor");
        SPRITE_PROP_NAMES.put(0x08, "height");
        SPRITE_PROP_NAMES.put(0x09, "immediate");
        SPRITE_PROP_NAMES.put(0x0a, "ink");
        SPRITE_PROP_NAMES.put(0x0b, "left");
        SPRITE_PROP_NAMES.put(0x0c, "lineSize");
        SPRITE_PROP_NAMES.put(0x0d, "locH");
        SPRITE_PROP_NAMES.put(0x0e, "locV");
        SPRITE_PROP_NAMES.put(0x0f, "movieRate");
        SPRITE_PROP_NAMES.put(0x10, "movieTime");
        SPRITE_PROP_NAMES.put(0x11, "pattern");
        SPRITE_PROP_NAMES.put(0x12, "puppet");
        SPRITE_PROP_NAMES.put(0x13, "right");
        SPRITE_PROP_NAMES.put(0x14, "startTime");
        SPRITE_PROP_NAMES.put(0x15, "stopTime");
        SPRITE_PROP_NAMES.put(0x16, "stretch");
        SPRITE_PROP_NAMES.put(0x17, "top");
        SPRITE_PROP_NAMES.put(0x18, "trails");
        SPRITE_PROP_NAMES.put(0x19, "visible");
        SPRITE_PROP_NAMES.put(0x1a, "volume");
        SPRITE_PROP_NAMES.put(0x1b, "width");
        SPRITE_PROP_NAMES.put(0x1c, "blend");
        SPRITE_PROP_NAMES.put(0x1d, "scriptNum");
        SPRITE_PROP_NAMES.put(0x1e, "moveableSprite");
        SPRITE_PROP_NAMES.put(0x1f, "editableText");
        SPRITE_PROP_NAMES.put(0x20, "scoreColor");
        SPRITE_PROP_NAMES.put(0x21, "loc");
        SPRITE_PROP_NAMES.put(0x22, "rect");
        SPRITE_PROP_NAMES.put(0x23, "memberNum");
        SPRITE_PROP_NAMES.put(0x24, "castLibNum");
        SPRITE_PROP_NAMES.put(0x25, "member");
        SPRITE_PROP_NAMES.put(0x26, "scriptInstanceList");
        SPRITE_PROP_NAMES.put(0x27, "currentTime");
        SPRITE_PROP_NAMES.put(0x28, "mostRecentCuePoint");
        SPRITE_PROP_NAMES.put(0x29, "tweened");
        SPRITE_PROP_NAMES.put(0x2a, "name");
    }

    public static String getOpcodeName(OpCode opcode) {
        return OPCODE_NAMES.getOrDefault(opcode, "unknown");
    }

    public static String getAnimPropName(int nameId) {
        return ANIM_PROP_NAMES.getOrDefault(nameId, "unknown_anim_prop_" + nameId);
    }

    public static String getAnim2PropName(int nameId) {
        return ANIM2_PROP_NAMES.getOrDefault(nameId, "unknown_anim2_prop_" + nameId);
    }

    public static String getSpritePropName(int nameId) {
        return SPRITE_PROP_NAMES.getOrDefault(nameId, "unknown_sprite_prop_" + nameId);
    }

    public static String getMoviePropName(int nameId) {
        return MOVIE_PROP_NAMES.getOrDefault(nameId, "unknown_movie_prop_" + nameId);
    }

    public static String getSoundPropName(int propertyId) {
        switch (propertyId) {
            case 0x01: return "volume";
            case 0x02: return "pan";
            case 0x03: return "loopCount";
            case 0x04: return "startTime";
            case 0x05: return "endTime";
            case 0x06: return "loopStartTime";
            case 0x07: return "loopEndTime";
            default: return "unknown_sound_prop_" + propertyId;
        }
    }

    public static Map<Integer, String> getMoviePropNames() {
        return new HashMap<>(MOVIE_PROP_NAMES);
    }

    public static Map<Integer, String> getSpritePropNames() {
        return new HashMap<>(SPRITE_PROP_NAMES);
    }
}
