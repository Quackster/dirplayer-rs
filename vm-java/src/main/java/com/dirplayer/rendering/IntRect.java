package com.dirplayer.rendering;

/**
 * Integer rectangle for rendering operations.
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

    public static IntRect from(int left, int top, int right, int bottom) {
        return new IntRect(left, top, right, bottom);
    }

    public static IntRect fromSize(int x, int y, int width, int height) {
        return new IntRect(x, y, x + width, y + height);
    }

    public int width() {
        return right - left;
    }

    public int height() {
        return bottom - top;
    }

    public boolean contains(int x, int y) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    public boolean isEmpty() {
        return width() <= 0 || height() <= 0;
    }

    public IntRect intersect(IntRect other) {
        int newLeft = Math.max(left, other.left);
        int newTop = Math.max(top, other.top);
        int newRight = Math.min(right, other.right);
        int newBottom = Math.min(bottom, other.bottom);
        if (newRight <= newLeft || newBottom <= newTop) {
            return new IntRect(0, 0, 0, 0);
        }
        return new IntRect(newLeft, newTop, newRight, newBottom);
    }

    public IntRect union(IntRect other) {
        if (isEmpty()) return other.copy();
        if (other.isEmpty()) return copy();
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
        return String.format("IntRect(%d, %d, %d, %d)", left, top, right, bottom);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        IntRect other = (IntRect) obj;
        return left == other.left && top == other.top &&
               right == other.right && bottom == other.bottom;
    }

    @Override
    public int hashCode() {
        int result = left;
        result = 31 * result + top;
        result = 31 * result + right;
        result = 31 * result + bottom;
        return result;
    }
}
