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
        ctx2d.setImageSmoothingEnabled(false);

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
        previewCtx2d.setImageSmoothingEnabled(false);
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

        // TODO: Implement preview rendering
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
        // TODO: Implement cast member info serialization
        return "{}";
    }

    /**
     * Get sprite info as JSON.
     */
    @JSExport
    public static String playerGetSpriteInfo(int channelNum) {
        if (player == null) {
            return "{}";
        }
        // TODO: Implement sprite info serialization
        return "{}";
    }

    /**
     * Execute a Lingo command.
     */
    @JSExport
    public static String playerExecuteLingo(String code) {
        if (player == null) {
            return "Error: Player not initialized";
        }
        // TODO: Implement Lingo evaluation
        return "";
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
}
