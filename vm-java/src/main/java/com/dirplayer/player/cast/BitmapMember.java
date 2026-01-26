package com.dirplayer.player.cast;

import com.dirplayer.director.BitmapInfo;
import com.dirplayer.player.CastMemberRef;

/**
 * Bitmap cast member data.
 * Port of Rust BitmapMember struct.
 */
public class BitmapMember {
    public int imageRef;
    public int regPointX;
    public int regPointY;
    public int scriptId;
    public CastMemberRef memberScriptRef;
    public BitmapInfo info;

    public BitmapMember() {
        this.imageRef = 0;
        this.regPointX = 0;
        this.regPointY = 0;
        this.scriptId = 0;
        this.memberScriptRef = null;
        this.info = new BitmapInfo();
    }

    public int getImageRef() {
        return imageRef;
    }

    public int getRegPointX() {
        return regPointX;
    }

    public int getRegPointY() {
        return regPointY;
    }

    public Point getRegPoint() {
        return new Point(regPointX, regPointY);
    }

    public BitmapInfo getInfo() {
        return info;
    }

    public BitmapMember copy() {
        BitmapMember copy = new BitmapMember();
        copy.imageRef = imageRef;
        copy.regPointX = regPointX;
        copy.regPointY = regPointY;
        copy.scriptId = scriptId;
        copy.memberScriptRef = memberScriptRef;
        copy.info = info;  // Info is typically immutable
        return copy;
    }

    /**
     * Simple point class for registration point.
     */
    public static class Point {
        public int x;
        public int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
