package com.dirplayer.player.score;

/**
 * Combined keyframes data for a sprite channel.
 * Groups all tween types (path, size, rotation, etc.) for a single channel.
 * Port of Rust ChannelKeyframes struct.
 */
public class ChannelKeyframes {
    public int channel;
    public SpriteBlendKeyframes blend;
    public SpriteRotationKeyframes rotation;
    public SpriteSkewKeyframes skew;
    public SpritePathKeyframes path;
    public SpriteSizeKeyframes size;
    public SpriteForeColorKeyframes foreColor;
    public SpriteBackColorKeyframes backColor;

    public ChannelKeyframes(int channel) {
        this.channel = channel;
        this.blend = null;
        this.rotation = null;
        this.skew = null;
        this.path = null;
        this.size = null;
        this.foreColor = null;
        this.backColor = null;
    }

    /**
     * Check if this channel has any keyframes.
     */
    public boolean hasKeyframes() {
        return (blend != null && !blend.keyframes.isEmpty()) ||
               (rotation != null && !rotation.keyframes.isEmpty()) ||
               (skew != null && !skew.keyframes.isEmpty()) ||
               (path != null && !path.keyframes.isEmpty()) ||
               (size != null && !size.keyframes.isEmpty()) ||
               (foreColor != null && !foreColor.keyframes.isEmpty()) ||
               (backColor != null && !backColor.keyframes.isEmpty());
    }

    /**
     * Check if path tweening is active for this channel.
     */
    public boolean hasPathTween() {
        return path != null && path.tweenInfo != null && path.tweenInfo.isPathTweened();
    }

    /**
     * Check if size tweening is active for this channel.
     */
    public boolean hasSizeTween() {
        return size != null && size.tweenInfo != null && size.tweenInfo.isSizeTweened();
    }

    /**
     * Check if rotation tweening is active for this channel.
     */
    public boolean hasRotationTween() {
        return rotation != null && rotation.tweenInfo != null && rotation.tweenInfo.isRotationTweened();
    }

    /**
     * Check if blend tweening is active for this channel.
     */
    public boolean hasBlendTween() {
        return blend != null && blend.tweenInfo != null && blend.tweenInfo.isBlendTweened();
    }

    /**
     * Check if skew tweening is active for this channel.
     */
    public boolean hasSkewTween() {
        return skew != null && skew.tweenInfo != null && skew.tweenInfo.isSkewTweened();
    }

    /**
     * Check if foreground color tweening is active for this channel.
     */
    public boolean hasForeColorTween() {
        return foreColor != null && foreColor.tweenInfo != null && foreColor.tweenInfo.isForecolorTweened();
    }

    /**
     * Check if background color tweening is active for this channel.
     */
    public boolean hasBackColorTween() {
        return backColor != null && backColor.tweenInfo != null && backColor.tweenInfo.isBackcolorTweened();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChannelKeyframes{channel=").append(channel);
        if (blend != null && !blend.keyframes.isEmpty()) {
            sb.append(", blend=").append(blend.keyframes.size());
        }
        if (rotation != null && !rotation.keyframes.isEmpty()) {
            sb.append(", rotation=").append(rotation.keyframes.size());
        }
        if (skew != null && !skew.keyframes.isEmpty()) {
            sb.append(", skew=").append(skew.keyframes.size());
        }
        if (path != null && !path.keyframes.isEmpty()) {
            sb.append(", path=").append(path.keyframes.size());
        }
        if (size != null && !size.keyframes.isEmpty()) {
            sb.append(", size=").append(size.keyframes.size());
        }
        if (foreColor != null && !foreColor.keyframes.isEmpty()) {
            sb.append(", foreColor=").append(foreColor.keyframes.size());
        }
        if (backColor != null && !backColor.keyframes.isEmpty()) {
            sb.append(", backColor=").append(backColor.keyframes.size());
        }
        sb.append('}');
        return sb.toString();
    }
}
