package com.dirplayer.player;

/**
 * A sprite in a score channel.
 * Port of Rust Sprite struct.
 */
public class Sprite {
    public int number;
    public String name;
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
    public int stretch;  // Changed to int to match Rust
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

    // Z-order for sprite layering
    public int locZ;

    // Span tracking
    public boolean entered;
    public boolean exited;

    // Base values for tweening (stored when sprite enters)
    public int baseLocH;
    public int baseLocV;
    public int baseWidth;
    public int baseHeight;
    public double baseRotation;
    public int baseBlend;
    public double baseSkew;
    public ColorRef baseColor;
    public ColorRef baseBgColor;

    // Tween color flags
    public boolean hasForeColor;
    public boolean hasBackColor;

    // Quad for arbitrary quadrilateral rendering [topLeft, topRight, bottomRight, bottomLeft]
    public int[][] quad;

    // Size change flags
    public boolean hasSizeTweened;
    public boolean hasSizeChanged;
    public boolean bitmapSizeOwnedBySprite;

    public Sprite(int number) {
        this.number = number;
        this.name = "";
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
        this.stretch = 0;
        this.flipH = false;
        this.flipV = false;
        this.scriptNum = 0;
        this.scriptInstanceList = new java.util.ArrayList<>();
        this.cursor = null;
        this.startTime = 0;
        this.stopTime = 0;
        this.movieRate = 0;
        this.movieTime = 0;
        this.currentTime = 0;
        this.lineSize = 1;
        this.pattern = 0;
        this.locZ = number;  // Default to sprite number like Rust
        this.entered = false;
        this.exited = false;
        this.baseLocH = 0;
        this.baseLocV = 0;
        this.baseWidth = 0;
        this.baseHeight = 0;
        this.baseRotation = 0.0;
        this.baseBlend = 100;
        this.baseSkew = 0.0;
        this.baseColor = ColorRef.paletteIndex(255);
        this.baseBgColor = ColorRef.paletteIndex(0);
        this.hasForeColor = false;
        this.hasBackColor = false;
        this.quad = null;
        this.hasSizeTweened = false;
        this.hasSizeChanged = false;
        this.bitmapSizeOwnedBySprite = false;

        // Initialize colors
        this.color = ColorRef.paletteIndex(255);
        this.bgColor = ColorRef.paletteIndex(0);
        this.rotationFloat = 0.0f;
        this.skew = 0.0f;
    }

    /**
     * Reset the sprite to default values.
     * Port of Rust Sprite::reset.
     */
    public void reset() {
        this.name = "";
        this.puppet = false;
        this.visible = true;
        this.stretch = 0;
        this.locH = 0;
        this.locV = 0;
        this.locZ = this.number;
        this.width = 0;
        this.height = 0;
        this.ink = 0;
        this.blend = 100;
        this.rotationFloat = 0.0f;
        this.skew = 0.0f;
        this.flipH = false;
        this.flipV = false;
        this.backColor = 0;
        this.color = ColorRef.paletteIndex(255);
        this.bgColor = ColorRef.paletteIndex(0);
        this.memberRef = null;
        this.scriptInstanceList.clear();
        this.cursor = null;
        this.editableText = false;
        this.entered = false;
        this.exited = false;
        this.quad = null;
        this.foreColor = 255;
        this.hasForeColor = false;
        this.hasBackColor = false;
        this.hasSizeTweened = false;
        this.hasSizeChanged = false;
        this.bitmapSizeOwnedBySprite = false;
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
    private float skew;

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

    public float getSkew() {
        return skew;
    }

    public void setSkew(float skew) {
        this.skew = skew;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int[][] getQuad() {
        return quad;
    }

    public void setQuad(int[][] quad) {
        this.quad = quad;
    }

    public boolean isBitmapSizeOwnedBySprite() {
        return bitmapSizeOwnedBySprite;
    }

    public void setBitmapSizeOwnedBySprite(boolean bitmapSizeOwnedBySprite) {
        this.bitmapSizeOwnedBySprite = bitmapSizeOwnedBySprite;
    }

    public CursorRef getCursor() {
        return cursor;
    }

    public void setCursor(CursorRef cursor) {
        this.cursor = cursor;
    }

    public Sprite copy() {
        Sprite copy = new Sprite(number);
        copy.name = name;
        copy.memberRef = memberRef != null ? memberRef.copy() : null;
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
        copy.rotationFloat = rotationFloat;
        copy.skew = skew;
        copy.flipH = flipH;
        copy.flipV = flipV;
        copy.scriptNum = scriptNum;
        copy.scriptInstanceList = new java.util.ArrayList<>(scriptInstanceList);
        copy.cursor = cursor != null ? cursor.copy() : null;
        copy.startTime = startTime;
        copy.stopTime = stopTime;
        copy.movieRate = movieRate;
        copy.movieTime = movieTime;
        copy.currentTime = currentTime;
        copy.lineSize = lineSize;
        copy.pattern = pattern;
        copy.color = color != null ? color.copy() : null;
        copy.bgColor = bgColor != null ? bgColor.copy() : null;
        copy.hasSizeTweened = hasSizeTweened;
        copy.hasSizeChanged = hasSizeChanged;
        copy.locZ = locZ;
        copy.entered = entered;
        copy.exited = exited;
        copy.baseLocH = baseLocH;
        copy.baseLocV = baseLocV;
        copy.baseWidth = baseWidth;
        copy.baseHeight = baseHeight;
        copy.baseRotation = baseRotation;
        copy.baseBlend = baseBlend;
        copy.baseSkew = baseSkew;
        copy.baseColor = baseColor != null ? baseColor.copy() : null;
        copy.baseBgColor = baseBgColor != null ? baseBgColor.copy() : null;
        copy.hasForeColor = hasForeColor;
        copy.hasBackColor = hasBackColor;
        copy.quad = quad != null ? copyQuad(quad) : null;
        copy.bitmapSizeOwnedBySprite = bitmapSizeOwnedBySprite;
        return copy;
    }

    private static int[][] copyQuad(int[][] quad) {
        int[][] copy = new int[4][2];
        for (int i = 0; i < 4; i++) {
            copy[i][0] = quad[i][0];
            copy[i][1] = quad[i][1];
        }
        return copy;
    }
}
