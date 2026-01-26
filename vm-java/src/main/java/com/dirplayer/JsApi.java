package com.dirplayer;

import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Movie;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.bitmap.BuiltInPalette;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.rendering.Renderer;
import com.dirplayer.rendering.IntRect;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSObject;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.typedarrays.Uint8ClampedArray;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JavaScript API for the DirPlayer.
 * Entry point for TeaVM WebAssembly/JavaScript compilation.
 * Port of Rust js_api.rs
 */
public class JsApi {
    private static DirPlayer player;
    private static HTMLCanvasElement canvas;
    private static CanvasRenderingContext2D ctx2d;
    private static HTMLCanvasElement previewCanvas;
    private static CanvasRenderingContext2D previewCtx2d;
    private static Bitmap renderBitmap;
    private static CastMemberRef previewMemberRef;
    private static Integer debugSelectedChannel;
    private static int lastFrameMs;

    /**
     * Main entry point - called when WASM module loads.
     */
    public static void main(String[] args) {
        // Initialize
        consoleLog("DirPlayer Java/TeaVM initialized");
    }

    /**
     * Initialize the player with movie data.
     */
    @JSExport
    public static void playerInit(byte[] movieData, String basePath) {
        try {
            player = new DirPlayer();
            player.loadMovie(movieData, basePath);
            consoleLog("Movie loaded: " + player.getMovie().getRect().width() + "x" +
                      player.getMovie().getRect().height());
        } catch (Exception e) {
            consoleError("Failed to load movie: " + e.getMessage());
        }
    }

    /**
     * Create the rendering canvas.
     */
    @JSExport
    public static void playerCreateCanvas(String containerSelector) {
        HTMLDocument document = HTMLDocument.current();
        HTMLElement container = (HTMLElement) document.querySelector(containerSelector);
        if (container == null) {
            consoleError("Container not found: " + containerSelector);
            return;
        }

        // Create main canvas
        canvas = (HTMLCanvasElement) document.createElement("canvas");
        int width = player.getMovie().getRect().width();
        int height = player.getMovie().getRect().height();
        canvas.setWidth(width);
        canvas.setHeight(height);
        setCanvasStyle(canvas);
        container.appendChild(canvas);

        ctx2d = (CanvasRenderingContext2D) canvas.getContext("2d");
        setImageSmoothingEnabled(ctx2d, false);

        // Initialize render bitmap
        renderBitmap = new Bitmap(width, height, 32, 32, 0,
            PaletteRef.ofBuiltIn(BuiltInPalette.SystemWin));

        consoleLog("Canvas created: " + width + "x" + height);
    }

    /**
     * Create preview canvas for cast member preview.
     */
    @JSExport
    public static void playerSetPreviewParent(String containerSelector) {
        if (containerSelector == null || containerSelector.isEmpty()) {
            previewCanvas = null;
            previewCtx2d = null;
            return;
        }

        HTMLDocument document = HTMLDocument.current();
        HTMLElement container = (HTMLElement) document.querySelector(containerSelector);
        if (container == null) {
            return;
        }

        previewCanvas = (HTMLCanvasElement) document.createElement("canvas");
        previewCanvas.setWidth(1);
        previewCanvas.setHeight(1);
        setCanvasStyle(previewCanvas);
        container.appendChild(previewCanvas);

        previewCtx2d = (CanvasRenderingContext2D) previewCanvas.getContext("2d");
        setImageSmoothingEnabled(previewCtx2d, false);
    }

    /**
     * Set the preview member reference.
     */
    @JSExport
    public static void playerSetPreviewMemberRef(int castLib, int castNum) {
        previewMemberRef = new CastMemberRef(castLib, castNum);
    }

    /**
     * Set debug selected channel.
     */
    @JSExport
    public static void playerSetDebugSelectedChannel(int channelNum) {
        debugSelectedChannel = channelNum;
        dispatchChannelChanged(channelNum);
    }

    /**
     * Step the player forward one frame.
     */
    @JSExport
    public static void playerStep() {
        if (player != null) {
            player.step();
        }
    }

    /**
     * Go to a specific frame.
     */
    @JSExport
    public static void playerGoToFrame(int frame) {
        if (player != null) {
            player.goToFrame(frame);
        }
    }

    /**
     * Play/resume the movie.
     */
    @JSExport
    public static void playerPlay() {
        if (player != null) {
            player.play();
        }
    }

    /**
     * Stop/pause the movie.
     */
    @JSExport
    public static void playerStop() {
        if (player != null) {
            player.stop();
        }
    }

    /**
     * Check if player is playing.
     */
    @JSExport
    public static boolean playerIsPlaying() {
        return player != null && player.isPlaying();
    }

    /**
     * Get current frame number.
     */
    @JSExport
    public static int playerGetCurrentFrame() {
        return player != null ? player.getMovie().getCurrentFrame() : 0;
    }

    /**
     * Get total frame count.
     */
    @JSExport
    public static int playerGetTotalFrames() {
        return player != null ? player.getMovie().getTotalFrames() : 0;
    }

    /**
     * Handle mouse move event.
     */
    @JSExport
    public static void playerMouseMove(int x, int y) {
        if (player != null) {
            player.handleMouseMove(x, y);
        }
    }

    /**
     * Handle mouse down event.
     */
    @JSExport
    public static void playerMouseDown(int x, int y, int button) {
        if (player != null) {
            player.handleMouseDown(x, y, button);
        }
    }

    /**
     * Handle mouse up event.
     */
    @JSExport
    public static void playerMouseUp(int x, int y, int button) {
        if (player != null) {
            player.handleMouseUp(x, y, button);
        }
    }

    /**
     * Handle key down event.
     */
    @JSExport
    public static void playerKeyDown(int keyCode, boolean shift, boolean ctrl, boolean alt) {
        if (player != null) {
            player.handleKeyDown(keyCode, shift, ctrl, alt);
        }
    }

    /**
     * Handle key up event.
     */
    @JSExport
    public static void playerKeyUp(int keyCode) {
        if (player != null) {
            player.handleKeyUp(keyCode);
        }
    }

    /**
     * Draw a frame to the canvas.
     */
    @JSExport
    public static void playerDrawFrame() {
        if (player == null || canvas == null || ctx2d == null) {
            return;
        }

        int movieWidth = player.getMovie().getRect().width();
        int movieHeight = player.getMovie().getRect().height();

        // Resize bitmap if needed
        if (renderBitmap.getWidth() != movieWidth || renderBitmap.getHeight() != movieHeight) {
            renderBitmap = new Bitmap(movieWidth, movieHeight, 32, 32, 0,
                PaletteRef.ofBuiltIn(BuiltInPalette.SystemWin));
        }

        // Render to bitmap
        Renderer.renderStageToBitmap(player, renderBitmap, debugSelectedChannel);

        // Draw bitmap to canvas
        drawBitmapToCanvas(renderBitmap, ctx2d);
    }

    /**
     * Draw preview frame if preview member is set.
     */
    @JSExport
    public static void playerDrawPreviewFrame() {
        if (player == null || previewCanvas == null || previewCtx2d == null || previewMemberRef == null) {
            return;
        }

        // Get member bitmap if available
        Bitmap memberBitmap = player.getMemberBitmap(previewMemberRef);
        if (memberBitmap == null) {
            return;
        }

        // Resize preview canvas if needed
        int width = memberBitmap.getWidth();
        int height = memberBitmap.getHeight();
        if (previewCanvas.getWidth() != width || previewCanvas.getHeight() != height) {
            previewCanvas.setWidth(width);
            previewCanvas.setHeight(height);
        }

        // Draw bitmap to preview canvas
        drawBitmapToCanvas(memberBitmap, previewCtx2d);
    }

    /**
     * Run the draw loop at specified FPS.
     */
    @JSExport
    public static void playerStartDrawLoop(int fps) {
        final int frameInterval = 1000 / fps;
        requestAnimationFrame(new AnimationFrameCallback() {
            @Override
            public void onFrame(double timestamp) {
                int currentMs = (int) timestamp;
                if (currentMs - lastFrameMs >= frameInterval) {
                    lastFrameMs = currentMs;
                    playerDrawFrame();
                    playerDrawPreviewFrame();
                }
                requestAnimationFrame(this);
            }
        });
    }

    /**
     * Get sprite info at a position.
     */
    @JSExport
    public static int playerGetSpriteAt(int x, int y) {
        if (player != null) {
            return player.getSpriteAt(x, y);
        }
        return 0;
    }

    /**
     * Get cast member info as JSON.
     */
    @JSExport
    public static String playerGetCastMemberInfo(int castLib, int castNum) {
        if (player == null) {
            return "{}";
        }

        com.dirplayer.player.CastMember member = player.getMovie().getCastManager().findMemberByRef(
            new CastMemberRef(castLib, castNum)
        );
        if (member == null) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"number\":").append(member.number).append(",");
        sb.append("\"castLib\":").append(castLib).append(",");
        sb.append("\"name\":\"").append(escapeJson(member.name != null ? member.name : "")).append("\",");
        sb.append("\"type\":\"").append(member.memberType != null ? member.memberType.name() : "Unknown").append("\"");

        // Add type-specific fields
        if (member.memberType != null) {
            switch (member.memberType) {
                case Bitmap:
                    sb.append(",\"width\":").append(member.bitmapWidth);
                    sb.append(",\"height\":").append(member.bitmapHeight);
                    sb.append(",\"bitDepth\":").append(member.bitDepth);
                    sb.append(",\"regPointX\":").append(member.regPointX);
                    sb.append(",\"regPointY\":").append(member.regPointY);
                    break;
                case Button:
                case RTE:
                case Text:
                    sb.append(",\"text\":\"").append(escapeJson(member.text != null ? member.text : "")).append("\"");
                    sb.append(",\"font\":\"").append(escapeJson(member.font != null ? member.font : "")).append("\"");
                    sb.append(",\"fontSize\":").append(member.fontSize);
                    break;
                case Sound:
                    sb.append(",\"sampleRate\":").append(member.sampleRate);
                    sb.append(",\"sampleSize\":").append(member.sampleSize);
                    break;
                default:
                    break;
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Get sprite info as JSON.
     */
    @JSExport
    public static String playerGetSpriteInfo(int channelNum) {
        if (player == null) {
            return "{}";
        }

        com.dirplayer.player.Sprite sprite = player.getSprite(channelNum);
        if (sprite == null) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"channel\":").append(sprite.number).append(",");
        sb.append("\"memberCastLib\":").append(sprite.memberRef != null ? sprite.memberRef.castLib : 0).append(",");
        sb.append("\"memberCastMember\":").append(sprite.memberRef != null ? sprite.memberRef.castMember : 0).append(",");
        sb.append("\"locH\":").append(sprite.locH).append(",");
        sb.append("\"locV\":").append(sprite.locV).append(",");
        sb.append("\"width\":").append(sprite.width).append(",");
        sb.append("\"height\":").append(sprite.height).append(",");
        sb.append("\"visible\":").append(sprite.visible).append(",");
        sb.append("\"puppet\":").append(sprite.puppet).append(",");
        sb.append("\"ink\":").append(sprite.ink).append(",");
        sb.append("\"blend\":").append(sprite.blend).append(",");
        sb.append("\"foreColor\":").append(sprite.foreColor).append(",");
        sb.append("\"backColor\":").append(sprite.backColor).append(",");
        sb.append("\"rotation\":").append(sprite.rotation);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Execute a Lingo command.
     */
    @JSExport
    public static String playerExecuteLingo(String code) {
        if (player == null) {
            return "Error: Player not initialized";
        }
        try {
            player.evalLingoCommand(code);
            return "";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ---- Helper methods ----

    private static void drawBitmapToCanvas(Bitmap bitmap, CanvasRenderingContext2D ctx) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] data = bitmap.getData();

        // Create ImageData
        ImageData imageData = ctx.createImageData(width, height);
        Uint8ClampedArray pixels = imageData.getData();

        // Copy bitmap data to ImageData (both are RGBA format)
        for (int i = 0; i < data.length; i++) {
            pixels.set(i, data[i] & 0xFF);
        }

        ctx.putImageData(imageData, 0, 0);
    }

    private static void setCanvasStyle(HTMLCanvasElement canvas) {
        setStyleProperty(canvas, "image-rendering", "pixelated");
        setStyleProperty(canvas, "image-rendering", "-moz-crisp-edges");
        setStyleProperty(canvas, "image-rendering", "crisp-edges");
    }

    // ---- Native JS methods via JSBody ----

    @JSBody(params = {"ctx", "enabled"},
            script = "ctx.imageSmoothingEnabled = enabled;")
    private static native void setImageSmoothingEnabled(CanvasRenderingContext2D ctx, boolean enabled);

    @JSBody(params = {"element", "property", "value"},
            script = "element.style[property] = value;")
    private static native void setStyleProperty(HTMLElement element, String property, String value);

    @JSBody(params = {"message"}, script = "console.log(message);")
    private static native void consoleLog(String message);

    @JSBody(params = {"message"}, script = "console.warn(message);")
    private static native void consoleWarn(String message);

    @JSBody(params = {"message"}, script = "console.error(message);")
    private static native void consoleError(String message);

    @JSBody(params = {"channelNum"},
            script = "if (window.onDirPlayerChannelChanged) window.onDirPlayerChannelChanged(channelNum);")
    private static native void dispatchChannelChanged(int channelNum);

    @JSBody(params = {"callback"}, script = "requestAnimationFrame(callback);")
    private static native void requestAnimationFrame(AnimationFrameCallback callback);

    /**
     * Callback interface for animation frames.
     */
    public interface AnimationFrameCallback extends JSObject {
        void onFrame(double timestamp);
    }

    /**
     * Create a safe JS string (handles null/undefined).
     */
    @JSBody(params = {"str"}, script = "return str || '';")
    public static native String safeJsString(String str);

    // ---- Dispatch methods for JS callbacks ----

    /**
     * Dispatch movie loaded event.
     */
    public static void dispatchMovieLoaded(int version, String info) {
        onMovieLoaded(version, info);
    }

    @JSBody(params = {"version", "info"},
            script = "if (window.onDirPlayerMovieLoaded) window.onDirPlayerMovieLoaded({version: version, info: info});")
    private static native void onMovieLoaded(int version, String info);

    /**
     * Dispatch cast list changed event.
     */
    public static void dispatchCastListChanged() {
        onCastListChanged();
    }

    @JSBody(params = {},
            script = "if (window.onDirPlayerCastListChanged) window.onDirPlayerCastListChanged();")
    private static native void onCastListChanged();

    /**
     * Dispatch cast member list changed event.
     */
    public static void dispatchCastMemberListChanged(int castNumber) {
        onCastMemberListChanged(castNumber);
    }

    @JSBody(params = {"castNumber"},
            script = "if (window.onDirPlayerCastMemberListChanged) window.onDirPlayerCastMemberListChanged(castNumber);")
    private static native void onCastMemberListChanged(int castNumber);

    /**
     * Dispatch cast member changed event.
     */
    public static void dispatchCastMemberChanged(int castLib, int castMember) {
        onCastMemberChanged(castLib, castMember);
    }

    @JSBody(params = {"castLib", "castMember"},
            script = "if (window.onDirPlayerCastMemberChanged) window.onDirPlayerCastMemberChanged(castLib, castMember);")
    private static native void onCastMemberChanged(int castLib, int castMember);

    /**
     * Dispatch score changed event.
     */
    public static void dispatchScoreChanged() {
        onScoreChanged();
    }

    @JSBody(params = {},
            script = "if (window.onDirPlayerScoreChanged) window.onDirPlayerScoreChanged();")
    private static native void onScoreChanged();

    /**
     * Dispatch frame changed event.
     */
    public static void dispatchFrameChanged(int frame) {
        onFrameChanged(frame);
    }

    @JSBody(params = {"frame"},
            script = "if (window.onDirPlayerFrameChanged) window.onDirPlayerFrameChanged(frame);")
    private static native void onFrameChanged(int frame);

    /**
     * Dispatch script error event.
     */
    public static void dispatchScriptError(String message, int castLib, int castMember, String handlerName, boolean isPaused) {
        onScriptError(message, castLib, castMember, handlerName, isPaused);
    }

    @JSBody(params = {"message", "castLib", "castMember", "handlerName", "isPaused"},
            script = "if (window.onDirPlayerScriptError) window.onDirPlayerScriptError({message: message, castLib: castLib, castMember: castMember, handlerName: handlerName, isPaused: isPaused});")
    private static native void onScriptError(String message, int castLib, int castMember, String handlerName, boolean isPaused);

    /**
     * Dispatch script error cleared event.
     */
    public static void dispatchScriptErrorCleared() {
        onScriptErrorCleared();
    }

    @JSBody(params = {},
            script = "if (window.onDirPlayerScriptErrorCleared) window.onDirPlayerScriptErrorCleared();")
    private static native void onScriptErrorCleared();

    /**
     * Dispatch debug message event.
     */
    public static void dispatchDebugMessage(String message) {
        onDebugMessage(message);
    }

    @JSBody(params = {"message"},
            script = "if (window.onDirPlayerDebugMessage) window.onDirPlayerDebugMessage(message);")
    private static native void onDebugMessage(String message);

    /**
     * Dispatch schedule timeout event.
     */
    public static void dispatchScheduleTimeout(String timeoutName, int interval) {
        onScheduleTimeout(timeoutName, interval);
    }

    @JSBody(params = {"timeoutName", "interval"},
            script = "if (window.onDirPlayerScheduleTimeout) window.onDirPlayerScheduleTimeout(timeoutName, interval);")
    private static native void onScheduleTimeout(String timeoutName, int interval);

    /**
     * Dispatch clear timeout event.
     */
    public static void dispatchClearTimeout(String timeoutName) {
        onClearTimeout(timeoutName);
    }

    @JSBody(params = {"timeoutName"},
            script = "if (window.onDirPlayerClearTimeout) window.onDirPlayerClearTimeout(timeoutName);")
    private static native void onClearTimeout(String timeoutName);

    /**
     * Dispatch global list changed event.
     */
    public static void dispatchGlobalListChanged() {
        onGlobalListChanged();
    }

    @JSBody(params = {},
            script = "if (window.onDirPlayerGlobalListChanged) window.onDirPlayerGlobalListChanged();")
    private static native void onGlobalListChanged();

    /**
     * Dispatch external event.
     */
    public static void dispatchExternalEvent(String eventName) {
        onExternalEvent(eventName);
    }

    @JSBody(params = {"eventName"},
            script = "if (window.onDirPlayerExternalEvent) window.onDirPlayerExternalEvent(eventName);")
    private static native void onExternalEvent(String eventName);

    /**
     * ASCII safe string conversion.
     */
    public static String asciiSafe(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            int code = c;
            if (code == 9 || code == 10 || code == 13 || (code >= 32 && code <= 126)) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    // =====================================================
    // External Parameters and Configuration
    // =====================================================

    /**
     * Set external parameters (e.g., from HTML embed).
     */
    @JSExport
    public static void setExternalParams(JSObject params) {
        if (player != null) {
            player.setExternalParams(params);
        }
    }

    /**
     * Set the base path for loading assets.
     */
    @JSExport
    public static void setBasePath(String path) {
        if (player != null) {
            player.setBasePath(path);
        }
    }

    /**
     * Set the system font path.
     */
    @JSExport
    public static void setSystemFontPath(String path) {
        if (player != null) {
            player.setSystemFontPath(path);
        }
    }

    /**
     * Load movie from file path.
     */
    @JSExport
    public static void loadMovieFile(String path, boolean autoplay) {
        if (player != null) {
            player.loadMovieFromFile(path, autoplay);
        }
    }

    /**
     * Set stage size.
     */
    @JSExport
    public static void setStageSize(int width, int height) {
        if (player != null) {
            player.setStageSize(width, height);
        }
    }

    // =====================================================
    // Player Control - Direct access (bypasses command queue)
    // =====================================================

    /**
     * Reset the player.
     */
    @JSExport
    public static void playerReset() {
        if (player != null) {
            player.reset();
        }
    }

    // =====================================================
    // Breakpoint Management
    // =====================================================

    /**
     * Add a breakpoint at the specified location.
     */
    @JSExport
    public static void addBreakpoint(String scriptName, String handlerName, int bytecodeIndex) {
        if (player != null) {
            player.getBreakpointManager().addBreakpoint(scriptName, handlerName, bytecodeIndex);
        }
    }

    /**
     * Remove a breakpoint at the specified location.
     */
    @JSExport
    public static void removeBreakpoint(String scriptName, String handlerName, int bytecodeIndex) {
        if (player != null) {
            player.getBreakpointManager().removeBreakpoint(scriptName, handlerName, bytecodeIndex);
        }
    }

    /**
     * Toggle a breakpoint at the specified location.
     */
    @JSExport
    public static void toggleBreakpoint(String scriptName, String handlerName, int bytecodeIndex) {
        if (player != null) {
            player.getBreakpointManager().toggleBreakpoint(scriptName, handlerName, bytecodeIndex);
        }
    }

    /**
     * Resume execution from a breakpoint.
     */
    @JSExport
    public static void resumeBreakpoint() {
        if (player != null) {
            player.resumeBreakpoint();
        }
    }

    // =====================================================
    // Step Debugging
    // =====================================================

    /**
     * Step into the next instruction.
     */
    @JSExport
    public static void stepInto() {
        if (player != null) {
            player.stepInto();
        }
    }

    /**
     * Step over the current instruction.
     */
    @JSExport
    public static void stepOver() {
        if (player != null) {
            player.stepOver();
        }
    }

    /**
     * Step out of the current handler.
     */
    @JSExport
    public static void stepOut() {
        if (player != null) {
            player.stepOut();
        }
    }

    /**
     * Step over to next line, skipping specified bytecode indices.
     */
    @JSExport
    public static void stepOverLine(int[] skipBytecodeIndices) {
        if (player != null) {
            List<Integer> indices = Arrays.stream(skipBytecodeIndices)
                .boxed()
                .collect(Collectors.toList());
            player.stepOverLine(indices);
        }
    }

    /**
     * Step into line, skipping specified bytecode indices.
     */
    @JSExport
    public static void stepIntoLine(int[] skipBytecodeIndices) {
        if (player != null) {
            List<Integer> indices = Arrays.stream(skipBytecodeIndices)
                .boxed()
                .collect(Collectors.toList());
            player.stepIntoLine(indices);
        }
    }

    /**
     * Set whether to break on script errors.
     */
    @JSExport
    public static void setBreakOnError(boolean enabled) {
        if (player != null) {
            player.setBreakOnError(enabled);
        }
    }

    /**
     * Get whether break on error is enabled.
     */
    @JSExport
    public static boolean getBreakOnError() {
        return player != null && player.getBreakOnError();
    }

    // =====================================================
    // Timeout Management
    // =====================================================

    /**
     * Trigger a named timeout.
     */
    @JSExport
    public static void triggerTimeout(String name) {
        if (player != null) {
            player.triggerTimeout(name);
        }
    }

    /**
     * Dispatch clear all timeouts event.
     */
    public static void dispatchClearTimeouts() {
        onClearTimeouts();
    }

    @JSBody(params = {},
            script = "if (window.onDirPlayerClearTimeouts) window.onDirPlayerClearTimeouts();")
    private static native void onClearTimeouts();

    // =====================================================
    // Datum and Script Instance Inspection
    // =====================================================

    /**
     * Request a datum snapshot by ID.
     */
    @JSExport
    public static void requestDatum(int datumId) {
        if (player != null) {
            player.requestDatumSnapshot(datumId);
        }
    }

    /**
     * Request a script instance snapshot by ID.
     */
    @JSExport
    public static void requestScriptInstanceSnapshot(int scriptInstanceId) {
        if (player != null) {
            player.requestScriptInstanceSnapshot(scriptInstanceId);
        }
    }

    /**
     * Dispatch datum snapshot event.
     */
    public static void dispatchDatumSnapshot(int datumId, JSObject data) {
        onDatumSnapshot(datumId, data);
    }

    @JSBody(params = {"datumId", "data"},
            script = "if (window.onDirPlayerDatumSnapshot) window.onDirPlayerDatumSnapshot(datumId, data);")
    private static native void onDatumSnapshot(int datumId, JSObject data);

    /**
     * Dispatch script instance snapshot event.
     */
    public static void dispatchScriptInstanceSnapshot(int scriptInstanceId, JSObject data) {
        onScriptInstanceSnapshot(scriptInstanceId, data);
    }

    @JSBody(params = {"scriptInstanceId", "data"},
            script = "if (window.onDirPlayerScriptInstanceSnapshot) window.onDirPlayerScriptInstanceSnapshot(scriptInstanceId, data);")
    private static native void onScriptInstanceSnapshot(int scriptInstanceId, JSObject data);

    // =====================================================
    // Cast Member Subscription
    // =====================================================

    /**
     * Subscribe to changes on a cast member.
     */
    @JSExport
    public static void subscribeToMember(int castLib, int castMember) {
        if (player != null) {
            player.subscribeToMember(castLib, castMember);
            dispatchCastMemberChanged(castLib, castMember);
        }
    }

    /**
     * Unsubscribe from changes on a cast member.
     */
    @JSExport
    public static void unsubscribeFromMember(int castLib, int castMember) {
        if (player != null) {
            player.unsubscribeFromMember(castLib, castMember);
        }
    }

    // =====================================================
    // Channel Name Subscription
    // =====================================================

    /**
     * Subscribe to channel name changes.
     */
    @JSExport
    public static void subscribeToChannelNames() {
        if (player != null) {
            player.setSubscribedToChannelNames(true);
            // Dispatch initial channel names
            int channelCount = player.getMovie().getScore().getChannelCount();
            for (int i = 0; i < channelCount; i++) {
                dispatchChannelDisplayNameChanged(i, getChannelDisplayName(i));
            }
        }
    }

    /**
     * Unsubscribe from channel name changes.
     */
    @JSExport
    public static void unsubscribeFromChannelNames() {
        if (player != null) {
            player.setSubscribedToChannelNames(false);
        }
    }

    /**
     * Get display name for a channel.
     */
    private static String getChannelDisplayName(int channelNum) {
        if (player == null) return "";
        return player.getChannelDisplayName(channelNum);
    }

    /**
     * Dispatch channel display name changed event.
     */
    public static void dispatchChannelDisplayNameChanged(int channelNum, String displayName) {
        onChannelDisplayNameChanged(channelNum, displayName != null ? displayName : "");
    }

    @JSBody(params = {"channelNum", "displayName"},
            script = "if (window.onDirPlayerChannelDisplayNameChanged) window.onDirPlayerChannelDisplayNameChanged(channelNum, displayName);")
    private static native void onChannelDisplayNameChanged(int channelNum, String displayName);

    // =====================================================
    // Net Task Support
    // =====================================================

    /**
     * Provide data for a pending network task.
     */
    @JSExport
    public static void provideNetTaskData(int taskId, byte[] data) {
        if (player != null) {
            player.provideNetTaskData(taskId, data);
        }
    }

    // =====================================================
    // Lingo Evaluation
    // =====================================================

    /**
     * Evaluate a Lingo command.
     */
    @JSExport
    public static void evalCommand(String command) {
        if (player != null) {
            dispatchDebugMessage(command);
            player.evalLingoCommand(command);
        }
    }

    // =====================================================
    // Alert Hook
    // =====================================================

    /**
     * Trigger the alert hook.
     */
    @JSExport
    public static void triggerAlertHook() {
        if (player != null) {
            player.triggerAlertHook();
        }
    }

    // =====================================================
    // Scope and Debug State
    // =====================================================

    /**
     * Dispatch scope list changed event.
     */
    public static void dispatchScopeListChanged(JSObject[] scopes) {
        onScopeListChanged(scopes);
    }

    @JSBody(params = {"scopes"},
            script = "if (window.onDirPlayerScopeListChanged) window.onDirPlayerScopeListChanged(scopes);")
    private static native void onScopeListChanged(JSObject[] scopes);

    /**
     * Dispatch breakpoint list changed event.
     */
    public static void dispatchBreakpointListChanged(JSObject[] breakpoints) {
        onBreakpointListChanged(breakpoints);
    }

    @JSBody(params = {"breakpoints"},
            script = "if (window.onDirPlayerBreakpointListChanged) window.onDirPlayerBreakpointListChanged(breakpoints);")
    private static native void onBreakpointListChanged(JSObject[] breakpoints);

    /**
     * Dispatch debug update - scope list and global list.
     */
    public static void dispatchDebugUpdate() {
        if (player != null) {
            // This should be called by player when debug state changes
            dispatchGlobalListChanged();
        }
    }

    // =====================================================
    // Cast Library Name Changed
    // =====================================================

    /**
     * Dispatch cast library name changed event.
     */
    public static void dispatchCastLibNameChanged(int castNumber, String name) {
        onCastLibNameChanged(castNumber, name != null ? name : "");
    }

    @JSBody(params = {"castNumber", "name"},
            script = "if (window.onDirPlayerCastLibNameChanged) window.onDirPlayerCastLibNameChanged(castNumber, name);")
    private static native void onCastLibNameChanged(int castNumber, String name);

    // =====================================================
    // Movie Chunk List
    // =====================================================

    /**
     * Dispatch movie chunk list changed event.
     */
    public static void dispatchMovieChunkListChanged(JSObject chunks) {
        onMovieChunkListChanged(chunks);
    }

    @JSBody(params = {"chunks"},
            script = "if (window.onDirPlayerMovieChunkListChanged) window.onDirPlayerMovieChunkListChanged(chunks);")
    private static native void onMovieChunkListChanged(JSObject chunks);

    // =====================================================
    // Print Member Bitmap (Debug)
    // =====================================================

    /**
     * Print member bitmap as hex (for debugging).
     */
    @JSExport
    public static void playerPrintMemberBitmapHex(int castLib, int castMember) {
        if (player != null) {
            player.printMemberBitmapHex(castLib, castMember);
        }
    }

    // =====================================================
    // Safe String Utility
    // =====================================================

    /**
     * Convert string to safe representation (UTF-8 lossy).
     */
    public static String safeString(String s) {
        if (s == null) return "";
        // Java strings are already UTF-16, convert to UTF-8 bytes and back
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
