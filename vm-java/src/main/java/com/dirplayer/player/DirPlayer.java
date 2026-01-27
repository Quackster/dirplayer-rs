package com.dirplayer.player;

import com.dirplayer.director.DirectorFile;
import com.dirplayer.player.bitmap.BitmapManager;
import com.dirplayer.player.script.Script;
import com.dirplayer.player.score.Score;
import com.dirplayer.player.xml.XmlNode;
import com.dirplayer.SimpleLogger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;

/**
 * Main DirPlayer class - the Shockwave/Director player emulator.
 * Port of Rust DirPlayer struct.
 */
public class DirPlayer {
    private static final SimpleLogger logger = SimpleLogger.getLogger(DirPlayer.class);

    public static final int MAX_STACK_SIZE = 50;

    // Player state
    public Movie movie;
    public boolean isPlaying;
    public boolean isScriptPaused;
    public Integer nextFrame;

    // Globals and scopes
    public Map<String, Integer> globals;  // String -> DatumRef ID
    public List<ScriptScope> scopes;

    // Managers
    public NetManager netManager;
    public BitmapManager bitmapManager;
    public TimeoutManager timeoutManager;
    public FontManager fontManager;
    public KeyboardManager keyboardManager;
    public SoundManager soundManager;
    public DatumAllocator allocator;
    public BreakpointManager breakpointManager;
    public com.dirplayer.player.xtra.XtraManager xtraManager;
    public com.dirplayer.player.events.EventDispatcher eventDispatcher;

    // Debug state
    public BreakpointContext currentBreakpoint;
    public StepMode stepMode;
    public int stepScopeDepth;
    public List<Integer> skipBytecodeIndices;
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
    public LocalDateTime systemStartTimeLocal;
    public long systemStartTime;  // Milliseconds since epoch
    public int floatPrecision;
    public int lastHandlerResult;
    public int scopeCount;
    public Map<String, String> externalParams;
    public org.teavm.jso.JSObject rawExternalParams;
    public java.util.Set<CastMemberRef> subscribedMembers;
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
    public int currentCursor;  // Current cursor type

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
    public LinkedList<PlayerVMCommand> commandQueue;

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
        this.xtraManager = new com.dirplayer.player.xtra.XtraManager();
        this.eventDispatcher = new com.dirplayer.player.events.EventDispatcher();

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
        this.systemStartTimeLocal = now;
        this.systemStartTime = System.currentTimeMillis();
        this.floatPrecision = 4;
        this.lastHandlerResult = 0;  // DatumRef.Void
        this.scopeCount = 0;
        this.externalParams = new HashMap<>();
        this.rawExternalParams = null;
        this.subscribedMembers = new java.util.HashSet<>();
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
        this.commandQueue = new LinkedList<>();
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
        scopeCount = 0;
        globals.clear();
        currentBreakpoint = null;
        stepMode = StepMode.None;
        hasPlayerFrameChanged = false;
        hasFrameChangedInGo = false;
        goDirection = 0;
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
            this.skipBytecodeIndices = skipBytecodeIndices != null ? new java.util.ArrayList<>(skipBytecodeIndices) : new java.util.ArrayList<>();
            currentBreakpoint = null;
        }
    }

    public void stepIntoLine(List<Integer> skipBytecodeIndices) {
        if (currentBreakpoint != null) {
            stepMode = StepMode.StepIntoLine;
            this.skipBytecodeIndices = skipBytecodeIndices != null ? new java.util.ArrayList<>(skipBytecodeIndices) : new java.util.ArrayList<>();
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
        clickOnSprite = getSpriteAt(x, y);

        // Dispatch mouseDown event to sprites and scripts
        eventDispatcher.dispatchGlobalEvent("mouseDown", new java.util.ArrayList<>());
    }

    public void mouseUp(int x, int y) {
        mouseLocX = x;
        mouseLocY = y;
        movie.mouseDown = false;

        // Dispatch mouseUp event to sprites and scripts
        eventDispatcher.dispatchGlobalEvent("mouseUp", new java.util.ArrayList<>());
    }

    public void mouseMove(int x, int y) {
        int prevHoveredSprite = hoveredSprite;
        mouseLocX = x;
        mouseLocY = y;

        // Update hovered sprite
        hoveredSprite = getSpriteAt(x, y);

        // Dispatch rollover events if sprite changed
        if (hoveredSprite != prevHoveredSprite) {
            // Could dispatch mouseEnter/mouseLeave events here
        }
    }

    public void keyDown(String key, int code) {
        keyboardManager.keyDown(key, code);

        // Dispatch keyDown event to sprites and scripts
        eventDispatcher.dispatchGlobalEvent("keyDown", new java.util.ArrayList<>());
    }

    public void keyUp(String key, int code) {
        keyboardManager.keyUp(key, code);

        // Dispatch keyUp event to sprites and scripts
        eventDispatcher.dispatchGlobalEvent("keyUp", new java.util.ArrayList<>());
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
        // Movie loading is handled by JsApi.loadContent() which calls loadFromFile()
        // This method is called after the file is already parsed and loaded.
        logger.info("loadMovie called with {} bytes, basePath: {}", data.length, basePath);
        movie.basePath = basePath;

        // Parse the movie file from data
        if (data != null && data.length > 0) {
            try {
                String fileName = basePath.contains("/") ?
                    basePath.substring(basePath.lastIndexOf('/') + 1) : basePath;
                com.dirplayer.director.DirectorFile dirFile =
                    com.dirplayer.director.DirectorFile.readBytes(data, fileName, basePath);
                loadFromDirectorFile(dirFile);
            } catch (Exception e) {
                logger.error("Failed to load movie: {}", e.getMessage());
            }
        }
    }

    /**
     * Load movie content from a DirectorFile.
     */
    public void loadFromDirectorFile(com.dirplayer.director.DirectorFile dirFile) {
        try {
            if (dirFile == null) {
                return;
            }

            // Store movie properties from config chunk first
            if (dirFile.config != null) {
                var config = dirFile.config;
                movie.rect = new IntRect(config.movieLeft, config.movieTop,
                    config.movieRight, config.movieBottom);
                movie.dirVersion = config.directorVersion;
                movie.frameRate = config.frameRate;
                // Use stage color from config
                if (config.d7StageColorIsRgb != 0) {
                    movie.stageColorR = config.d7StageColorR;
                    movie.stageColorG = config.d7StageColorG;
                    movie.stageColorB = config.d7StageColorB;
                } else {
                    movie.stageColorRef = ColorRef.fromPaletteIndex(config.preD7StageColor);
                }
            }

            // Load cast members from the parsed director file
            movie.castManager.loadFromDir(dirFile, bitmapManager);

            // Load score from the parsed director file
            if (dirFile.score != null) {
                movie.score.loadFromScoreChunk(dirFile.score);

                // Load frame labels if available
                if (dirFile.frameLabels != null) {
                    movie.score.frameLabels = dirFile.frameLabels.labels;
                }
            }

            // Store the file reference
            movie.basePath = dirFile.basePath != null ? dirFile.basePath.toString() : "";
            movie.fileName = dirFile.fileName;

            // Load fonts from cast members
            movie.castManager.loadFontsIntoManager(fontManager);

            reset();
            logger.info("Loaded movie: {}x{}, {} casts, {} frames",
                movie.rect.width(), movie.rect.height(),
                movie.castManager.getCastLibCount(),
                movie.score.totalFrames);
        } catch (Exception e) {
            logger.error("Failed to load from director file: {}", e.getMessage());
        }
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
            Sprite sprite = movie.score.getSprite((short) i);
            if (sprite != null && sprite.visible && sprite.containsPoint(x, y)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Get sprite by channel number.
     */
    public Sprite getSprite(int channelNum) {
        return movie.score.getSprite((short) channelNum);
    }

    /**
     * Get bitmap for a cast member.
     * @param memberRef The cast member reference
     * @return The member's bitmap or null if not a bitmap member
     */
    public com.dirplayer.player.bitmap.Bitmap getMemberBitmap(CastMemberRef memberRef) {
        CastMember member = movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            return null;
        }
        // Get the bitmap from member's bitmap reference
        if (member.bitmap != null && member.bitmap.bitmapId > 0) {
            return bitmapManager.getBitmap(member.bitmap.bitmapId);
        }
        return null;
    }

    // Datum allocation and access methods
    public com.dirplayer.director.lingo.Datum getDatum(int ref) {
        return allocator.get(ref);
    }

    /**
     * Get a mutable reference to a datum.
     * In Java, this is the same as getDatum since objects are already mutable.
     */
    public com.dirplayer.director.lingo.Datum getDatumMut(int ref) {
        return allocator.get(ref);
    }

    public int allocDatum(com.dirplayer.director.lingo.Datum datum) {
        return allocator.alloc(datum);
    }

    /**
     * Get a script instance by ID.
     */
    public com.dirplayer.player.script.ScriptInstance getScriptInstance(int id) {
        return allocator.getScriptInstance(id);
    }

    /**
     * Create a new script instance from a script member reference.
     */
    public int createScriptInstance(CastMemberRef scriptRef, java.util.List<Integer> constructorArgs) throws ScriptError {
        // Find the script
        CastMember member = movie.castManager.findMemberByRef(scriptRef);
        if (member == null) {
            throw new ScriptError("Script member not found: " + scriptRef);
        }

        // Create the instance
        com.dirplayer.player.script.ScriptInstance instance = new com.dirplayer.player.script.ScriptInstance();
        instance.script = scriptRef;
        instance.ancestor = 0;  // 0 = no ancestor

        // Add to allocator
        int instanceId = allocator.allocScriptInstance(instance);

        return instanceId;
    }

    /**
     * Get the current scope reference (index into scopes list).
     */
    public int currentScopeRef() {
        return scopes.isEmpty() ? -1 : scopes.size() - 1;
    }

    /**
     * Initialize default global variables.
     */
    public void initializeGlobals() {
        // Initialize standard Lingo globals
        // These would include things like TRUE, FALSE, PI, etc.
        // For now this is a stub
    }

    public String formatDatum(com.dirplayer.director.lingo.Datum datum) {
        if (datum == null) {
            return "VOID";
        }
        try {
            switch (datum.getType()) {
                case Int:
                    return String.valueOf(datum.intValue());
                case Float:
                    return String.valueOf(datum.floatValue());
                case String:
                    return datum.stringValue();
                case Symbol:
                    return "#" + datum.symbolValue();
                case Void:
                    return "VOID";
                case List:
                    return "[list]";
                case PropList:
                    return "[propList]";
                default:
                    return "<" + datum.typeStr() + ">";
            }
        } catch (ScriptError e) {
            return "<error>";
        }
    }

    public char getItemDelimiter() {
        return movie.itemDelimiter;
    }

    /**
     * Convert datum to string for concatenation operations.
     */
    public String datumToStringForConcat(com.dirplayer.director.lingo.Datum datum) {
        return DatumFormatter.datumToStringForConcat(datum, this);
    }

    public int[] resolvePaletteColor(int index) {
        // Default VGA palette approximation
        if (index < 0 || index > 255) {
            return new int[] { 0, 0, 0 };
        }
        // Standard VGA colors for indices 0-15
        int[][] vgaColors = {
            {0, 0, 0},       // 0: Black
            {0, 0, 170},     // 1: Blue
            {0, 170, 0},     // 2: Green
            {0, 170, 170},   // 3: Cyan
            {170, 0, 0},     // 4: Red
            {170, 0, 170},   // 5: Magenta
            {170, 85, 0},    // 6: Brown
            {170, 170, 170}, // 7: Light Gray
            {85, 85, 85},    // 8: Dark Gray
            {85, 85, 255},   // 9: Light Blue
            {85, 255, 85},   // 10: Light Green
            {85, 255, 255},  // 11: Light Cyan
            {255, 85, 85},   // 12: Light Red
            {255, 85, 255},  // 13: Light Magenta
            {255, 255, 85},  // 14: Yellow
            {255, 255, 255}  // 15: White
        };
        if (index < 16) {
            return vgaColors[index];
        }
        // Grayscale ramp for higher indices
        int gray = (index - 16) * 255 / 239;
        return new int[] { gray, gray, gray };
    }

    // --- Bytecode helper methods ---

    public com.dirplayer.director.chunks.Bytecode getCtxCurrentBytecode(com.dirplayer.player.bytecode.BytecodeHandlerContext ctx) {
        if (ctx.handler != null && ctx.handler.bytecodeArray != null && ctx.bytecodeIndex < ctx.handler.bytecodeArray.size()) {
            return ctx.handler.bytecodeArray.get(ctx.bytecodeIndex);
        }
        return null;
    }

    public String getName(com.dirplayer.player.bytecode.BytecodeHandlerContext ctx, int nameId) {
        if (ctx.scriptContext != null && ctx.scriptContext.names != null && nameId >= 0 && nameId < ctx.scriptContext.names.size()) {
            return ctx.scriptContext.names.get(nameId);
        }
        return "name_" + nameId;
    }

    public com.dirplayer.director.chunks.HandlerDef getCurrentHandlerDef(com.dirplayer.player.bytecode.BytecodeHandlerContext ctx) {
        return ctx.handler;
    }

    public BreakpointManager getBreakpointManager() {
        return breakpointManager;
    }

    public int getMovieProp(String propName) throws ScriptError {
        switch (propName) {
            case "stage":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofSymbol("stage"));
            case "time":
                java.time.LocalTime now = java.time.LocalTime.now();
                String timeStr = String.format("%02d:%02d %s",
                    now.getHour() % 12 == 0 ? 12 : now.getHour() % 12,
                    now.getMinute(),
                    now.getHour() >= 12 ? "PM" : "AM");
                return allocDatum(com.dirplayer.director.lingo.Datum.ofString(timeStr));
            case "milliSeconds":
                long elapsed = System.currentTimeMillis() - systemStartTime;
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt((int) elapsed));
            case "keyboardFocusSprite":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(keyboardFocusSprite));
            case "frameTempo":
                Integer tempo = movie.score.getFrameTempo(movie.currentFrame);
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(tempo != null ? tempo : movie.frameRate));
            case "mouseLoc": {
                int xRef = allocDatum(com.dirplayer.director.lingo.Datum.ofInt(mouseLocX));
                int yRef = allocDatum(com.dirplayer.director.lingo.Datum.ofInt(mouseLocY));
                return allocDatum(com.dirplayer.director.lingo.Datum.ofPoint(xRef, yRef));
            }
            case "mouseH":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(mouseLocX));
            case "mouseV":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(mouseLocY));
            case "rollover": {
                int sprite = getSpriteAt(mouseLocX, mouseLocY);
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(sprite));
            }
            case "keyCode":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(keyboardManager.getLastKeyCode()));
            case "shiftDown":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofBool(keyboardManager.isShiftDown()));
            case "optionDown":
            case "altDown":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofBool(keyboardManager.isAltDown()));
            case "commandDown":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofBool(keyboardManager.isCommandDown()));
            case "controlDown":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofBool(keyboardManager.isControlDown()));
            case "key":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofString(keyboardManager.getLastKey()));
            case "floatPrecision":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(floatPrecision));
            case "doubleClick":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofBool(isDoubleClick));
            case "ticks":
                long ticksElapsed = (System.currentTimeMillis() - systemStartTime) * 60 / 1000;
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt((int) ticksElapsed));
            case "frameLabel": {
                String label = "0";
                for (var frameLabel : movie.score.frameLabels) {
                    if (frameLabel.frameNum <= movie.currentFrame) {
                        label = frameLabel.label;
                    }
                }
                return allocDatum(com.dirplayer.director.lingo.Datum.ofString(label));
            }
            case "currentSpriteNum": {
                ScriptScope scope = scopes.get(currentScopeRef());
                if (scope != null && scope.receiver != null) {
                    Integer propRef = com.dirplayer.player.script.ScriptUtils.scriptGetPropOpt(
                        this, scope.receiver, "spriteNum");
                    if (propRef != null) {
                        com.dirplayer.director.lingo.Datum datum = getDatum(propRef);
                        if (!datum.isVoid()) {
                            return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(datum.intValue()));
                        }
                    }
                }
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(0));
            }
            case "actorList": {
                Integer ref = globals.get("actorList");
                return ref != null ? ref : 0;
            }
            case "clickOn":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(clickOnSprite));
            case "clickLoc": {
                int xRef = allocDatum(com.dirplayer.director.lingo.Datum.ofInt(movie.clickLocX));
                int yRef = allocDatum(com.dirplayer.director.lingo.Datum.ofInt(movie.clickLocY));
                return allocDatum(com.dirplayer.director.lingo.Datum.ofPoint(xRef, yRef));
            }
            case "environment": {
                java.util.List<com.dirplayer.director.lingo.Datum.PropListPair> props = new java.util.ArrayList<>();
                props.add(new com.dirplayer.director.lingo.Datum.PropListPair(allocDatum(com.dirplayer.director.lingo.Datum.ofSymbol("shockMachine")), allocDatum(com.dirplayer.director.lingo.Datum.ofInt(0))));
                props.add(new com.dirplayer.director.lingo.Datum.PropListPair(allocDatum(com.dirplayer.director.lingo.Datum.ofSymbol("platform")), allocDatum(com.dirplayer.director.lingo.Datum.ofString("Windows,32"))));
                props.add(new com.dirplayer.director.lingo.Datum.PropListPair(allocDatum(com.dirplayer.director.lingo.Datum.ofSymbol("runMode")), allocDatum(com.dirplayer.director.lingo.Datum.ofString("Plugin"))));
                props.add(new com.dirplayer.director.lingo.Datum.PropListPair(allocDatum(com.dirplayer.director.lingo.Datum.ofSymbol("colorDepth")), allocDatum(com.dirplayer.director.lingo.Datum.ofInt(32))));
                props.add(new com.dirplayer.director.lingo.Datum.PropListPair(allocDatum(com.dirplayer.director.lingo.Datum.ofSymbol("internetConnected")), allocDatum(com.dirplayer.director.lingo.Datum.ofSymbol("online"))));
                props.add(new com.dirplayer.director.lingo.Datum.PropListPair(allocDatum(com.dirplayer.director.lingo.Datum.ofSymbol("productVersion")), allocDatum(com.dirplayer.director.lingo.Datum.ofString("10.1"))));
                return allocDatum(com.dirplayer.director.lingo.Datum.ofPropList(props, false));
            }
            default:
                // Delegate to movie properties, then check globals
                try {
                    com.dirplayer.director.lingo.Datum datum = movie.getProperty(propName);
                    if (datum != null) {
                        return allocDatum(datum);
                    }
                } catch (ScriptError e) {
                    // Property not found in movie, check globals
                }
                // Check globals as fallback
                if (globals.containsKey(propName)) {
                    return globals.get(propName);
                }
                // Return void for unknown properties (like Director does)
                return allocDatum(com.dirplayer.director.lingo.Datum.ofVoid());
        }
    }

    public void setMovieProp(String propName, com.dirplayer.director.lingo.Datum value) throws ScriptError {
        switch (propName) {
            case "keyboardFocusSprite":
                keyboardFocusSprite = (short) value.intValue();
                break;
            case "selStart":
                textSelectionStart = value.intValue();
                break;
            case "selEnd":
                textSelectionEnd = value.intValue();
                break;
            case "floatPrecision":
                floatPrecision = value.intValue();
                break;
            case "centerStage":
                movie.centerStage = value.toBool();
                break;
            case "actorList":
                if (value.isList()) {
                    int newActorList = allocDatum(value.clone());
                    globals.put("actorList", newActorList);
                } else {
                    throw new ScriptError("actorList must be a list");
                }
                break;
            default:
                // Try to set as movie property, if unknown treat as global
                try {
                    movie.setProperty(propName, value);
                } catch (ScriptError e) {
                    // Store as global variable
                    globals.put(propName, allocDatum(value.clone()));
                }
        }
    }

    public int callDatumHandler(int objRef, String handlerName, java.util.List<Integer> args) throws ScriptError {
        com.dirplayer.director.lingo.Datum datum = getDatum(objRef);
        if (datum == null || datum.isVoid()) {
            throw new ScriptError("Cannot call handler on void");
        }

        // Delegate to type-specific handlers
        com.dirplayer.director.lingo.DatumType type = datum.getType();
        switch (type) {
            case List:
            case ArgList:
            case ArgListNoRet:
                return com.dirplayer.player.handlers.datum.ListHandlers.call(this, objRef, handlerName, args);
            case PropList:
                return com.dirplayer.player.handlers.datum.PropListHandlers.call(this, objRef, handlerName, args);
            case String:
                return com.dirplayer.player.handlers.datum.StringDatumHandlers.call(this, objRef, handlerName, args);
            case Point:
                return com.dirplayer.player.handlers.datum.PointHandlers.call(this, objRef, handlerName, args);
            case Rect:
                return com.dirplayer.player.handlers.datum.RectHandlers.call(this, objRef, handlerName, args);
            case Symbol:
                return com.dirplayer.player.handlers.datum.SymbolHandlers.call(this, objRef, handlerName, args);
            case ColorRef:
                return com.dirplayer.player.handlers.datum.ColorHandlers.call(this, objRef, handlerName, args);
            case CastMember:
            case CastMemberRef:
                return com.dirplayer.player.handlers.datum.CastMemberRefHandlers.call(this, objRef, handlerName, args);
            case SpriteRef:
                return com.dirplayer.player.handlers.datum.SpriteHandlers.call(this, objRef, handlerName, args);
            case ScriptInstanceRef:
                return com.dirplayer.player.handlers.datum.ScriptInstanceHandlers.call(this, objRef, handlerName, args);
            case Int:
                // Int doesn't have a call method - return void
                return 0;
            case Float:
                // Float doesn't have a call method - return void
                return 0;
            case DateRef:
                return com.dirplayer.player.handlers.datum.DateHandlers.call(this, objRef, handlerName, args);
            case SoundChannel:
                return com.dirplayer.player.handlers.datum.SoundChannelHandlers.call(this, objRef, handlerName, args);
            case StringChunk:
                return com.dirplayer.player.handlers.datum.StringChunkHandlers.call(this, objRef, handlerName, args);
            case TimeoutRef:
            case TimeoutInstance:
                return com.dirplayer.player.handlers.datum.TimeoutHandlers.call(this, objRef, handlerName, args);
            case Xtra:
            case XtraInstance:
                return com.dirplayer.player.handlers.datum.XtraHandlers.call(this, objRef, handlerName, args);
            case BitmapRef:
                return com.dirplayer.player.handlers.datum.BitmapHandlers.call(this, objRef, handlerName, args);
            case XmlRef:
                return com.dirplayer.player.handlers.datum.XmlHandlers.call(this, objRef, handlerName, args);
            case Void:
                throw new ScriptError("Cannot call handler on void");
            default:
                throw new ScriptError("Unknown datum type for handler call: " + type);
        }
    }

    public int localCall(com.dirplayer.player.bytecode.BytecodeHandlerContext ctx, int handlerIndex, java.util.List<Integer> args) throws ScriptError {
        // Get current script and handler reference
        com.dirplayer.player.script.Script script = getCurrentScript(ctx);
        if (script == null) {
            throw new ScriptError("No script context for local call");
        }

        // Get handler reference at the given index
        com.dirplayer.player.script.Script.ScriptHandlerRef handlerRef = script.getOwnHandlerRefAt(handlerIndex);
        if (handlerRef == null) {
            throw new ScriptError("Handler not found at index: " + handlerIndex);
        }

        String handlerName = handlerRef.handlerName;
        CastMemberRef scriptRef = handlerRef.memberRef;

        // Check if first arg is a receiver that has this handler
        com.dirplayer.player.script.ScriptInstanceRef receiver = null;
        if (!args.isEmpty()) {
            com.dirplayer.director.lingo.Datum firstArg = getDatum(args.get(0));
            if (firstArg.isScriptInstanceRef()) {
                com.dirplayer.player.script.ScriptInstanceRef argInstanceRef =
                    new com.dirplayer.player.script.ScriptInstanceRef(firstArg.intValue());
                // Check if this instance has the handler
                com.dirplayer.player.script.ScriptInstance instance = allocator.getScriptInstance(argInstanceRef);
                if (instance != null) {
                    com.dirplayer.player.script.Script instanceScript =
                        movie.castManager.getScriptByRef(instance.script);
                    if (instanceScript != null && instanceScript.hasHandler(handlerName)) {
                        receiver = argInstanceRef;
                        scriptRef = instance.script;
                    }
                }
            }
        }

        // If no receiver from args, use scope's receiver
        if (receiver == null) {
            ScriptScope currentScope = scopes.get(ctx.scopeRef);
            receiver = currentScope.receiver;
        }

        // Call the handler
        return callScriptHandler(receiver, scriptRef, handlerName, args);
    }

    /**
     * Call a script handler (returns just the return value for compatibility).
     * @param receiver The receiver script instance (or null)
     * @param scriptRef The script member reference
     * @param handlerName The handler name
     * @param args The arguments
     * @return The result datum reference
     */
    public int callScriptHandler(com.dirplayer.player.script.ScriptInstanceRef receiver,
                                  CastMemberRef scriptRef, String handlerName,
                                  java.util.List<Integer> args) throws ScriptError {
        ScopeResult result = callScriptHandlerWithResult(receiver, scriptRef, handlerName, args);
        return result.returnValue;
    }

    /**
     * Call a script handler and return full result with passed flag.
     * Port of Rust player_call_script_handler.
     * @param receiver The receiver script instance (or null)
     * @param scriptRef The script member reference
     * @param handlerName The handler name
     * @param args The arguments
     * @return The ScopeResult containing return value and passed flag
     */
    public ScopeResult callScriptHandlerWithResult(com.dirplayer.player.script.ScriptInstanceRef receiver,
                                  CastMemberRef scriptRef, String handlerName,
                                  java.util.List<Integer> args) throws ScriptError {
        // Get the script
        com.dirplayer.player.script.Script script = movie.castManager.getScriptByRef(scriptRef);
        if (script == null) {
            throw new ScriptError("Script not found: " + scriptRef);
        }

        // Get the handler
        com.dirplayer.director.chunks.HandlerDef handler = script.getOwnHandler(handlerName);
        if (handler == null) {
            throw new ScriptError("Handler not found: " + handlerName + " in script " + scriptRef);
        }

        // Push a new scope
        int scopeRef = pushScope();
        ScriptScope scope = scopes.get(scopeRef);
        scope.scriptMemberRef = scriptRef;
        scope.receiver = receiver;
        scope.handlerNameId = handler.nameId;

        // Build context for name lookup
        com.dirplayer.player.bytecode.BytecodeHandlerContext ctx =
            new com.dirplayer.player.bytecode.BytecodeHandlerContext(
                scopeRef, 0, 0, scriptRef.castLib, scriptRef.castMember, handler, script.chunk);
        // Get script context from the cast library
        CastLib cast = movie.castManager.getCastOrNull(scriptRef.castLib);
        if (cast != null && cast.scriptContext != null) {
            ctx.scriptContext = cast.scriptContext;
        }

        // Set up arguments as local variables
        for (int i = 0; i < args.size() && i < handler.argumentNameIds.size(); i++) {
            int argNameId = handler.argumentNameIds.get(i);
            String argName = getName(ctx, argNameId);
            scope.locals.put(argName, args.get(i));
        }

        try {
            // Execute bytecode loop
            while (scope.bytecodeIndex < handler.bytecodeArray.size()) {
                com.dirplayer.player.bytecode.HandlerExecutionResult result =
                    com.dirplayer.player.bytecode.BytecodeHandlerManager.executeBytecode(this, ctx);

                switch (result) {
                    case ADVANCE:
                        scope.bytecodeIndex++;
                        break;
                    case JUMP:
                        // bytecodeIndex was already set by the jump instruction
                        break;
                    case STOP:
                        // Capture result before popping scope
                        ScopeResult stopResult = new ScopeResult(scope.returnValue, scope.passed);
                        popScope();
                        return stopResult;
                }
            }
        } catch (ScriptError e) {
            popScope();
            throw e;
        }

        // Capture result before popping scope
        ScopeResult finalResult = new ScopeResult(scope.returnValue, scope.passed);
        popScope();
        return finalResult;
    }

    public void setBasePath(String path) {
        movie.basePath = path;
    }

    public void setSystemFontPath(String path) {
        fontManager.setSystemFontPath(path);
    }

    public void setBreakOnError(boolean value) {
        this.breakOnError = value;
    }

    public void setSubscribedToChannelNames(boolean value) {
        this.isSubscribedToChannelNames = value;
    }

    public void subscribeToMember(int castLib, int memberNum) {
        CastMemberRef ref = new CastMemberRef(castLib, memberNum);
        subscribedMembers.add(ref);
    }

    public void unsubscribeFromMember(int castLib, int memberNum) {
        CastMemberRef ref = new CastMemberRef(castLib, memberNum);
        subscribedMembers.remove(ref);
    }

    public void triggerTimeout(String name) {
        try {
            timeoutManager.triggerTimeout(this, name);
        } catch (ScriptError e) {
            logger.warn("Error triggering timeout '{}': {}", name, e.getMessage());
        }
    }

    public void triggerAlertHook() {
        if (movie.alertHook != null) {
            try {
                // Call the alert hook script instance
                com.dirplayer.director.lingo.Datum hookDatum = getDatum(movie.alertHook);
                if (hookDatum.isScriptInstanceRef()) {
                    com.dirplayer.player.script.ScriptInstanceRef instanceRef =
                        new com.dirplayer.player.script.ScriptInstanceRef(hookDatum.intValue());
                    com.dirplayer.player.script.ScriptInstance instance = allocator.getScriptInstance(instanceRef);
                    if (instance != null) {
                        callScriptHandler(instanceRef, instance.script, "alertHook", new java.util.ArrayList<>());
                    }
                }
            } catch (ScriptError e) {
                logger.warn("Error triggering alert hook: {}", e.getMessage());
            }
        }
    }

    public void setExternalParams(org.teavm.jso.JSObject params) {
        // Store raw params - can be converted to Map<String,String> later if needed
        this.rawExternalParams = params;
    }

    public void loadMovieFromFile(String path, boolean autoplay) {
        logger.info("Loading movie from file: {}", path);
        // In browser environment, this would need to fetch the file via HTTP
        // For now, log the request
        if (autoplay) {
            isPlaying = true;
        }
    }

    public void requestDatumSnapshot(int datumId) {
        // For debugging - return datum value as JSON-like string
        com.dirplayer.director.lingo.Datum datum = getDatum(datumId);
        String snapshot = com.dirplayer.player.DatumFormatter.formatDatum(datumId, this);
        logger.debug("Datum {} snapshot: {}", datumId, snapshot);
    }

    public void requestScriptInstanceSnapshot(int scriptInstanceId) {
        // For debugging - return script instance properties
        com.dirplayer.player.script.ScriptInstanceRef ref = new com.dirplayer.player.script.ScriptInstanceRef(scriptInstanceId);
        com.dirplayer.player.script.ScriptInstance instance = allocator.getScriptInstance(ref);
        if (instance != null) {
            logger.debug("ScriptInstance {} snapshot: script={}, props={}",
                scriptInstanceId, instance.script, instance.properties.size());
        }
    }

    public void provideNetTaskData(int taskId, byte[] data) {
        netManager.provideNetTaskData(taskId, data);
    }

    public void evalLingoCommand(String command) {
        // Parse and evaluate a Lingo command string
        // For now, just log and handle simple commands
        logger.info("Evaluating Lingo command: {}", command);

        // Handle some basic commands
        command = command.trim();
        if (command.isEmpty()) {
            return;
        }

        // Check for go command
        if (command.toLowerCase().startsWith("go ")) {
            String target = command.substring(3).trim();
            try {
                int frame = Integer.parseInt(target);
                goToFrame(frame);
            } catch (NumberFormatException e) {
                // Try to find label
                for (var label : movie.score.frameLabels) {
                    if (label.label.equalsIgnoreCase(target)) {
                        goToFrame(label.frameNum);
                        return;
                    }
                }
            }
        } else if (command.toLowerCase().equals("play")) {
            play();
        } else if (command.toLowerCase().equals("stop")) {
            stop();
        } else {
            logger.warn("Unknown Lingo command: {}", command);
        }
    }

    public String getChannelDisplayName(int channelNum) {
        if (movie == null || movie.score == null) {
            return "";
        }
        // Check if score has channel names
        if (movie.score.channelNames != null && channelNum > 0 && channelNum <= movie.score.channelNames.size()) {
            String name = movie.score.channelNames.get(channelNum - 1);
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        return "Channel " + channelNum;
    }

    public void printMemberBitmapHex(int castLib, int castMember) {
        logger.info("Print member bitmap hex: cast {} member {}", castLib, castMember);
        CastMemberRef memberRef = new CastMemberRef(castLib, castMember);
        CastMember member = movie.castManager.findMemberByRef(memberRef);
        if (member == null) {
            logger.warn("Member not found: {} {}", castLib, castMember);
            return;
        }
        if (member.bitmap != null) {
            com.dirplayer.player.bitmap.Bitmap bitmap = bitmapManager.getBitmap(member.bitmap.bitmapId);
            if (bitmap != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Bitmap: ").append(bitmap.getWidth()).append("x").append(bitmap.getHeight());
                sb.append(" depth=").append(bitmap.getBitDepth());
                sb.append("\nFirst 64 bytes: ");
                int maxBytes = Math.min(64, bitmap.data.length);
                for (int i = 0; i < maxBytes; i++) {
                    sb.append(String.format("%02X ", bitmap.data[i] & 0xFF));
                }
                logger.info(sb.toString());
            } else {
                logger.warn("Bitmap not found in manager: {}", member.bitmap.bitmapId);
            }
        } else {
            logger.warn("Member has no bitmap reference");
        }
    }

    public boolean getBreakOnError() {
        return breakOnError;
    }

    // --- Movie property methods ---

    public com.dirplayer.director.lingo.Datum getAnimProp(int propId) throws ScriptError {
        // Anim props from LingoConstants
        switch (propId) {
            case 0x01: // beepOn
                return com.dirplayer.director.lingo.Datum.ofBool(true);
            case 0x02: // buttonStyle
                return com.dirplayer.director.lingo.Datum.ofInt(0);
            case 0x03: // centerStage
                return com.dirplayer.director.lingo.Datum.ofBool(movie.centerStage);
            case 0x06: // colorDepth
                return com.dirplayer.director.lingo.Datum.ofInt(32);
            case 0x08: // exitLock
                return com.dirplayer.director.lingo.Datum.ofBool(false);
            case 0x0d: // key
                return com.dirplayer.director.lingo.Datum.ofString(keyboardManager.lastKey != null ? keyboardManager.lastKey : "");
            case 0x10: // keyCode
                return com.dirplayer.director.lingo.Datum.ofInt(keyboardManager.lastKeyCode);
            case 0x15: // pauseState
                return com.dirplayer.director.lingo.Datum.ofBool(isScriptPaused);
            case 0x17: // selEnd
                return com.dirplayer.director.lingo.Datum.ofInt(textSelectionEnd);
            case 0x18: // selStart
                return com.dirplayer.director.lingo.Datum.ofInt(textSelectionStart);
            case 0x19: // soundEnabled
                return com.dirplayer.director.lingo.Datum.ofBool(true);
            case 0x22: // timer
                return com.dirplayer.director.lingo.Datum.ofInt(getElapsedTicks());
            default:
                return com.dirplayer.director.lingo.Datum.ofInt(0);
        }
    }

    public com.dirplayer.director.lingo.Datum getAnim2Prop(int propId) throws ScriptError {
        switch (propId) {
            case 0x02: // number of castMembers
                return getCastMemberCount(com.dirplayer.director.lingo.Datum.ofInt(0));
            case 0x03: // number of menus
                return com.dirplayer.director.lingo.Datum.ofInt(0);
            case 0x04: // number of castLibs
                return com.dirplayer.director.lingo.Datum.ofInt(movie.castManager.castLibs.size());
            case 0x05: // number of xtras
                return com.dirplayer.director.lingo.Datum.ofInt(0);
            default:
                return com.dirplayer.director.lingo.Datum.ofInt(0);
        }
    }

    public com.dirplayer.director.lingo.Datum getCastMemberCount(com.dirplayer.director.lingo.Datum castLibId) throws ScriptError {
        int count = 0;
        if (castLibId.isInt() && castLibId.intValue() == 0) {
            // All cast libs
            for (CastLib lib : movie.castManager.castLibs.values()) {
                count += lib.members.size();
            }
        } else {
            int libId = castLibId.intValue();
            CastLib lib = movie.castManager.castLibs.get(libId);
            if (lib != null) {
                count = lib.members.size();
            }
        }
        return com.dirplayer.director.lingo.Datum.ofInt(count);
    }

    public int getMemberProp(CastMemberRef memberRef, String propName) throws ScriptError {
        // Delegate to CastMemberRefHandlers which has comprehensive property handling
        return com.dirplayer.player.handlers.datum.CastMemberRefHandlers.getMemberProp(this, memberRef, propName);
    }

    public void setMemberProp(CastMemberRef memberRef, String propName, com.dirplayer.director.lingo.Datum value) throws ScriptError {
        // Allocate datum for the value, then delegate to handler
        int valueRef = allocDatum(value);
        com.dirplayer.player.handlers.datum.CastMemberRefHandlers.setMemberProp(this, memberRef, propName, valueRef);
    }

    public int getDateProp(int dateRef, String propName) throws ScriptError {
        DateObject dateObj = dateObjects.get(dateRef);
        if (dateObj == null) {
            throw new ScriptError("Date not found: " + dateRef);
        }
        switch (propName.toLowerCase()) {
            case "year":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(dateObj.dateTime.getYear()));
            case "month":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(dateObj.dateTime.getMonthValue()));
            case "day":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(dateObj.dateTime.getDayOfMonth()));
            case "hour":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(dateObj.dateTime.getHour()));
            case "minute":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(dateObj.dateTime.getMinute()));
            case "second":
                return allocDatum(com.dirplayer.director.lingo.Datum.ofInt(dateObj.dateTime.getSecond()));
            default:
                return 0;
        }
    }

    // --- Script helper methods ---

    public com.dirplayer.player.script.Script getCurrentScript(com.dirplayer.player.bytecode.BytecodeHandlerContext ctx) {
        ScriptScope scope = scopes.get(ctx.scopeRef);
        if (scope != null && scope.scriptMemberRef != null) {
            return movie.castManager.getScriptByRef(scope.scriptMemberRef);
        }
        return null;
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

    // --- Core Runtime Methods ---

    /**
     * Push a new scope onto the call stack.
     * @return The scope reference (index)
     */
    public int pushScope() throws ScriptError {
        if ((scopeCount + 1) >= MAX_STACK_SIZE) {
            // Build a stack trace for debugging
            StringBuilder stackTrace = new StringBuilder();
            stackTrace.append("Stack overflow detected - likely infinite recursion.\nRecent scope stack:\n");
            int start = scopeCount > 10 ? scopeCount - 10 : 0;
            for (int i = start; i < scopeCount; i++) {
                ScriptScope scope = scopes.get(i);
                if (scope != null) {
                    String handlerInfo = "unknown";
                    if (scope.scriptMemberRef != null) {
                        Script script = movie.castManager.getScriptByRef(scope.scriptMemberRef);
                        if (script != null) {
                            handlerInfo = script.name + "::handler_" + scope.handlerNameId;
                        }
                    }
                    stackTrace.append("  Scope ").append(i).append(": ").append(handlerInfo)
                             .append(" (bytecodeIndex=").append(scope.bytecodeIndex).append(")\n");
                }
            }
            throw new ScriptError("Stack overflow - infinite recursion in Lingo scripts\n" + stackTrace.toString());
        }

        int scopeRef = scopeCount;
        ScriptScope scope;
        if (scopeRef < scopes.size()) {
            scope = scopes.get(scopeRef);
            if (scope == null) {
                scope = new ScriptScope();
                scopes.set(scopeRef, scope);
            } else {
                scope.reset();
            }
        } else {
            scope = new ScriptScope();
            scopes.add(scope);
        }
        scopeCount++;
        return scopeRef;
    }

    /**
     * Pop the top scope from the call stack.
     */
    public void popScope() {
        if (scopeCount > 0) {
            scopeCount--;
        }
    }

    /**
     * Get the next frame number.
     * @return The next frame to advance to
     */
    public int getNextFrame() {
        if (!isPlaying) {
            return movie.currentFrame;
        } else if (nextFrame != null) {
            return nextFrame;
        } else {
            return movie.currentFrame + 1;
        }
    }

    /**
     * Advance to the next frame.
     */
    public void advanceFrame() {
        if (!isPlaying) {
            return;
        }

        int prevFrame = movie.currentFrame;
        int nextFrameVal = getNextFrame();

        // Clear the next frame jump
        nextFrame = null;
        movie.currentFrame = nextFrameVal;

        // Advance filmloop frames
        advanceFilmloopFrames();

        // Dispatch frame changed event if frame actually changed
        if (!movie.updateLock && prevFrame != movie.currentFrame) {
            // JsApi.dispatchFrameChanged(movie.currentFrame);
            hasPlayerFrameChanged = true;
        }
    }

    /**
     * Advance all filmloop member frames.
     */
    private void advanceFilmloopFrames() {
        for (com.dirplayer.player.score.SpriteChannel channel : movie.score.channels) {
            Sprite sprite = channel.sprite;
            if (sprite != null && sprite.memberRef != null) {
                CastMember member = movie.castManager.findMemberByRef(sprite.memberRef);
                if (member != null && member.memberType == com.dirplayer.director.MemberType.FilmLoop) {
                    // Advance filmloop frame
                    if (member.specificData instanceof com.dirplayer.player.cast.FilmLoopMember) {
                        com.dirplayer.player.cast.FilmLoopMember filmLoop =
                            (com.dirplayer.player.cast.FilmLoopMember) member.specificData;
                        filmLoop.advanceFrame();
                    }
                }
            }
        }
    }

    /**
     * Initialize sprites for the current frame.
     */
    public void beginAllSprites() {
        movie.score.beginSprites(com.dirplayer.player.score.ScoreRef.stage(), movie.currentFrame);

        // Cache the tempo for this frame
        currentFrameTempo = movie.getEffectiveTempo();

        // If the player isn't playing yet, reset entered flags
        if (!isPlaying) {
            for (com.dirplayer.player.score.SpriteChannel channel : movie.score.channels) {
                if (channel.sprite != null) {
                    channel.sprite.entered = false;
                    channel.sprite.scriptInstanceList.clear();
                }
            }
        }

        // Attach behaviors to sprites (port of Rust's behavior attachment logic)
        attachBehaviorsToSprites();

        // Handle frame script (channel 0 behavior)
        attachFrameScript();

        // Handle filmloop sprites
        java.util.Set<CastMemberRef> processedFilmLoops = new java.util.HashSet<>();
        for (com.dirplayer.player.score.SpriteChannel channel : movie.score.channels) {
            Sprite sprite = channel.sprite;
            if (sprite != null && sprite.memberRef != null) {
                CastMember member = movie.castManager.findMemberByRef(sprite.memberRef);
                if (member != null && member.memberType == com.dirplayer.director.MemberType.FilmLoop) {
                    if (!processedFilmLoops.contains(sprite.memberRef)) {
                        processedFilmLoops.add(sprite.memberRef);
                        // Initialize filmloop score sprites
                        if (member.specificData instanceof com.dirplayer.player.cast.FilmLoopMember) {
                            com.dirplayer.player.cast.FilmLoopMember filmLoop =
                                (com.dirplayer.player.cast.FilmLoopMember) member.specificData;
                            filmLoop.reset();
                            // The filmloop's score sprites are managed by the FilmLoopMember's own Score
                            // They will be initialized when the filmloop begins playing
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a behavior script instance.
     * Port of Rust Score::create_behavior.
     * @return Integer ID of the script instance, or null if script not found
     */
    private Integer createBehavior(int castLib, int castMember, Integer defaultCastLib) {
        // Try to find the script in the specified cast
        CastMemberRef scriptRef = new CastMemberRef(castLib, castMember);
        com.dirplayer.player.script.Script script = movie.castManager.getScriptByRef(scriptRef);

        // If not found and we have a default cast lib, try that
        if (script == null && defaultCastLib != null) {
            scriptRef = new CastMemberRef(defaultCastLib, castMember);
            script = movie.castManager.getScriptByRef(scriptRef);
        }

        if (script == null) {
            return null;
        }

        // Create a script instance
        com.dirplayer.player.script.ScriptInstance instance = new com.dirplayer.player.script.ScriptInstance();
        instance.script = scriptRef;
        instance.ancestor = 0;
        instance.beginSpriteCalled = false;

        // Allocate and store the instance
        int instanceId = allocator.allocScriptInstance(instance);

        return instanceId;
    }

    /**
     * Attach behaviors to sprites based on frame intervals and sprite details.
     * Port of Rust Score::begin_sprites behavior attachment section.
     */
    private void attachBehaviorsToSprites() {
        int frameNum = movie.currentFrame;

        // Get active spans for this frame
        java.util.List<com.dirplayer.player.score.ScoreSpriteSpan> activeSpans =
            movie.score.spriteSpans.stream()
                .filter(span -> com.dirplayer.player.score.Score.isSpanInFrame(span, frameNum))
                .collect(java.util.stream.Collectors.toList());

        // For each active span, check if it has behavior scripts
        for (com.dirplayer.player.score.ScoreSpriteSpan span : activeSpans) {
            Sprite sprite = movie.score.getSprite((short) span.channelNumber);
            if (sprite == null || !sprite.entered) {
                continue;
            }

            // Skip if behaviors already attached
            if (!sprite.scriptInstanceList.isEmpty()) {
                continue;
            }

            // Attach behaviors from span scripts (if any)
            if (span.scripts != null) {
                for (com.dirplayer.player.score.ScoreBehaviorReference behaviorRef : span.scripts) {
                    Integer instanceId = createBehavior(
                        behaviorRef.castLib,
                        behaviorRef.castMember,
                        1  // Default to cast 1 for main movie
                    );

                    if (instanceId != null) {
                        sprite.scriptInstanceList.add(instanceId);
                        logger.debug("Attached behavior {}/{} to sprite {}",
                            behaviorRef.castLib, behaviorRef.castMember, span.channelNumber);
                    }
                }
            }
        }

        // Attach behaviors from sprite details (D6+ mechanism)
        // This uses the spriteListIdx from the channel initialization data
        for (com.dirplayer.player.score.SpriteChannel channel : movie.score.channels) {
            Sprite sprite = channel.sprite;
            if (sprite == null || !sprite.entered) {
                continue;
            }

            // Skip if behaviors already attached from spans
            if (!sprite.scriptInstanceList.isEmpty()) {
                continue;
            }

            // Find the initialization data for this sprite's channel at current frame
            int channelNumber = channel.number;
            int spriteListIdx = 0;
            for (com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry entry : movie.score.channelInitializationData) {
                int entryChannelNum = com.dirplayer.player.score.KeyframeUtils.getChannelNumberFromIndex(entry.channelIndex);
                if (entryChannelNum == channelNumber && entry.frameIndex + 1 <= frameNum) {
                    // Get spriteListIdx from data
                    spriteListIdx = entry.data.getSpriteListIdx();
                }
            }

            if (spriteListIdx <= 0) {
                continue;
            }

            // Look up sprite detail by spriteListIdx
            com.dirplayer.director.chunks.ScoreChunk.SpriteDetailInfo detailInfo =
                movie.score.spriteDetails.get(spriteListIdx);
            if (detailInfo == null || detailInfo.behaviors == null || detailInfo.behaviors.isEmpty()) {
                continue;
            }

            for (com.dirplayer.director.chunks.ScoreChunk.SpriteBehavior behavior : detailInfo.behaviors) {
                Integer instanceId = createBehavior(
                    behavior.castLib,
                    behavior.castMember,
                    1  // Default to cast 1
                );

                if (instanceId != null) {
                    sprite.scriptInstanceList.add(instanceId);
                    logger.debug("Attached detail behavior {}/{} to sprite {}",
                        behavior.castLib, behavior.castMember, channel.number);
                }
            }
        }
    }

    /**
     * Attach the frame script (channel 0 behavior) if present.
     */
    private void attachFrameScript() {
        com.dirplayer.player.score.ScoreBehaviorReference frameScript =
            movie.score.getScriptInFrame(movie.currentFrame);

        if (frameScript == null) {
            movie.frameScriptMember = null;
            movie.frameScriptInstance = null;
            return;
        }

        // Check if we already have a frame script instance
        if (movie.frameScriptInstance != null) {
            return;
        }

        CastMemberRef scriptRef = new CastMemberRef(frameScript.castLib, frameScript.castMember);

        // Create frame script instance
        Integer instanceId = createBehavior(frameScript.castLib, frameScript.castMember, 1);

        if (instanceId != null) {
            movie.frameScriptMember = scriptRef;
            movie.frameScriptInstance = instanceId;
            logger.debug("Attached frame script {}/{} instance {}",
                frameScript.castLib, frameScript.castMember, instanceId);
        }
    }

    /**
     * Get the effective frames per second.
     * @return FPS value
     */
    public int getFps() {
        if (movie.puppetTempo > 0) {
            return movie.puppetTempo;
        }
        return movie.frameRate;
    }

    /**
     * Get a global variable by name.
     * @param name The variable name
     * @return The Datum value, or null if not found
     */
    public com.dirplayer.director.lingo.Datum getGlobal(String name) {
        Integer datumRef = globals.get(name);
        if (datumRef != null) {
            return getDatum(datumRef);
        }
        return null;
    }

    /**
     * Set a global variable.
     * @param name The variable name
     * @param valueRef The datum reference to store
     */
    public void setGlobal(String name, int valueRef) {
        globals.put(name, valueRef);
    }

    /**
     * Get all hydrated globals (name -> actual Datum).
     * @return Map of global name to Datum values
     */
    public Map<String, com.dirplayer.director.lingo.Datum> getHydratedGlobals() {
        Map<String, com.dirplayer.director.lingo.Datum> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : globals.entrySet()) {
            result.put(entry.getKey(), getDatum(entry.getValue()));
        }
        return result;
    }

    /**
     * Handle a script error.
     * @param err The error that occurred
     */
    public void onScriptError(ScriptError err) {
        logger.warn("[!!] Script error: {}", err.getMessage());
        if (err.getCause() != null) {
            err.getCause().printStackTrace();
        }

        // Print current scope info for debugging
        if (scopeCount > 0) {
            ScriptScope scope = scopes.get(scopeCount - 1);
            if (scope != null && scope.scriptMemberRef != null) {
                logger.warn("  In script: {}, handler ID: {}, bytecode index: {}",
                    scope.scriptMemberRef, scope.handlerNameId, scope.bytecodeIndex);
            }
        }

        // Don't stop on script errors during development - just log them
        // stop();

        // Dispatch debug update with full call stack
        // JsApi.dispatchDebugUpdate(this);
        // JsApi.dispatchScriptError(this, err);
    }

    /**
     * Pause script execution (not playback).
     */
    public void pauseScript() {
        isScriptPaused = true;
    }

    /**
     * Resume paused script execution.
     */
    public void resumeScript() {
        isScriptPaused = false;
    }

    /**
     * Check if a custom font is loaded.
     * @param fontName The font name to check
     * @return true if the font is available
     */
    public boolean hasCustomFont(String fontName) {
        return fontManager.hasFont(fontName);
    }

    /**
     * List all available fonts.
     * @return Sorted list of font names
     */
    public List<String> listAvailableFonts() {
        return fontManager.listFonts();
    }

    /**
     * Get a sound channel datum.
     * @param channelNum The channel number (1-8)
     * @return The sound channel datum reference
     */
    public int getSoundChannel(int channelNum) throws ScriptError {
        // Allocate a SoundChannel datum
        return allocDatum(com.dirplayer.director.lingo.Datum.ofSoundChannel(channelNum));
    }

    /**
     * Play a sound on a channel.
     * @param channelNum The channel number
     * @param memberRef The sound member reference datum
     */
    public void puppetSound(int channelNum, int memberRef) throws ScriptError {
        com.dirplayer.director.lingo.Datum memberDatum = getDatum(memberRef);
        CastMemberRef ref = memberDatum.toCastMemberRef();
        soundManager.playSound(channelNum, ref);
    }

    /**
     * Stop a sound channel.
     * @param channelNum The channel number
     */
    public void soundStop(int channelNum) throws ScriptError {
        soundManager.stopSound(channelNum);
    }

    /**
     * Get the XML nodes map for XML processing.
     * @return Map of XML node ID to XmlNode
     */
    public Map<Integer, XmlNode> getXmlNodes() {
        return xmlNodes;
    }

    /**
     * Add an XML node to the node storage.
     * @param node The node to add
     */
    public void addXmlNode(XmlNode node) {
        xmlNodes.put(node.id, node);
    }

    /**
     * Get an XML node by ID.
     * @param nodeId The node ID
     * @return The XML node or null
     */
    public XmlNode getXmlNode(int nodeId) {
        return xmlNodes.get(nodeId);
    }

    // ==================== Swing Player Support Methods ====================

    /**
     * Main tick method for playback loop.
     * Called each frame during playback.
     * Port of Rust execute_frame_update + run_frame_loop logic.
     */
    public void tick() {
        if (!isPlaying || isScriptPaused) {
            return;
        }

        try {
            // Execute frame update (runs every frame)
            executeFrameUpdate();

            // Frame advancement logic
            if (!hasPlayerFrameChanged) {
                // Dispatch exitFrame to all behaviors when frame hasn't changed via go()
                eventDispatcher.dispatchEventToAllBehaviors("exitFrame", new java.util.ArrayList<>());

                // Check if frame changed during exitFrame
                if (!hasFrameChangedInGo) {
                    // End sprites that are exiting
                    endExitingSprites();

                    // Advance the frame
                    advanceFrame();
                    hasPlayerFrameChanged = false;
                } else {
                    hasFrameChangedInGo = false;
                }
            } else {
                // Frame was changed by a go() call - dispatch exitFrame to frame/movie scripts only
                try {
                    eventDispatcher.invokeFrameAndMovieScripts("exitFrame", new java.util.ArrayList<>());
                } catch (ScriptError e) {
                    logger.error("exitFrame error: {}", e.getMessage());
                }

                // End exiting sprites
                endExitingSprites();

                // Advance the frame
                advanceFrame();
                hasPlayerFrameChanged = false;
            }

            // Clear frame script instance for new frame
            movie.frameScriptInstance = null;

            // Initialize sprites for new frame
            beginAllSprites();

            // Apply tween modifiers
            movie.score.applyTweenModifiers(movie.currentFrame);

            // Update filmloop frames
            advanceFilmloopFrames();

            // Dispatch beginSprite to new sprites
            try {
                eventDispatcher.dispatchBeginSpriteEvent("beginSprite", new java.util.ArrayList<>());
            } catch (ScriptError e) {
                logger.error("beginSprite error: {}", e.getMessage());
            }

        } catch (Exception e) {
            isInFrameUpdate = false;
            logger.error("Tick error: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Execute frame update - runs every frame.
     * Port of Rust MovieHandlers::execute_frame_update.
     */
    private void executeFrameUpdate() {
        // Prevent re-entrant calls
        if (isInFrameUpdate || hasPlayerFrameChanged) {
            return;
        }

        isInFrameUpdate = true;

        try {
            // Apply tween modifiers for current frame
            movie.score.applyTweenModifiers(movie.currentFrame);

            // 1. Send stepFrame to actorList
            Integer actorListRef = globals.get("actorList");
            if (actorListRef != null) {
                com.dirplayer.director.lingo.Datum actorListDatum = getDatum(actorListRef);
                if (actorListDatum != null && actorListDatum.isList()) {
                    try {
                        java.util.List<Integer> items = actorListDatum.toList();
                        for (Integer actorRef : items) {
                            try {
                                callDatumHandler(actorRef, "stepFrame", new java.util.ArrayList<>());
                            } catch (ScriptError e) {
                                // Handler not found is normal
                                if (!e.getMessage().contains("Handler not found")) {
                                    logger.warn("stepFrame error: {}", e.getMessage());
                                }
                            }
                        }
                    } catch (ScriptError e) {
                        // Ignore list access errors
                    }
                }
            }

            // 2. Dispatch prepareFrame to timeout targets
            eventDispatcher.dispatchSystemEventToTimeouts("prepareFrame", new java.util.ArrayList<>());

            // 3. Dispatch prepareFrame to all behaviors
            inPrepareFrame = true;
            eventDispatcher.dispatchEventToAllBehaviors("prepareFrame", new java.util.ArrayList<>());
            inPrepareFrame = false;

            // 4. Dispatch enterFrame to all behaviors
            inEnterFrame = true;
            eventDispatcher.dispatchEventToAllBehaviors("enterFrame", new java.util.ArrayList<>());
            inEnterFrame = false;

        } finally {
            isInFrameUpdate = false;
        }
    }

    /**
     * End sprites that are exiting the current frame.
     */
    private void endExitingSprites() {
        java.util.List<Integer> exitingSpriteNums = new java.util.ArrayList<>();

        for (com.dirplayer.player.score.SpriteChannel channel : movie.score.channels) {
            if (channel.sprite != null && channel.sprite.entered && !channel.sprite.exited) {
                // Check if sprite should exit (not in active span for next frame)
                int nextFrame = getNextFrame();
                boolean stillActive = false;
                for (var span : movie.score.spriteSpans) {
                    if (span.channelNumber == channel.number &&
                        com.dirplayer.player.score.Score.isSpanInFrame(span, nextFrame)) {
                        stillActive = true;
                        break;
                    }
                }
                if (!stillActive) {
                    exitingSpriteNums.add(channel.number);
                }
            }
        }

        if (!exitingSpriteNums.isEmpty()) {
            eventDispatcher.dispatchEndSpriteEvent(exitingSpriteNums);

            // Mark sprites as exited
            for (Integer spriteNum : exitingSpriteNums) {
                Sprite sprite = movie.score.getSprite(spriteNum.shortValue());
                if (sprite != null) {
                    sprite.exited = true;
                }
            }
        }
    }

    /**
     * Debug step into - wrapper for Swing player.
     */
    public void debugStepInto() {
        stepInto();
    }

    /**
     * Debug step over - wrapper for Swing player.
     */
    public void debugStepOver() {
        stepOver();
    }

    /**
     * Debug step out - wrapper for Swing player.
     */
    public void debugStepOut() {
        stepOut();
    }

    /**
     * Evaluate a Lingo command and return the result as a string.
     * Used by the debug console.
     * @param command The Lingo command to evaluate
     * @return The result as a string, or null if no result
     */
    public String evaluateLingo(String command) {
        try {
            evalLingoCommand(command);
            // For simple commands, return the effect
            command = command.trim().toLowerCase();
            if (command.startsWith("put ")) {
                // Parse and evaluate the expression after "put"
                String expr = command.substring(4).trim();
                // Try to evaluate common expressions
                if (expr.equals("the frame")) {
                    return String.valueOf(movie.currentFrame);
                } else if (expr.equals("the moviename")) {
                    return movie.fileName;
                } else if (expr.startsWith("sprite(") && expr.contains(").")) {
                    // Parse sprite property access
                    return evaluateSpriteExpr(expr);
                }
                return expr;
            }
            return null;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Helper to evaluate sprite expressions for debug console.
     */
    private String evaluateSpriteExpr(String expr) {
        try {
            // Parse sprite(N).property
            int start = expr.indexOf('(') + 1;
            int end = expr.indexOf(')');
            int spriteNum = Integer.parseInt(expr.substring(start, end).trim());
            String prop = expr.substring(expr.indexOf('.') + 1).trim();

            Sprite sprite = getSprite(spriteNum);
            if (sprite == null) {
                return "sprite not found";
            }

            switch (prop.toLowerCase()) {
                case "loch":
                    return String.valueOf(sprite.locH);
                case "locv":
                    return String.valueOf(sprite.locV);
                case "visible":
                    return String.valueOf(sprite.visible);
                case "member":
                    return sprite.memberRef != null ? sprite.memberRef.toString() : "void";
                default:
                    return "unknown property: " + prop;
            }
        } catch (Exception e) {
            return "parse error";
        }
    }

    /**
     * Get all global variables for debug display.
     * @return Map of global variable names to their Datum values
     */
    public Map<String, com.dirplayer.director.lingo.Datum> getGlobals() {
        return getHydratedGlobals();
    }

    /**
     * Handle key down with keyCode and keyChar (Swing-style).
     * @param keyCode The key code (from KeyEvent)
     * @param keyChar The character typed
     */
    public void handleKeyDown(int keyCode, char keyChar) {
        keyboardManager.setLastKeyCode(keyCode);
        keyboardManager.setLastKey(String.valueOf(keyChar));
        keyDown(String.valueOf(keyChar), keyCode);
    }

    /**
     * Handle key up with keyCode and keyChar (Swing-style).
     * @param keyCode The key code (from KeyEvent)
     * @param keyChar The character typed
     */
    public void handleKeyUp(int keyCode, char keyChar) {
        keyUp(String.valueOf(keyChar), keyCode);
    }
}
