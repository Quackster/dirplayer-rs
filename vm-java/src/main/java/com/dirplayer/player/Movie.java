package com.dirplayer.player;

import com.dirplayer.player.score.Score;

/**
 * Movie state representing a loaded Director movie.
 * Port of Rust Movie struct.
 */
public class Movie {
    public IntRect rect;
    public CastManager castManager;
    public Score score;
    public int currentFrame;
    public int puppetTempo;
    public boolean exitLock;
    public int dirVersion;
    public char itemDelimiter;
    public Integer alertHook;
    public String basePath;
    public String fileName;
    public int stageColorR;
    public int stageColorG;
    public int stageColorB;
    public ColorRef stageColorRef;
    public int frameRate;
    public DirectorFile file;
    public boolean updateLock;
    public Integer mouseDownScript;
    public Integer mouseUpScript;
    public boolean allowCustomCaching;
    public boolean traceScript;
    public String traceLogFile;
    public boolean mouseDown;
    public int clickLocX;
    public int clickLocY;
    public Integer frameScriptInstance;
    public CastMemberRef frameScriptMember;
    public boolean centerStage;

    public Movie() {
        this.rect = new IntRect(0, 0, 0, 0);
        this.castManager = new CastManager();
        this.score = new Score();
        this.currentFrame = 1;
        this.puppetTempo = 30;
        this.exitLock = false;
        this.dirVersion = 0;
        this.itemDelimiter = ',';
        this.alertHook = null;
        this.basePath = "";
        this.fileName = "";
        this.stageColorR = 255;
        this.stageColorG = 255;
        this.stageColorB = 255;
        this.stageColorRef = ColorRef.fromPaletteIndex(255);
        this.frameRate = 30;
        this.file = null;
        this.updateLock = false;
        this.mouseDownScript = null;
        this.mouseUpScript = null;
        this.allowCustomCaching = false;
        this.traceScript = false;
        this.traceLogFile = "";
        this.mouseDown = false;
        this.clickLocX = 0;
        this.clickLocY = 0;
        this.frameScriptInstance = null;
        this.frameScriptMember = null;
        this.centerStage = false;
    }

    // Getter methods for rendering compatibility
    public IntRect getRect() {
        return rect;
    }

    public CastManager getCastManager() {
        return castManager;
    }

    public Score getScore() {
        return score;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public void setCurrentFrame(int frame) {
        this.currentFrame = frame;
    }

    public int getTotalFrames() {
        return score.totalFrames;
    }

    public int getDirVersion() {
        return dirVersion;
    }

    /**
     * Get the effective tempo for the current frame.
     * Uses puppetTempo if set, otherwise frame rate.
     * @return The effective tempo (fps)
     */
    public int getEffectiveTempo() {
        if (puppetTempo > 0) {
            return puppetTempo;
        }
        return frameRate;
    }

    /**
     * Get a movie property by name.
     * @param propName The property name
     * @return The property value as a Datum
     * @throws ScriptError if property is unknown
     */
    public com.dirplayer.director.lingo.Datum getProperty(String propName) throws ScriptError {
        switch (propName) {
            case "frame":
            case "currentFrame":
                return com.dirplayer.director.lingo.Datum.ofInt(currentFrame);
            case "frameRate":
            case "tempo":
                return com.dirplayer.director.lingo.Datum.ofInt(frameRate);
            case "puppetTempo":
                return com.dirplayer.director.lingo.Datum.ofInt(puppetTempo);
            case "exitLock":
                return com.dirplayer.director.lingo.Datum.ofBool(exitLock);
            case "itemDelimiter":
                return com.dirplayer.director.lingo.Datum.ofString(String.valueOf(itemDelimiter));
            case "name":
            case "fileName":
                return com.dirplayer.director.lingo.Datum.ofString(fileName);
            case "path":
                return com.dirplayer.director.lingo.Datum.ofString(basePath);
            case "stageLeft":
                return com.dirplayer.director.lingo.Datum.ofInt(rect.left);
            case "stageTop":
                return com.dirplayer.director.lingo.Datum.ofInt(rect.top);
            case "stageRight":
                return com.dirplayer.director.lingo.Datum.ofInt(rect.right);
            case "stageBottom":
                return com.dirplayer.director.lingo.Datum.ofInt(rect.bottom);
            case "stageColor":
                return com.dirplayer.director.lingo.Datum.ofInt(stageColorR);
            case "updateLock":
                return com.dirplayer.director.lingo.Datum.ofBool(updateLock);
            case "traceScript":
                return com.dirplayer.director.lingo.Datum.ofBool(traceScript);
            case "traceLogFile":
                return com.dirplayer.director.lingo.Datum.ofString(traceLogFile);
            case "lastFrame":
                return com.dirplayer.director.lingo.Datum.ofInt(score.totalFrames);
            case "centerStage":
                return com.dirplayer.director.lingo.Datum.ofBool(centerStage);
            default:
                throw new ScriptError("Unknown movie property: " + propName);
        }
    }

    /**
     * Set a movie property by name.
     * @param propName The property name
     * @param value The value to set
     * @throws ScriptError if property is unknown or read-only
     */
    public void setProperty(String propName, com.dirplayer.director.lingo.Datum value) throws ScriptError {
        switch (propName) {
            case "frameRate":
            case "tempo":
                frameRate = value.intValue();
                break;
            case "puppetTempo":
                puppetTempo = value.intValue();
                break;
            case "exitLock":
                exitLock = value.toBool();
                break;
            case "itemDelimiter":
                itemDelimiter = value.stringValue().charAt(0);
                break;
            case "updateLock":
                updateLock = value.toBool();
                break;
            case "traceScript":
                traceScript = value.toBool();
                break;
            case "traceLogFile":
                traceLogFile = value.stringValue();
                break;
            case "centerStage":
                centerStage = value.toBool();
                break;
            default:
                throw new ScriptError("Cannot set movie property: " + propName);
        }
    }
}
