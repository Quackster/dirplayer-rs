package com.dirplayer.director.chunks;

import com.dirplayer.director.BitmapInfo;
import com.dirplayer.director.FieldInfo;
import com.dirplayer.director.FilmLoopInfo;
import com.dirplayer.director.FontInfo;
import com.dirplayer.director.ScriptType;
import com.dirplayer.director.ShapeInfo;
import com.dirplayer.director.SoundInfo;

/**
 * Cast member specific data - type-specific data for cast members.
 * Port of Rust CastMemberSpecificData enum.
 */
public class CastMemberSpecificData {
    public enum Type {
        NONE,
        SCRIPT,
        BITMAP,
        SHAPE,
        FILM_LOOP,
        SOUND,
        FONT,
        FIELD
    }

    private Type type;
    private ScriptType scriptType;
    private BitmapInfo bitmapInfo;
    private ShapeInfo shapeInfo;
    private FilmLoopInfo filmLoopInfo;
    private SoundInfo soundInfo;
    private FontInfo fontInfo;
    private FieldInfo fieldInfo;

    private CastMemberSpecificData(Type type) {
        this.type = type;
    }

    public static CastMemberSpecificData none() {
        return new CastMemberSpecificData(Type.NONE);
    }

    public static CastMemberSpecificData script(ScriptType scriptType) {
        CastMemberSpecificData data = new CastMemberSpecificData(Type.SCRIPT);
        data.scriptType = scriptType;
        return data;
    }

    public static CastMemberSpecificData bitmap(BitmapInfo bitmapInfo) {
        CastMemberSpecificData data = new CastMemberSpecificData(Type.BITMAP);
        data.bitmapInfo = bitmapInfo;
        return data;
    }

    public static CastMemberSpecificData shape(ShapeInfo shapeInfo) {
        CastMemberSpecificData data = new CastMemberSpecificData(Type.SHAPE);
        data.shapeInfo = shapeInfo;
        return data;
    }

    public static CastMemberSpecificData filmLoop(FilmLoopInfo filmLoopInfo) {
        CastMemberSpecificData data = new CastMemberSpecificData(Type.FILM_LOOP);
        data.filmLoopInfo = filmLoopInfo;
        return data;
    }

    public static CastMemberSpecificData sound(SoundInfo soundInfo) {
        CastMemberSpecificData data = new CastMemberSpecificData(Type.SOUND);
        data.soundInfo = soundInfo;
        return data;
    }

    public static CastMemberSpecificData font(FontInfo fontInfo) {
        CastMemberSpecificData data = new CastMemberSpecificData(Type.FONT);
        data.fontInfo = fontInfo;
        return data;
    }

    public static CastMemberSpecificData field(FieldInfo fieldInfo) {
        CastMemberSpecificData data = new CastMemberSpecificData(Type.FIELD);
        data.fieldInfo = fieldInfo;
        return data;
    }

    public Type getType() {
        return type;
    }

    public ScriptType getScriptType() {
        return type == Type.SCRIPT ? scriptType : null;
    }

    public BitmapInfo getBitmapInfo() {
        return type == Type.BITMAP ? bitmapInfo : null;
    }

    public ShapeInfo getShapeInfo() {
        return type == Type.SHAPE ? shapeInfo : null;
    }

    public FilmLoopInfo getFilmLoopInfo() {
        return type == Type.FILM_LOOP ? filmLoopInfo : null;
    }

    public SoundInfo getSoundInfo() {
        return type == Type.SOUND ? soundInfo : null;
    }

    public FontInfo getFontInfo() {
        return type == Type.FONT ? fontInfo : null;
    }

    public FieldInfo getFieldInfo() {
        return type == Type.FIELD ? fieldInfo : null;
    }
}
