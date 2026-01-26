package com.dirplayer.player.cast;

import com.dirplayer.director.ShapeInfo;

/**
 * Shape cast member data.
 * Port of Rust ShapeMember struct.
 */
public class ShapeMember {
    public ShapeInfo shapeInfo;

    public ShapeMember() {
        this.shapeInfo = new ShapeInfo();
    }

    public ShapeMember(ShapeInfo info) {
        this.shapeInfo = info;
    }

    public ShapeInfo getInfo() {
        return shapeInfo;
    }

    public ShapeMember copy() {
        return new ShapeMember(shapeInfo);
    }
}
