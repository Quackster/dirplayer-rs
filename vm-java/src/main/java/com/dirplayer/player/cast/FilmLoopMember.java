package com.dirplayer.player.cast;

import com.dirplayer.director.FilmLoopInfo;
import com.dirplayer.director.chunks.ScoreChunk;
import com.dirplayer.player.score.Score;
import com.dirplayer.rendering.IntRect;

/**
 * Film loop cast member data.
 * Port of Rust FilmLoopMember struct.
 */
public class FilmLoopMember {
    public FilmLoopInfo info;
    public ScoreChunk scoreChunk;
    public Score score;
    public int currentFrame;
    public IntRect initialRect;

    public FilmLoopMember() {
        this.info = new FilmLoopInfo();
        this.scoreChunk = null;
        this.score = new Score();
        this.currentFrame = 1;
        this.initialRect = new IntRect(0, 0, 0, 0);
    }

    public FilmLoopInfo getInfo() {
        return info;
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

    public IntRect getInitialRect() {
        return initialRect;
    }

    /**
     * Calculate the total frames in the film loop.
     */
    public int getTotalFrames() {
        return score.totalFrames;
    }

    /**
     * Advance the frame, looping if necessary.
     */
    public void advanceFrame() {
        currentFrame++;
        if (currentFrame > getTotalFrames()) {
            currentFrame = 1;
        }
    }

    /**
     * Reset to the first frame.
     */
    public void reset() {
        currentFrame = 1;
    }

    public FilmLoopMember copy() {
        FilmLoopMember copy = new FilmLoopMember();
        copy.info = info;
        copy.scoreChunk = scoreChunk;
        copy.score = score;  // Note: score is shared, not deep copied
        copy.currentFrame = currentFrame;
        copy.initialRect = initialRect.copy();
        return copy;
    }
}
