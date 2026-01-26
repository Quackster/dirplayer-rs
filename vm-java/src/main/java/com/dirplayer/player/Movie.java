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
    }
}
