package com.dirplayer.director.lingo;

/**
 * Enumeration of all Lingo datum types.
 * Port of Rust DatumType enum.
 */
public enum DatumType {
    Null("null"),
    Void("void"),
    Symbol("symbol"),
    VarRef("var_ref"),
    ScriptInstanceRef("script_instance"),
    ScriptRef("script_ref"),
    CastLibRef("cast_lib"),
    CastMemberRef("cast_member"),
    StageRef("stage"),
    SpriteRef("sprite_ref"),
    StringChunk("string_chunk"),
    String("string"),
    Int("int"),
    Float("float"),
    List("list"),
    XmlChildNodes("list"),
    ArgList("arg_list"),
    ArgListNoRet("arg_list_no_ret"),
    PropList("prop_list"),
    Eval("eval"),
    Rect("rect"),
    Point("point"),
    SoundRef("sound"),
    SoundChannel("sound_channel"),
    CursorRef("cursor_ref"),
    TimeoutRef("timeout"),
    TimeoutFactory("timeout_factory"),
    TimeoutInstance("timeout_instance"),
    ColorRef("color_ref"),
    BitmapRef("bitmap_ref"),
    PaletteRef("palette_ref"),
    Xtra("xtra"),
    XtraInstance("xtra_instance"),
    Matte("matte"),
    PlayerRef("player_ref"),
    MovieRef("movie_ref"),
    XmlRef("xml"),
    DateRef("date"),
    MathRef("math"),
    Vector("vector");

    private final String typeName;

    DatumType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
