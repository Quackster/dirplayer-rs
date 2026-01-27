package com.dirplayer.swing;

import com.dirplayer.director.DirectorFile;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Movie;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.rendering.Renderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test class for capturing screenshots of Director movies.
 * This helps verify that rendering is working correctly.
 */
public class ScreenshotTest {

    private static final String TEST_MOVIE_URL = "http://localhost/dcr/14.1_b8/habbo.dcr";
    private static final String OUTPUT_DIR = "test-screenshots";

    public static void main(String[] args) {
        try {
            // Create output directory
            Path outputPath = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            System.out.println("Loading movie: " + TEST_MOVIE_URL);

            // Determine base path from URL
            String basePath = TEST_MOVIE_URL.substring(0, TEST_MOVIE_URL.lastIndexOf('/') + 1);
            String fileName = TEST_MOVIE_URL.substring(TEST_MOVIE_URL.lastIndexOf('/') + 1);

            // Create player
            DirPlayer player = new DirPlayer();
            player.netManager.setSynchronousMode(true);
            player.netManager.setBasePath(basePath);

            // Download the movie
            System.out.println("Downloading from: " + TEST_MOVIE_URL);
            byte[] data = downloadUrl(TEST_MOVIE_URL);
            System.out.println("Downloaded " + data.length + " bytes");

            // Parse and load the movie
            DirectorFile dirFile = DirectorFile.readBytes(data, fileName, basePath);
            player.loadFromDirectorFile(dirFile);

            // Preload external casts
            player.movie.castManager.loadFromDir(dirFile, player.netManager, player.bitmapManager, player.dirCache);
            player.movie.castManager.preloadCasts(
                com.dirplayer.player.CastManager.CastPreloadReason.AfterFrameOne,
                player.netManager, player.bitmapManager, player.dirCache);

            Movie movie = player.getMovie();
            System.out.println("Movie loaded: " + movie.rect.width() + "x" + movie.rect.height() +
                ", " + movie.score.totalFrames + " frames, " + movie.frameRate + " fps");

            // Initialize sprites for frame 1
            player.beginAllSprites();

            // Set up event dispatcher callbacks (simplified for testing)
            setupEventDispatcher(player);

            // Dispatch startMovie
            try {
                player.eventDispatcher.invokeGlobalEvent("startMovie", new java.util.ArrayList<>());
            } catch (Exception e) {
                System.err.println("startMovie error: " + e.getMessage());
            }

            // Dispatch beginSprite
            try {
                player.eventDispatcher.dispatchBeginSpriteEvent("beginSprite", new java.util.ArrayList<>());
            } catch (Exception e) {
                System.err.println("beginSprite error: " + e.getMessage());
            }

            // Print debug info about sprites
            printSpriteDebug(player);

            // Capture initial frame screenshot
            captureScreenshot(player, "frame_0_initial.png");

            // Start playback
            player.play();

            // Run a few frames and capture screenshots
            for (int i = 1; i <= 10; i++) {
                try {
                    player.tick();
                } catch (Exception e) {
                    System.err.println("Tick " + i + " error: " + e.getMessage());
                    e.printStackTrace();
                }

                // Capture every few frames
                if (i == 1 || i == 5 || i == 10) {
                    captureScreenshot(player, "frame_" + i + ".png");
                }

                // Print current frame
                System.out.println("Frame " + i + ": current=" + movie.currentFrame);
            }

            System.out.println("\nScreenshots saved to: " + outputPath.toAbsolutePath());
            System.out.println("Done!");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setupEventDispatcher(DirPlayer player) {
        // Provide player access to EventDispatcher
        player.eventDispatcher.setPlayerSupplier(() -> player);

        // Handler invoker - calls script handlers via DirPlayer
        player.eventDispatcher.setHandlerInvoker(invocation -> {
            try {
                com.dirplayer.player.script.ScriptInstanceRef receiver = invocation.instanceRef;
                com.dirplayer.player.ScopeResult result = player.callScriptHandlerWithResult(
                    receiver,
                    invocation.handlerRef.scriptRef,
                    invocation.handlerRef.handlerName,
                    invocation.args
                );
                return result.passed ?
                    com.dirplayer.player.events.EventResult.passed() :
                    com.dirplayer.player.events.EventResult.withResult(result.returnValue);
            } catch (com.dirplayer.player.ScriptError e) {
                if (e.getMessage() != null && e.getMessage().contains("Handler not found")) {
                    return com.dirplayer.player.events.EventResult.passed();
                }
                System.err.println("Script error: " + e.getMessage());
                return com.dirplayer.player.events.EventResult.passed();
            }
        });

        // Datum handler invoker
        player.eventDispatcher.setDatumHandlerInvoker(invocation -> {
            try {
                int result = player.callDatumHandler(
                    invocation.receiverRef,
                    invocation.handlerName,
                    invocation.args
                );
                return com.dirplayer.player.events.EventResult.withResult(result);
            } catch (com.dirplayer.player.ScriptError e) {
                return com.dirplayer.player.events.EventResult.passed();
            }
        });

        // Error handler
        player.eventDispatcher.setErrorHandler(err -> {
            System.err.println("Event error: " + err.getMessage());
        });
    }

    private static void captureScreenshot(DirPlayer player, String filename) {
        try {
            Movie movie = player.getMovie();
            int width = movie.rect.width();
            int height = movie.rect.height();

            // Create bitmap for rendering
            Bitmap stageBitmap = new Bitmap(width, height, 32, 32, 8,
                com.dirplayer.player.bitmap.PaletteRef.ofBuiltIn(
                    com.dirplayer.player.bitmap.BuiltInPalette.SystemWin));
            stageBitmap.useAlpha = true;

            // Clear with background color
            stageBitmap.clear(player.bgColor);

            // Render stage to bitmap
            Renderer.renderStageToBitmap(player, stageBitmap, null);

            // Convert to BufferedImage
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            byte[] bitmapData = stageBitmap.data;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int srcIdx = (y * width + x) * 4;
                    if (srcIdx + 3 < bitmapData.length) {
                        int r = bitmapData[srcIdx] & 0xFF;
                        int g = bitmapData[srcIdx + 1] & 0xFF;
                        int b = bitmapData[srcIdx + 2] & 0xFF;
                        int a = bitmapData[srcIdx + 3] & 0xFF;
                        image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                    }
                }
            }

            // Save to file
            File outputFile = new File(OUTPUT_DIR, filename);
            ImageIO.write(image, "PNG", outputFile);
            System.out.println("Screenshot saved: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Screenshot error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static byte[] readFileBytes(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        }
    }

    private static byte[] downloadUrl(String urlString) throws Exception {
        java.net.URL url = new java.net.URL(urlString);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        try (java.io.InputStream is = conn.getInputStream();
             java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    private static void printSpriteDebug(DirPlayer player) {
        System.out.println("\n=== Sprite Debug Info ===");
        System.out.println("Current frame: " + player.getMovie().currentFrame);
        System.out.println("Score channels: " + player.getMovie().score.channels.size());
        System.out.println("Sprite spans: " + player.getMovie().score.spriteSpans.size());

        // Print casts info
        System.out.println("\nCasts loaded:");
        for (com.dirplayer.player.CastLib castLib : player.getMovie().castManager.casts) {
            if (castLib != null) {
                int scriptCount = castLib.scripts != null ? castLib.scripts.size() : 0;
                int memberCount = castLib.members != null ? castLib.members.size() : 0;
                System.out.println("  Cast " + castLib.number + " (" + castLib.name + "): " +
                    memberCount + " members, " + scriptCount + " scripts");

                // Show script members if any
                if (scriptCount > 0) {
                    int shown = 0;
                    for (var entry : castLib.scripts.entrySet()) {
                        if (shown >= 3) {
                            System.out.println("    ... and " + (scriptCount - shown) + " more scripts");
                            break;
                        }
                        com.dirplayer.player.script.Script script = entry.getValue();
                        if (script != null && script.chunk != null) {
                            System.out.println("    Script " + entry.getKey() + ": " +
                                (script.name != null ? script.name : "(unnamed)") +
                                ", handlers=" + (script.chunk.handlers != null ? script.chunk.handlers.size() : 0));
                        }
                        shown++;
                    }
                }
            }
        }

        // Check for movie scripts
        System.out.println("\nMovie scripts:");
        for (var script : player.getMovie().castManager.getMovieScripts()) {
            System.out.println("  " + (script.name != null ? script.name : "(unnamed)"));
            if (script.chunk != null && script.chunk.handlers != null) {
                for (var handler : script.chunk.handlers) {
                    System.out.println("    - handler ID " + handler.nameId);
                }
            }
        }

        // Print first 10 sprite spans
        System.out.println("\nSprite spans (first 10):");
        int count = 0;
        for (com.dirplayer.player.score.ScoreSpriteSpan span : player.getMovie().score.spriteSpans) {
            if (count >= 10) break;
            System.out.println("  Span: channel=" + span.channelNumber +
                ", startFrame=" + span.startFrame +
                ", endFrame=" + span.endFrame +
                ", scripts=" + (span.scripts != null ? span.scripts.size() : 0));
            count++;
        }

        // Print active sprites
        System.out.println("\nActive sprites (entered):");
        count = 0;
        for (com.dirplayer.player.score.SpriteChannel channel : player.getMovie().score.channels) {
            com.dirplayer.player.Sprite sprite = channel.sprite;
            if (sprite != null && sprite.entered) {
                System.out.println("  Channel " + channel.number + ": member=" + sprite.memberRef +
                    ", loc=" + sprite.locH + "," + sprite.locV +
                    ", size=" + sprite.width + "x" + sprite.height +
                    ", visible=" + sprite.visible +
                    ", ink=" + sprite.ink);
                count++;
            }
        }
        if (count == 0) {
            System.out.println("  (none)");
        }

        // Check channel initialization data
        System.out.println("\nChannel init data (first 10):");
        count = 0;
        for (com.dirplayer.director.chunks.ScoreFrameData.FrameChannelEntry entry :
             player.getMovie().score.channelInitializationData) {
            if (count >= 10) break;
            int channelNum = com.dirplayer.player.score.KeyframeUtils.getChannelNumberFromIndex(entry.channelIndex);
            System.out.println("  Frame " + (entry.frameIndex + 1) + ", Channel " + channelNum +
                ": castMember=" + entry.data.castMember +
                ", castLib=" + entry.data.castLib +
                ", pos=" + entry.data.posX + "," + entry.data.posY +
                ", size=" + entry.data.width + "x" + entry.data.height);
            count++;
        }

        System.out.println("========================\n");
    }
}
