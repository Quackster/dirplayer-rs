package com.dirplayer.player;

/**
 * Integer rectangle for stage/sprite bounds.
 * Port of Rust IntRect struct.
 */
public class IntRect {
    public int left;
    public int top;
    public int right;
    public int bottom;

    public IntRect() {
        this.left = 0;
        this.top = 0;
        this.right = 0;
        this.bottom = 0;
    }

    public IntRect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public int getWidth() {
        return right - left;
    }

    public int getHeight() {
        return bottom - top;
    }

    // Alias methods without "get" prefix
    public int width() {
        return right - left;
    }

    public int height() {
        return bottom - top;
    }

    public boolean contains(int x, int y) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    public boolean intersects(IntRect other) {
        return !(other.left >= right || other.right <= left ||
                other.top >= bottom || other.bottom <= top);
    }

    public IntRect intersection(IntRect other) {
        int newLeft = Math.max(left, other.left);
        int newTop = Math.max(top, other.top);
        int newRight = Math.min(right, other.right);
        int newBottom = Math.min(bottom, other.bottom);

        if (newRight > newLeft && newBottom > newTop) {
            return new IntRect(newLeft, newTop, newRight, newBottom);
        }
        return new IntRect(0, 0, 0, 0);
    }

    public IntRect union(IntRect other) {
        return new IntRect(
            Math.min(left, other.left),
            Math.min(top, other.top),
            Math.max(right, other.right),
            Math.max(bottom, other.bottom)
        );
    }

    public IntRect offset(int dx, int dy) {
        return new IntRect(left + dx, top + dy, right + dx, bottom + dy);
    }

    public IntRect copy() {
        return new IntRect(left, top, right, bottom);
    }

    @Override
    public String toString() {
        return "rect(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
}
