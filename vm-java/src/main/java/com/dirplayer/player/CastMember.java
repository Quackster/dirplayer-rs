package com.dirplayer.player;

import com.dirplayer.director.MemberType;

/**
 * A cast member in a cast library.
 * Port of Rust CastMember struct.
 */
public class CastMember {
    public CastMemberRef memberRef;
    public MemberType memberType;
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

    // Sound specific
    public int sampleRate;
    public int sampleSize;
    public int channels;
    public long sampleCount;
    public boolean loopEnabled;

    // Script specific
    public int scriptType;

    public CastMember() {
        this.memberRef = new CastMemberRef();
        this.memberType = MemberType.Null;
        this.name = "";
        this.scriptId = 0;
        this.isLoaded = false;
    }

    public CastMember(CastMemberRef ref, MemberType type) {
        this.memberRef = ref;
        this.memberType = type;
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
        // For bitmap members, this would be the bitmap manager reference
        return memberRef.hashCode();  // Placeholder
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
}
