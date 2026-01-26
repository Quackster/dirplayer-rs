package com.dirplayer.player.score;

import com.dirplayer.player.Sprite;

/**
 * A sprite channel in the score.
 * Each channel holds one sprite and has associated properties.
 * Port of Rust SpriteChannel struct.
 */
public class SpriteChannel {
    public int number;
    public String name;
    public boolean scripted;
    public Sprite sprite;

    public SpriteChannel(int number) {
        this.number = number;
        this.name = "";
        this.scripted = false;
        this.sprite = new Sprite(number);
    }

    public SpriteChannel(int number, String name) {
        this.number = number;
        this.name = name != null ? name : "";
        this.scripted = false;
        this.sprite = new Sprite(number);
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public boolean isScripted() {
        return scripted;
    }

    public void setScripted(boolean scripted) {
        this.scripted = scripted;
    }

    public Sprite getSprite() {
        return sprite;
    }

    @Override
    public String toString() {
        return "SpriteChannel{" +
            "number=" + number +
            ", name='" + name + '\'' +
            ", scripted=" + scripted +
            '}';
    }
}
