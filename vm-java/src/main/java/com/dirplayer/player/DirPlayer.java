package com.dirplayer.player;

import com.dirplayer.player.bitmap.BitmapManager;
import com.dirplayer.player.score.Score;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Main DirPlayer class - the Shockwave/Director player emulator.
 * Port of Rust DirPlayer struct.
 */
public class DirPlayer {
    private static final Logger logger = LoggerFactory.getLogger(DirPlayer.class);

    public static final int MAX_STACK_SIZE = 50;

    // Player state
    public Movie movie;
    public boolean isPlaying;
    public boolean isScriptPaused;
    public Integer nextFrame;

    // Globals and scopes
    public Map<String, Integer> globals;  // String -> DatumRef ID
    public List<Scope> scopes;

    // Managers
    public NetManager netManager;
    public BitmapManager bitmapManager;
    public TimeoutManager timeoutManager;
    public FontManager fontManager;
    public KeyboardManager keyboardManager;
    public SoundManager soundManager;
    public DatumAllocator allocator;
    public BreakpointManager breakpointManager;

    // Debug state
    public BreakpointContext currentBreakpoint;
    public StepMode stepMode;
    public int stepScopeDepth;
    public boolean breakOnError;

    // Stage properties
    public int stageWidth;
    public int stageHeight;
    public CursorRef cursor;
    public String title;
    public ColorRef bgColor;

    // Input state
    public int mouseLocX;
    public int mouseLocY;
    public long lastMouseDownTime;
    public boolean isDoubleClick;
    public int mouseDownSprite;
    public int clickOnSprite;
    public int keyboardFocusSprite;
    public int textSelectionStart;
    public int textSelectionEnd;

    // UI state
    public int hoveredSprite;
    public List<CastMemberRef> subscribedMemberRefs;
    public boolean isSubscribedToChannelNames;

    // Runtime state
    public LocalDateTime startTime;
    public LocalDateTime systemStartTime;
    public int floatPrecision;
    public int lastHandlerResult;
    public int scopeCount;
    public Map<String, String> externalParams;
    public int handlerStackDepth;

    // Frame state
    public boolean isInFrameUpdate;
    public boolean isDispatchingEvents;
    public boolean isInSendAllSprites;
    public boolean inFrameScript;
    public boolean inEnterFrame;
    public boolean inPrepareFrame;
    public boolean inEventDispatch;
    public int currentFrameTempo;
    public boolean hasPlayerFrameChanged;
    public boolean hasFrameChangedInGo;
    public int goDirection;
    public boolean isGettingPropertyDescriptions;
    public boolean isInitializingBehaviorProps;
    public Integer lastInitializedFrame;

    // Storage
    public Map<Integer, XmlDocument> xmlDocuments;
    public Map<Integer, XmlNode> xmlNodes;
    public int nextXmlId;
    public Map<Integer, DateObject> dateObjects;
    public Map<Integer, MathObject> mathObjects;
    public Map<String, DirectorFile> dirCache;

    // Score context
    public ScoreRef currentScoreContext;

    // Command queue
    public ConcurrentLinkedQueue<PlayerVMCommand> commandQueue;

    // Stream handling
    public boolean enableStreamStatusHandler;

    public DirPlayer() {
        LocalDateTime now = LocalDateTime.now();

        this.movie = new Movie();
        this.isPlaying = false;
        this.isScriptPaused = false;
        this.nextFrame = null;

        this.globals = new HashMap<>();
        this.scopes = new ArrayList<>(MAX_STACK_SIZE);

        this.netManager = new NetManager();
        this.bitmapManager = new BitmapManager();
        this.timeoutManager = new TimeoutManager();
        this.fontManager = new FontManager();
        this.keyboardManager = new KeyboardManager();
        this.soundManager = new SoundManager(8);  // 8 sound channels
        this.allocator = new DatumAllocator();
        this.breakpointManager = new BreakpointManager();

        this.currentBreakpoint = null;
        this.stepMode = StepMode.None;
        this.stepScopeDepth = 0;
        this.breakOnError = true;

        this.stageWidth = 100;
        this.stageHeight = 100;
        this.cursor = new CursorRef();
        this.title = "";
        this.bgColor = new ColorRef(0, 0, 0);

        this.mouseLocX = 0;
        this.mouseLocY = 0;
        this.lastMouseDownTime = 0;
        this.isDoubleClick = false;
        this.mouseDownSprite = 0;
        this.clickOnSprite = 0;
        this.keyboardFocusSprite = -1;
        this.textSelectionStart = 0;
        this.textSelectionEnd = 0;

        this.hoveredSprite = 0;
        this.subscribedMemberRefs = new ArrayList<>();
        this.isSubscribedToChannelNames = false;

        this.startTime = now;
        this.systemStartTime = now;
        this.floatPrecision = 4;
        this.lastHandlerResult = 0;  // DatumRef.Void
        this.scopeCount = 0;
        this.externalParams = new HashMap<>();
        this.handlerStackDepth = 0;

        this.isInFrameUpdate = false;
        this.isDispatchingEvents = false;
        this.isInSendAllSprites = false;
        this.inFrameScript = false;
        this.inEnterFrame = false;
        this.inPrepareFrame = false;
        this.inEventDispatch = false;
        this.currentFrameTempo = 30;
        this.hasPlayerFrameChanged = false;
        this.hasFrameChangedInGo = false;
        this.goDirection = 0;
        this.isGettingPropertyDescriptions = false;
        this.isInitializingBehaviorProps = false;
        this.lastInitializedFrame = null;

        this.xmlDocuments = new HashMap<>();
        this.xmlNodes = new HashMap<>();
        this.nextXmlId = 1;
        this.dateObjects = new HashMap<>();
        this.mathObjects = new HashMap<>();
        this.dirCache = new HashMap<>();

        this.currentScoreContext = new ScoreRef();
        this.commandQueue = new ConcurrentLinkedQueue<>();
        this.enableStreamStatusHandler = false;
    }

    public void play() {
        isPlaying = true;
        isScriptPaused = false;
        logger.info("Player started");
    }

    public void stop() {
        isPlaying = false;
        logger.info("Player stopped");
    }

    public void reset() {
        isPlaying = false;
        isScriptPaused = false;
        nextFrame = null;
        scopes.clear();
        globals.clear();
        currentBreakpoint = null;
        stepMode = StepMode.None;
        logger.info("Player reset");
    }

    public void resumeBreakpoint() {
        if (currentBreakpoint != null) {
            currentBreakpoint = null;
            stepMode = StepMode.None;
            logger.info("Resumed from breakpoint");
        }
    }

    public void stepInto() {
        if (currentBreakpoint != null) {
            stepMode = StepMode.StepInto;
            currentBreakpoint = null;
        }
    }

    public void stepOver() {
        if (currentBreakpoint != null) {
            stepMode = StepMode.StepOver;
            stepScopeDepth = scopes.size();
            currentBreakpoint = null;
        }
    }

    public void stepOut() {
        if (currentBreakpoint != null) {
            stepMode = StepMode.StepOut;
            stepScopeDepth = scopes.size();
            currentBreakpoint = null;
        }
    }

    public void stepOverLine(List<Integer> skipBytecodeIndices) {
        if (currentBreakpoint != null) {
            stepMode = StepMode.StepOverLine;
            stepScopeDepth = scopes.size();
            // TODO: Store skip indices
            currentBreakpoint = null;
        }
    }

    public void stepIntoLine(List<Integer> skipBytecodeIndices) {
        if (currentBreakpoint != null) {
            stepMode = StepMode.StepIntoLine;
            // TODO: Store skip indices
            currentBreakpoint = null;
        }
    }

    public void setStageSize(int width, int height) {
        this.stageWidth = width;
        this.stageHeight = height;
        movie.rect = new IntRect(0, 0, width, height);
    }

    public int getElapsedTicks() {
        long nanos = java.time.temporal.ChronoUnit.NANOS.between(startTime, LocalDateTime.now());
        long tickDurationNanos = 1_000_000_000L / 60;  // 60 ticks per second
        return (int) (nanos / tickDurationNanos);
    }

    public void mouseDown(int x, int y) {
        mouseLocX = x;
        mouseLocY = y;
        movie.mouseDown = true;
        movie.clickLocX = x;
        movie.clickLocY = y;
        // TODO: Dispatch mouseDown event
    }

    public void mouseUp(int x, int y) {
        mouseLocX = x;
        mouseLocY = y;
        movie.mouseDown = false;
        // TODO: Dispatch mouseUp event
    }

    public void mouseMove(int x, int y) {
        mouseLocX = x;
        mouseLocY = y;
        // TODO: Update hovered sprite
    }

    public void keyDown(String key, int code) {
        keyboardManager.keyDown(key, code);
        // TODO: Dispatch keyDown event
    }

    public void keyUp(String key, int code) {
        keyboardManager.keyUp(key, code);
        // TODO: Dispatch keyUp event
    }

    // Getter methods for rendering/JsApi compatibility
    public Movie getMovie() {
        return movie;
    }

    public BitmapManager getBitmapManager() {
        return bitmapManager;
    }

    public ColorRef getBgColor() {
        return bgColor;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    // JsApi methods
    public void loadMovie(byte[] data, String basePath) {
        // TODO: Implement movie loading
        logger.info("Loading movie from {} bytes, basePath: {}", data.length, basePath);
        movie.basePath = basePath;
    }

    public void step() {
        // Step one frame
        if (movie.currentFrame < movie.score.totalFrames) {
            movie.currentFrame++;
        }
    }

    public void goToFrame(int frame) {
        if (frame >= 1 && frame <= movie.score.totalFrames) {
            movie.currentFrame = frame;
        }
    }

    public void handleMouseMove(int x, int y) {
        mouseMove(x, y);
    }

    public void handleMouseDown(int x, int y, int button) {
        mouseDown(x, y);
    }

    public void handleMouseUp(int x, int y, int button) {
        mouseUp(x, y);
    }

    public void handleKeyDown(int keyCode, boolean shift, boolean ctrl, boolean alt) {
        keyboardManager.setShiftDown(shift);
        keyboardManager.setControlDown(ctrl);
        keyboardManager.setAltDown(alt);
        keyDown(String.valueOf((char) keyCode), keyCode);
    }

    public void handleKeyUp(int keyCode) {
        keyUp(String.valueOf((char) keyCode), keyCode);
    }

    public int getSpriteAt(int x, int y) {
        // Find topmost sprite at position
        for (int i = movie.score.channels.size() - 1; i >= 0; i--) {
            Sprite sprite = movie.score.getSprite(i);
            if (sprite != null && sprite.visible && sprite.containsPoint(x, y)) {
                return i;
            }
        }
        return 0;
    }

    // Placeholder inner classes - these would be fully implemented
    public static class Scope {
        public int scriptInstanceRef;
        public int handlerIndex;
        public int bytecodeIndex;
        public List<Integer> stack;
        public Map<Integer, Integer> locals;

        public Scope() {
            this.stack = new ArrayList<>();
            this.locals = new HashMap<>();
        }
    }

    public static class ScoreRef {
        public int castLib;
        public int castMember;
        public boolean isMainScore;

        public ScoreRef() {
            this.isMainScore = true;
        }
    }
}
