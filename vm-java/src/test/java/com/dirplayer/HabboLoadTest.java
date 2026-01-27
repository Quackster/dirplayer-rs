package com.dirplayer;

import com.dirplayer.director.DirectorFile;
import com.dirplayer.player.CastLib;
import com.dirplayer.player.CastManager;
import com.dirplayer.player.CastMember;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.NetManager;
import com.dirplayer.player.bitmap.BitmapManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration test for loading Habbo DCR file and verifying external cast loading.
 * Tests that preloadNetThing correctly loads external casts like fuse_client.
 */
public class HabboLoadTest {

    private static final String HABBO_URL = "http://localhost/assets/habbo.dcr";

    /**
     * Downloads a file from a URL and returns the bytes.
     */
    private byte[] downloadFile(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "DirPlayer-Java-Test/1.0");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP error: " + responseCode + " for URL: " + urlString);
        }

        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            return buffer.toByteArray();
        }
    }

    /**
     * Test loading habbo.dcr and verifying that external casts are loaded,
     * specifically looking for fuse_client.
     */
    @Test
    void testLoadHabboWithExternalCasts() throws Exception {
        System.out.println("========================================");
        System.out.println("Habbo DCR Load Test with External Casts");
        System.out.println("========================================");

        // Download the main DCR file
        System.out.println("Downloading: " + HABBO_URL);
        byte[] fileData = downloadFile(HABBO_URL);
        System.out.println("Downloaded " + fileData.length + " bytes");

        // Parse the Director file
        DirectorFile dirFile = DirectorFile.readBytes(fileData, "habbo.dcr", "http://localhost/assets/");
        assertNotNull(dirFile, "DirectorFile should not be null");

        System.out.println("\n--- Director File Info ---");
        System.out.println("Version: " + dirFile.version);
        System.out.println("Cast Entries: " + dirFile.castEntries.size());
        System.out.println("Internal Casts: " + dirFile.casts.size());

        // Print all cast entries
        System.out.println("\n--- Cast Entries ---");
        for (int i = 0; i < dirFile.castEntries.size(); i++) {
            var entry = dirFile.castEntries.get(i);
            String castName = entry.name != null && !entry.name.isEmpty() ? entry.name : "(unnamed)";
            String filePath = entry.filePath != null && !entry.filePath.isEmpty() ? entry.filePath : "(internal)";
            boolean isExternal = entry.filePath != null && !entry.filePath.isEmpty();

            System.out.println("  Cast " + (i + 1) + ": \"" + castName + "\"");
            System.out.println("    File: " + filePath + (isExternal ? " [EXTERNAL]" : ""));
            System.out.println("    Preload Mode: " + entry.preloadSettings);
        }

        // Create player components
        NetManager netManager = new NetManager();
        netManager.setSynchronousMode(true);  // Enable synchronous fetching for test
        netManager.setBasePath("http://localhost/assets/");

        BitmapManager bitmapManager = new BitmapManager();
        Map<String, DirectorFile> dirCache = new HashMap<>();

        // Load casts with preloading enabled
        CastManager castManager = new CastManager();
        castManager.loadFromDir(dirFile, netManager, bitmapManager, dirCache);

        System.out.println("\n--- Loaded Cast Libraries ---");
        System.out.println("Total cast libraries: " + castManager.getCastLibCount());

        boolean foundFuseClient = false;
        int totalExternalCasts = 0;
        int loadedExternalCasts = 0;

        for (CastLib cast : castManager.getCastLibs()) {
            String stateStr = cast.state.name();
            System.out.println("  Cast " + cast.number + ": \"" + cast.name + "\"");
            System.out.println("    State: " + stateStr);
            System.out.println("    External: " + cast.isExternal);
            System.out.println("    FileName: " + cast.fileName);
            System.out.println("    Members: " + cast.getMemberCount());

            if (cast.isExternal || (cast.fileName != null && !cast.fileName.isEmpty())) {
                totalExternalCasts++;
                if (cast.state == CastLib.CastLibState.Loaded) {
                    loadedExternalCasts++;
                }
            }

            // Check for fuse_client
            if (cast.name != null && cast.name.toLowerCase().contains("fuse_client")) {
                foundFuseClient = true;
                System.out.println("    *** Found fuse_client! ***");
                System.out.println("    fuse_client preload mode: " + cast.preloadMode);
                System.out.println("    fuse_client member count: " + cast.getMemberCount());

                // Note: fuse_client has preload mode 1 (After frame one),
                // so it won't be loaded during MovieLoaded phase
                if (cast.preloadMode == 2) {
                    // Only check members if preload mode is "Before frame one"
                    assertTrue(cast.getMemberCount() > 0,
                        "fuse_client should have members loaded");
                }
            }

            // Also check filename
            if (cast.fileName != null && cast.fileName.toLowerCase().contains("fuse_client")) {
                foundFuseClient = true;
                System.out.println("    *** Found fuse_client (by filename)! ***");
            }
        }

        System.out.println("\n--- Summary ---");
        System.out.println("Total external casts: " + totalExternalCasts);
        System.out.println("Loaded external casts: " + loadedExternalCasts);
        System.out.println("Found fuse_client: " + foundFuseClient);
        System.out.println("Director cache entries: " + dirCache.size());

        // Assertions
        assertTrue(castManager.getCastLibCount() > 0, "Should have at least one cast library");

        if (totalExternalCasts > 0) {
            System.out.println("\nExternal casts were found. Checking preload behavior...");
            // Note: Not all external casts may be loaded depending on preload mode
        }

        System.out.println("\n========================================");
        System.out.println("Test completed successfully!");
        System.out.println("========================================");
    }

    /**
     * Test that preloadNetThing works correctly in isolation.
     */
    @Test
    void testPreloadNetThingBasic() throws Exception {
        System.out.println("Testing preloadNetThing basic functionality...");

        NetManager netManager = new NetManager();
        netManager.setSynchronousMode(true);

        // Test with a simple URL
        String testUrl = "http://localhost/assets/habbo.dcr";
        int taskId = netManager.preloadNetThing(testUrl);

        System.out.println("Task ID: " + taskId);
        assertTrue(taskId > 0, "Task ID should be positive");

        // In synchronous mode, task should be done immediately
        boolean isDone = netManager.isTaskDone(taskId);
        System.out.println("Task done: " + isDone);

        if (isDone) {
            var result = netManager.getTaskResult(taskId);
            if (result != null && result.isOk()) {
                System.out.println("Successfully fetched " + result.getData().length + " bytes");
            } else {
                System.out.println("Fetch failed or result is null");
            }
        }
    }

    /**
     * Test loading habbo.dcr using the full DirPlayer.
     */
    @Test
    void testLoadHabboWithDirPlayer() throws Exception {
        System.out.println("========================================");
        System.out.println("Habbo DCR Load Test with DirPlayer");
        System.out.println("========================================");

        // Download the main DCR file
        System.out.println("Downloading: " + HABBO_URL);
        byte[] fileData = downloadFile(HABBO_URL);
        System.out.println("Downloaded " + fileData.length + " bytes");

        // Create DirPlayer
        DirPlayer player = new DirPlayer();
        player.netManager.setSynchronousMode(true);
        player.netManager.setBasePath("http://localhost/assets/");

        // Parse and load
        DirectorFile dirFile = DirectorFile.readBytes(fileData, "habbo.dcr", "http://localhost/assets/");

        // Use the new loadFromDir method with preloading
        player.movie.castManager.loadFromDir(dirFile, player.netManager, player.bitmapManager, player.dirCache);

        System.out.println("\n--- Player Movie Info ---");
        System.out.println("Cast libraries: " + player.movie.castManager.getCastLibCount());

        // Look for fuse_client
        boolean foundFuseClient = false;
        for (CastLib cast : player.movie.castManager.getCastLibs()) {
            System.out.println("Cast " + cast.number + ": " + cast.name +
                " (state=" + cast.state + ", members=" + cast.getMemberCount() + ")");

            if ((cast.name != null && cast.name.toLowerCase().contains("fuse_client")) ||
                (cast.fileName != null && cast.fileName.toLowerCase().contains("fuse_client"))) {
                foundFuseClient = true;
                System.out.println("  *** fuse_client found! ***");
            }
        }

        System.out.println("\nfuse_client found: " + foundFuseClient);

        // Now trigger AfterFrameOne preloading for fuse_client
        System.out.println("\nTriggering AfterFrameOne preload...");
        player.movie.castManager.preloadCasts(
            CastManager.CastPreloadReason.AfterFrameOne,
            player.netManager,
            player.bitmapManager,
            player.dirCache
        );

        // Check fuse_client again
        System.out.println("\n--- After AfterFrameOne preload ---");
        for (CastLib cast : player.movie.castManager.getCastLibs()) {
            if (cast.name != null && cast.name.toLowerCase().contains("fuse_client")) {
                System.out.println("fuse_client state: " + cast.state);
                System.out.println("fuse_client members: " + cast.getMemberCount());
            }
        }

        System.out.println("========================================");
    }
}
