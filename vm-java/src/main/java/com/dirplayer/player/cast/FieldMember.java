package com.dirplayer.player.cast;

import com.dirplayer.director.FieldInfo;

/**
 * Field cast member data.
 * Port of Rust FieldMember struct.
 */
public class FieldMember {
    public String text;
    public String alignment;
    public boolean wordWrap;
    public String font;
    public String fontStyle;
    public int fontSize;
    public int fixedLineSpace;
    public int topSpacing;
    public String boxType;
    public boolean antiAlias;
    public int width;
    public boolean autoTab;
    public boolean editable;
    public int border;
    public int backColor;

    public FieldMember() {
        this.text = "";
        this.alignment = "left";
        this.wordWrap = true;
        this.font = "Arial";
        this.fontStyle = "plain";
        this.fontSize = 12;
        this.fixedLineSpace = 0;
        this.topSpacing = 0;
        this.boxType = "adjust";
        this.antiAlias = false;
        this.width = 100;
        this.autoTab = false;
        this.editable = false;
        this.border = 0;
        this.backColor = 0;
    }

    public static FieldMember fromFieldInfo(FieldInfo info) {
        FieldMember member = new FieldMember();
        member.alignment = info.alignmentStr();
        member.wordWrap = info.wordwrap();
        member.font = info.fontName();
        member.fixedLineSpace = info.height;
        member.topSpacing = info.scrollTop;
        member.boxType = info.boxTypeStr();
        member.width = info.width;
        member.autoTab = info.autoTab();
        member.editable = info.editable();
        member.border = info.border;
        member.backColor = info.bgColor();
        return member;
    }

    public FieldMember copy() {
        FieldMember copy = new FieldMember();
        copy.text = text;
        copy.alignment = alignment;
        copy.wordWrap = wordWrap;
        copy.font = font;
        copy.fontStyle = fontStyle;
        copy.fontSize = fontSize;
        copy.fixedLineSpace = fixedLineSpace;
        copy.topSpacing = topSpacing;
        copy.boxType = boxType;
        copy.antiAlias = antiAlias;
        copy.width = width;
        copy.autoTab = autoTab;
        copy.editable = editable;
        copy.border = border;
        copy.backColor = backColor;
        return copy;
    }

    // Getter methods for rendering
    public String getFont() { return font; }
    public Integer getFontSize() { return fontSize; }
    public String getText() { return text; }
    public int getFixedLineSpace() { return fixedLineSpace; }
    public int getTopSpacing() { return topSpacing; }
}
