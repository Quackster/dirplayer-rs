package com.dirplayer.player;

/**
 * A sprite in a score channel.
 * Port of Rust Sprite struct.
 */
public class Sprite {
    public int number;
    public CastMemberRef memberRef;
    public int locH;
    public int locV;
    public int width;
    public int height;
    public int ink;
    public int blend;
    public boolean visible;
    public boolean puppet;
    public boolean moveable;
    public boolean editableText;
    public int foreColor;
    public int backColor;
    public int constraint;
    public boolean trails;
    public boolean stretch;
    public int rotation;
    public int skew;
    public boolean flipH;
    public boolean flipV;

    // Behavior/script list
    public int scriptNum;
    public java.util.List<Integer> scriptInstanceList;

    // Cursor
    public CursorRef cursor;

    // Timeline properties
    public int startTime;
    public int stopTime;
    public int movieRate;
    public int movieTime;
    public int currentTime;

    // Type-specific properties
    public int lineSize;
    public int pattern;

    public Sprite(int number) {
        this.number = number;
        this.memberRef = new CastMemberRef();
        this.locH = 0;
        this.locV = 0;
        this.width = 0;
        this.height = 0;
        this.ink = 0;
        this.blend = 100;
        this.visible = true;
        this.puppet = false;
        this.moveable = false;
        this.editableText = false;
        this.foreColor = 255;
        this.backColor = 0;
        this.constraint = 0;
        this.trails = false;
        this.stretch = false;
        this.rotation = 0;
        this.skew = 0;
        this.flipH = false;
        this.flipV = false;
        this.scriptNum = 0;
        this.scriptInstanceList = new java.util.ArrayList<>();
        this.cursor = new CursorRef();
        this.startTime = 0;
        this.stopTime = 0;
        this.movieRate = 0;
        this.movieTime = 0;
        this.currentTime = 0;
        this.lineSize = 1;
        this.pattern = 0;
    }

    public int getLeft() {
        return locH - width / 2;
    }

    public int getTop() {
        return locV - height / 2;
    }

    public int getRight() {
        return locH + width / 2;
    }

    public int getBottom() {
        return locV + height / 2;
    }

    public IntRect getRect() {
        return new IntRect(getLeft(), getTop(), getRight(), getBottom());
    }

    public boolean containsPoint(int x, int y) {
        return x >= getLeft() && x < getRight() && y >= getTop() && y < getBottom();
    }

    public void setLoc(int h, int v) {
        this.locH = h;
        this.locV = v;
    }

    public void setRect(int left, int top, int right, int bottom) {
        this.width = right - left;
        this.height = bottom - top;
        this.locH = left + width / 2;
        this.locV = top + height / 2;
    }

    // Color properties for rendering
    private ColorRef color;
    private ColorRef bgColor;
    private float rotationFloat;
    private boolean hasSizeTweened;
    private boolean hasSizeChanged;

    public CastMemberRef getMember() {
        return memberRef;
    }

    public void setMember(CastMemberRef member) {
        this.memberRef = member;
    }

    public int getNumber() {
        return number;
    }

    public int getLocH() {
        return locH;
    }

    public int getLocV() {
        return locV;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getInk() {
        return ink;
    }

    public int getBlend() {
        return blend;
    }

    public ColorRef getColor() {
        if (color == null) {
            color = ColorRef.paletteIndex(foreColor);
        }
        return color;
    }

    public void setColor(ColorRef color) {
        this.color = color;
    }

    public ColorRef getBgColor() {
        if (bgColor == null) {
            bgColor = ColorRef.paletteIndex(backColor);
        }
        return bgColor;
    }

    public void setBgColor(ColorRef bgColor) {
        this.bgColor = bgColor;
    }

    public float getRotation() {
        return rotationFloat;
    }

    public void setRotation(float rotation) {
        this.rotationFloat = rotation;
    }

    public boolean isFlipH() {
        return flipH;
    }

    public boolean isFlipV() {
        return flipV;
    }

    public boolean hasSizeTweened() {
        return hasSizeTweened;
    }

    public void setHasSizeTweened(boolean hasSizeTweened) {
        this.hasSizeTweened = hasSizeTweened;
    }

    public boolean hasSizeChanged() {
        return hasSizeChanged;
    }

    public void setHasSizeChanged(boolean hasSizeChanged) {
        this.hasSizeChanged = hasSizeChanged;
    }

    public Sprite copy() {
        Sprite copy = new Sprite(number);
        copy.memberRef = memberRef;
        copy.locH = locH;
        copy.locV = locV;
        copy.width = width;
        copy.height = height;
        copy.ink = ink;
        copy.blend = blend;
        copy.visible = visible;
        copy.puppet = puppet;
        copy.moveable = moveable;
        copy.editableText = editableText;
        copy.foreColor = foreColor;
        copy.backColor = backColor;
        copy.constraint = constraint;
        copy.trails = trails;
        copy.stretch = stretch;
        copy.rotation = rotation;
        copy.skew = skew;
        copy.flipH = flipH;
        copy.flipV = flipV;
        copy.scriptNum = scriptNum;
        copy.scriptInstanceList = new java.util.ArrayList<>(scriptInstanceList);
        copy.cursor = cursor;
        copy.startTime = startTime;
        copy.stopTime = stopTime;
        copy.movieRate = movieRate;
        copy.movieTime = movieTime;
        copy.currentTime = currentTime;
        copy.lineSize = lineSize;
        copy.pattern = pattern;
        copy.color = color;
        copy.bgColor = bgColor;
        copy.rotationFloat = rotationFloat;
        copy.hasSizeTweened = hasSizeTweened;
        copy.hasSizeChanged = hasSizeChanged;
        return copy;
    }
}
