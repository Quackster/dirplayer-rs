package com.dirplayer.player.cast;

import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ColorRef;

/**
 * Complete cast member with type-specific data.
 * Port of Rust CastMember struct with CastMemberType enum.
 */
public class CastMemberData {
    public int number;
    public String name;
    public CastMemberType memberType;
    public ColorRef color;
    public ColorRef bgColor;

    // Type-specific member data (only one will be non-null)
    private FieldMember fieldMember;
    private TextMember textMember;
    private ScriptMember scriptMember;
    private BitmapMember bitmapMember;
    private PaletteMember paletteMember;
    private ShapeMember shapeMember;
    private FilmLoopMember filmLoopMember;
    private SoundMember soundMember;
    private FontMember fontMember;
    private FlashMember flashMember;

    public CastMemberData(int number, CastMemberType type) {
        this.number = number;
        this.name = "";
        this.memberType = type;
        this.color = ColorRef.paletteIndex(255);  // Black
        this.bgColor = ColorRef.paletteIndex(0);   // White
    }

    // Getters for type-specific data
    public FieldMember getFieldMember() {
        return fieldMember;
    }

    public void setFieldMember(FieldMember member) {
        this.fieldMember = member;
        this.memberType = CastMemberType.Field;
    }

    public TextMember getTextMember() {
        return textMember;
    }

    public void setTextMember(TextMember member) {
        this.textMember = member;
        this.memberType = CastMemberType.Text;
    }

    public ScriptMember getScriptMember() {
        return scriptMember;
    }

    public void setScriptMember(ScriptMember member) {
        this.scriptMember = member;
        this.memberType = CastMemberType.Script;
    }

    public BitmapMember getBitmapMember() {
        return bitmapMember;
    }

    public void setBitmapMember(BitmapMember member) {
        this.bitmapMember = member;
        this.memberType = CastMemberType.Bitmap;
    }

    public PaletteMember getPaletteMember() {
        return paletteMember;
    }

    public void setPaletteMember(PaletteMember member) {
        this.paletteMember = member;
        this.memberType = CastMemberType.Palette;
    }

    public ShapeMember getShapeMember() {
        return shapeMember;
    }

    public void setShapeMember(ShapeMember member) {
        this.shapeMember = member;
        this.memberType = CastMemberType.Shape;
    }

    public FilmLoopMember getFilmLoopMember() {
        return filmLoopMember;
    }

    public void setFilmLoopMember(FilmLoopMember member) {
        this.filmLoopMember = member;
        this.memberType = CastMemberType.FilmLoop;
    }

    public SoundMember getSoundMember() {
        return soundMember;
    }

    public void setSoundMember(SoundMember member) {
        this.soundMember = member;
        this.memberType = CastMemberType.Sound;
    }

    public FontMember getFontMember() {
        return fontMember;
    }

    public void setFontMember(FontMember member) {
        this.fontMember = member;
        this.memberType = CastMemberType.Font;
    }

    public FlashMember getFlashMember() {
        return flashMember;
    }

    public void setFlashMember(FlashMember member) {
        this.flashMember = member;
        this.memberType = CastMemberType.Flash;
    }

    /**
     * Get the type string for Lingo.
     */
    public String getTypeString() {
        return memberType.getSymbolString();
    }

    /**
     * Check if this is a specific type.
     */
    public boolean isType(CastMemberType type) {
        return memberType == type;
    }

    public boolean isField() {
        return memberType == CastMemberType.Field;
    }

    public boolean isText() {
        return memberType == CastMemberType.Text;
    }

    public boolean isScript() {
        return memberType == CastMemberType.Script;
    }

    public boolean isBitmap() {
        return memberType == CastMemberType.Bitmap;
    }

    public boolean isPalette() {
        return memberType == CastMemberType.Palette;
    }

    public boolean isShape() {
        return memberType == CastMemberType.Shape;
    }

    public boolean isFilmLoop() {
        return memberType == CastMemberType.FilmLoop;
    }

    public boolean isSound() {
        return memberType == CastMemberType.Sound;
    }

    public boolean isFont() {
        return memberType == CastMemberType.Font;
    }

    public boolean isFlash() {
        return memberType == CastMemberType.Flash;
    }

    /**
     * Get width based on member type.
     */
    public int getWidth() {
        if (bitmapMember != null && bitmapMember.info != null) {
            return bitmapMember.info.width;
        }
        if (fieldMember != null) {
            return fieldMember.width;
        }
        if (textMember != null) {
            return textMember.width;
        }
        if (shapeMember != null && shapeMember.shapeInfo != null) {
            return shapeMember.shapeInfo.width;
        }
        return 0;
    }

    /**
     * Get height based on member type.
     */
    public int getHeight() {
        if (bitmapMember != null && bitmapMember.info != null) {
            return bitmapMember.info.height;
        }
        if (shapeMember != null && shapeMember.shapeInfo != null) {
            return shapeMember.shapeInfo.height;
        }
        return 0;
    }

    /**
     * Get the registration point based on member type.
     */
    public int[] getRegPoint() {
        if (bitmapMember != null) {
            return new int[]{bitmapMember.regPointX, bitmapMember.regPointY};
        }
        // Default to center for other types
        return new int[]{getWidth() / 2, getHeight() / 2};
    }

    @Override
    public String toString() {
        return "CastMember(" + number + ", " + memberType + ", \"" + name + "\")";
    }
}
