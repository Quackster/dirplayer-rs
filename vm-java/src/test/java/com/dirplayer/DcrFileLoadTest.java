package com.dirplayer;

import com.dirplayer.director.CastDef;
import com.dirplayer.director.DirectorFile;
import com.dirplayer.director.chunks.ConfigChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Integration test that loads a .dcr file from a URL and outputs basic information.
 *
 * This test demonstrates how to:
 * 1. Fetch a Director file from a URL
 * 2. Parse it using DirectorFile
 * 3. Extract and display movie information
 */
public class DcrFileLoadTest {

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
     * Prints basic information about a loaded Director file.
     */
    private void printFileInfo(DirectorFile dirFile, String url) {
        System.out.println("========================================");
        System.out.println("Director File Information");
        System.out.println("========================================");
        System.out.println("URL: " + url);
        System.out.println("File Name: " + dirFile.fileName);
        System.out.println("Director Version: " + dirFile.version);
        System.out.println();

        // Configuration info
        if (dirFile.config != null) {
            ConfigChunk config = dirFile.config;
            System.out.println("--- Movie Configuration ---");
            System.out.println("Director Version (from config): " + config.directorVersion);

            int width = config.movieRight - config.movieLeft;
            int height = config.movieBottom - config.movieTop;
            System.out.println("Stage Size: " + width + " x " + height);
            System.out.println("Stage Rect: (" + config.movieLeft + ", " + config.movieTop +
                             ") to (" + config.movieRight + ", " + config.movieBottom + ")");
            System.out.println("Frame Rate: " + config.frameRate + " fps");
            System.out.println("Bit Depth: " + config.bitDepth);

            if (config.directorVersion >= 700) {
                System.out.println("Stage Color (RGB): rgb(" +
                    config.d7StageColorR + ", " +
                    config.d7StageColorG + ", " +
                    config.d7StageColorB + ")");
            } else {
                System.out.println("Stage Color (Palette Index): " + config.preD7StageColor);
            }

            System.out.println("Cast Member Range: " + config.minMember + " to " + config.maxMember);
            System.out.println();
        }

        // Cast libraries info
        if (dirFile.casts != null && !dirFile.casts.isEmpty()) {
            System.out.println("--- Cast Libraries ---");
            System.out.println("Number of Cast Libraries: " + dirFile.casts.size());

            int totalMembers = 0;
            for (int i = 0; i < dirFile.casts.size(); i++) {
                CastDef cast = dirFile.casts.get(i);
                String castName = cast.name != null && !cast.name.isEmpty() ? cast.name : "(unnamed)";
                int memberCount = cast.members != null ? cast.members.size() : 0;
                totalMembers += memberCount;

                System.out.println("  Cast " + (i + 1) + ": \"" + castName + "\" - " + memberCount + " members");

                // Print member type breakdown
                if (cast.members != null && !cast.members.isEmpty()) {
                    printMemberTypeBreakdown(cast.members);
                }
            }
            System.out.println("Total Cast Members: " + totalMembers);
            System.out.println();
        }

        // Score info
        if (dirFile.score != null) {
            System.out.println("--- Score/Timeline ---");
            if (dirFile.score.header != null) {
                System.out.println("Score Header Present: Yes");
                System.out.println("Score Entry Count: " + dirFile.score.header.entryCount);
            }
            if (dirFile.score.entries != null) {
                System.out.println("Score Entries: " + dirFile.score.entries.size());
            }
            if (dirFile.score.frameData != null && dirFile.score.frameData.header != null) {
                System.out.println("Frame Data Present: Yes");
                System.out.println("Frame Count: " + dirFile.score.frameData.header.frameCount);
                System.out.println("Num Channels: " + dirFile.score.frameData.header.numChannels);
                System.out.println("Sprite Record Size: " + dirFile.score.frameData.header.spriteRecordSize);
                System.out.println("Sprite Channel Entries: " + dirFile.score.frameData.frameChannelData.size());
                System.out.println("Sound Channel Entries: " + dirFile.score.frameData.soundChannelData.size());
                System.out.println("Tempo Channel Entries: " + dirFile.score.frameData.tempoChannelData.size());
            }
            System.out.println();
        }

        // Frame labels
        if (dirFile.frameLabels != null && dirFile.frameLabels.labels != null) {
            System.out.println("--- Frame Labels ---");
            System.out.println("Number of Labels: " + dirFile.frameLabels.labels.size());
            int maxLabelsToShow = Math.min(10, dirFile.frameLabels.labels.size());
            for (int i = 0; i < maxLabelsToShow; i++) {
                var label = dirFile.frameLabels.labels.get(i);
                System.out.println("  Frame " + label.frameNum + ": \"" + label.label + "\"");
            }
            if (dirFile.frameLabels.labels.size() > 10) {
                System.out.println("  ... and " + (dirFile.frameLabels.labels.size() - 10) + " more");
            }
            System.out.println();
        }

        System.out.println("========================================");
    }

    /**
     * Prints a breakdown of member types in a cast library.
     */
    private void printMemberTypeBreakdown(Map<Integer, CastDef.CastMemberDef> members) {
        java.util.Map<String, Integer> typeCounts = new java.util.HashMap<>();

        for (CastDef.CastMemberDef member : members.values()) {
            String typeName = "Unknown";
            if (member.chunk != null && member.chunk.memberType != null) {
                typeName = member.chunk.memberType.name();
            }
            typeCounts.merge(typeName, 1, Integer::sum);
        }

        // Sort by count descending
        typeCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)  // Show top 5 types
            .forEach(entry ->
                System.out.println("    - " + entry.getKey() + ": " + entry.getValue())
            );
    }

    /**
     * Test loading a .dcr file from the Internet Archive's Shockwave collection.
     * This uses a known working Shockwave game.
     */
    @Test
    @Disabled("Requires network access - enable manually to test")
    void testLoadDcrFromInternetArchive() throws Exception {
        // Example: A simple Shockwave file from Internet Archive
        // You can replace this URL with any publicly accessible .dcr file
        String testUrl = "https://archive.org/download/flash_director_shockwave_resources/games/shockwave/simple_test.dcr";

        System.out.println("Downloading DCR file from: " + testUrl);
        byte[] fileData = downloadFile(testUrl);
        System.out.println("Downloaded " + fileData.length + " bytes");

        DirectorFile dirFile = DirectorFile.readBytes(fileData, "simple_test.dcr", testUrl);
        assertNotNull(dirFile, "DirectorFile should not be null");

        printFileInfo(dirFile, testUrl);

        // Basic assertions
        assertTrue(dirFile.version > 0, "Version should be positive");
        assertNotNull(dirFile.config, "Config should not be null");
    }

    /**
     * Test loading a .dcr file from a custom URL.
     * Provide the URL as a system property: -Ddcr.test.url=https://example.com/file.dcr
     */
    @Test
    @Disabled("Requires network access and custom URL - enable manually to test")
    void testLoadDcrFromCustomUrl() throws Exception {
        String testUrl = System.getProperty("dcr.test.url");
        if (testUrl == null || testUrl.isEmpty()) {
            System.out.println("No URL provided. Set -Ddcr.test.url=<url> to test.");
            return;
        }

        System.out.println("Downloading DCR file from: " + testUrl);
        byte[] fileData = downloadFile(testUrl);
        System.out.println("Downloaded " + fileData.length + " bytes");

        String fileName = testUrl.substring(testUrl.lastIndexOf('/') + 1);
        DirectorFile dirFile = DirectorFile.readBytes(fileData, fileName, testUrl);
        assertNotNull(dirFile, "DirectorFile should not be null");

        printFileInfo(dirFile, testUrl);

        // Basic assertions
        assertTrue(dirFile.version > 0, "Version should be positive");
    }

    /**
     * Test loading a local .dcr file from the file system.
     * Provide the path as a system property: -Ddcr.test.path=C:\path\to\file.dcr
     */
    @Test
    @Disabled("Requires local file - enable manually to test")
    void testLoadDcrFromLocalFile() throws Exception {
        String testPath = System.getProperty("dcr.test.path");
        if (testPath == null || testPath.isEmpty()) {
            System.out.println("No path provided. Set -Ddcr.test.path=<path> to test.");
            return;
        }

        java.io.File file = new java.io.File(testPath);
        assertTrue(file.exists(), "File should exist: " + testPath);

        byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());
        System.out.println("Loaded " + fileData.length + " bytes from: " + testPath);

        DirectorFile dirFile = DirectorFile.readBytes(fileData, file.getName(), file.getParent());
        assertNotNull(dirFile, "DirectorFile should not be null");

        printFileInfo(dirFile, testPath);

        // Basic assertions
        assertTrue(dirFile.version > 0, "Version should be positive");
        assertNotNull(dirFile.config, "Config should not be null");
    }

    /**
     * Test with a minimal synthetic Director file header.
     * This tests the parser can at least detect file format.
     */
    @Test
    void testParserDetectsInvalidFile() {
        // Random bytes that are not a valid Director file
        byte[] invalidData = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

        assertThrows(Exception.class, () -> {
            DirectorFile.readBytes(invalidData, "invalid.dcr", "");
        }, "Should throw exception for invalid file");
    }

    /**
     * Integration test using a known public DCR file.
     * This test can be enabled for CI/CD if a reliable test file is available.
     */
    @Test
    @Disabled("Enable with a reliable test URL for CI/CD")
    void testLoadKnownGoodDcrFile() throws Exception {
        // Replace with a reliable, publicly accessible DCR file URL
        String testUrl = "https://example.com/test.dcr";

        byte[] fileData = downloadFile(testUrl);
        DirectorFile dirFile = DirectorFile.readBytes(fileData, "test.dcr", testUrl);

        // Specific assertions for the known test file
        assertNotNull(dirFile);
        assertNotNull(dirFile.config);
        assertTrue(dirFile.version >= 400, "Should be Director 4 or later");

        // Check movie dimensions are reasonable
        int width = dirFile.config.movieRight - dirFile.config.movieLeft;
        int height = dirFile.config.movieBottom - dirFile.config.movieTop;
        assertTrue(width > 0 && width < 10000, "Width should be reasonable");
        assertTrue(height > 0 && height < 10000, "Height should be reasonable");

        printFileInfo(dirFile, testUrl);
    }
}
