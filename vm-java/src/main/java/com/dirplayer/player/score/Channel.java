package com.dirplayer.player.score;

import com.dirplayer.player.Sprite;

/**
 * A score channel containing a sprite.
 * Port of Rust Channel struct.
 */
public class Channel {
    public int number;
    public Sprite sprite;
    public boolean enabled;
    public boolean visible;

    public Channel(int number) {
        this.number = number;
        this.sprite = new Sprite(number);
        this.enabled = true;
        this.visible = true;
    }

    public Sprite getSprite() {
        return sprite;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    public int getNumber() {
        return number;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void reset() {
        sprite.reset();
    }
}
