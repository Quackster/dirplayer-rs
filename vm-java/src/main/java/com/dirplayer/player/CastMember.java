package com.dirplayer.player;

import com.dirplayer.director.MemberType;
import com.dirplayer.player.bitmap.BitmapRef;

/**
 * A cast member in a cast library.
 * Port of Rust CastMember struct.
 */
public class CastMember {
    public int number;  // Member number within cast
    public CastMemberRef memberRef;
    public MemberType memberType;
    public MemberType type;  // Alias for memberType for compatibility
    public String name;
    public int scriptId;
    public Object specificData;  // Type-specific data (bitmap, sound, etc.)
    public Object mediaData;     // Raw media data
    public boolean isLoaded;

    // Bitmap specific
    public int bitmapWidth;
    public int bitmapHeight;
    public int bitDepth;
    public int regPointX;
    public int regPointY;
    public int paletteRef;

    // Text/Field specific
    public String text;
    public int textWidth;
    public int textHeight;
    public String font;
    public int fontSize;
    public String fontStyle;
    public String alignment;

    // Sound specific
    public int sampleRate;
    public int sampleSize;
    public int channels;
    public long sampleCount;
    public boolean loopEnabled;
    public int duration;
    public int channelCount;

    // Script specific
    public int scriptType;

    // Common visual properties
    public ColorRef color;
    public ColorRef bgColor;

    // Bitmap rendering
    public BitmapRef bitmap;
    public int depth;
    public boolean usePalette;

    public CastMember() {
        this.memberRef = new CastMemberRef();
        this.memberType = MemberType.Null;
        this.type = MemberType.Null;
        this.name = "";
        this.scriptId = 0;
        this.isLoaded = false;
    }

    public CastMember(CastMemberRef ref, MemberType memberType) {
        this.memberRef = ref;
        this.memberType = memberType;
        this.type = memberType;
        this.name = "";
        this.scriptId = 0;
        this.isLoaded = false;
    }

    public boolean isBitmap() {
        return memberType == MemberType.Bitmap;
    }

    public boolean isText() {
        return memberType == MemberType.Text;
    }

    public boolean isField() {
        return memberType == MemberType.Button || memberType == MemberType.RTE;
    }

    public boolean isScript() {
        return memberType == MemberType.Script;
    }

    public boolean isSound() {
        return memberType == MemberType.Sound;
    }

    public boolean isShape() {
        return memberType == MemberType.Shape;
    }

    public boolean isFilmLoop() {
        return memberType == MemberType.FilmLoop;
    }

    public boolean isDigitalVideo() {
        return memberType == MemberType.DigitalVideo;
    }

    // Getter methods for rendering
    public MemberType getMemberType() {
        return memberType;
    }

    public String getName() {
        return name;
    }

    // Type-specific member accessors for rendering
    // In a full implementation these would return proper typed objects

    public int getImageRef() {
        // Return the bitmap ID from the bitmap reference
        if (bitmap != null) {
            return bitmap.bitmapId;
        }
        return -1;
    }

    public int getRegPointX() {
        return regPointX;
    }

    public int getRegPointY() {
        return regPointY;
    }

    public int getBitmapWidth() {
        return bitmapWidth;
    }

    public int getBitmapHeight() {
        return bitmapHeight;
    }

    /**
     * Create a copy of this cast member.
     */
    public CastMember copy() {
        CastMember copy = new CastMember();
        copy.number = this.number;
        copy.memberRef = this.memberRef != null ? new CastMemberRef(this.memberRef.getCastLib(), this.memberRef.getCastMember()) : null;
        copy.memberType = this.memberType;
        copy.name = this.name;
        copy.scriptId = this.scriptId;
        copy.specificData = this.specificData;
        copy.mediaData = this.mediaData;
        copy.isLoaded = this.isLoaded;

        // Bitmap specific
        copy.bitmapWidth = this.bitmapWidth;
        copy.bitmapHeight = this.bitmapHeight;
        copy.bitDepth = this.bitDepth;
        copy.regPointX = this.regPointX;
        copy.regPointY = this.regPointY;
        copy.paletteRef = this.paletteRef;

        // Text/Field specific
        copy.text = this.text;
        copy.textWidth = this.textWidth;
        copy.textHeight = this.textHeight;
        copy.font = this.font;
        copy.fontSize = this.fontSize;
        copy.fontStyle = this.fontStyle;
        copy.alignment = this.alignment;

        // Sound specific
        copy.sampleRate = this.sampleRate;
        copy.sampleSize = this.sampleSize;
        copy.channels = this.channels;
        copy.sampleCount = this.sampleCount;
        copy.loopEnabled = this.loopEnabled;
        copy.duration = this.duration;
        copy.channelCount = this.channelCount;

        // Script specific
        copy.scriptType = this.scriptType;

        // Common visual properties
        copy.color = this.color;
        copy.bgColor = this.bgColor;

        // Bitmap rendering
        copy.bitmap = this.bitmap;
        copy.depth = this.depth;
        copy.usePalette = this.usePalette;

        return copy;
    }

    /**
     * Get text content for text/field members.
     */
    public String getText() {
        return text;
    }

    /**
     * Set text content for text/field members.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Get width based on member type.
     */
    public int getWidth() {
        if (isBitmap()) {
            return bitmapWidth;
        } else if (isText() || isField()) {
            return textWidth;
        }
        return 0;
    }

    /**
     * Get height based on member type.
     */
    public int getHeight() {
        if (isBitmap()) {
            return bitmapHeight;
        } else if (isText() || isField()) {
            return textHeight;
        }
        return 0;
    }

    /**
     * Set width based on member type.
     */
    public void setWidth(int width) {
        if (isBitmap()) {
            this.bitmapWidth = width;
        } else if (isText() || isField()) {
            this.textWidth = width;
        }
    }

    /**
     * Set height based on member type.
     */
    public void setHeight(int height) {
        if (isBitmap()) {
            this.bitmapHeight = height;
        } else if (isText() || isField()) {
            this.textHeight = height;
        }
    }
}
