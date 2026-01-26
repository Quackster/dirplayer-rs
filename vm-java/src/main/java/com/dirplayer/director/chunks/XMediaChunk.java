package com.dirplayer.director.chunks;

import com.dirplayer.io.BinaryReader;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended media chunk (XMED) - contains extended media data like PFR fonts.
 * Port of Rust XMediaChunk struct.
 */
public class XMediaChunk {
    private static final Logger logger = LoggerFactory.getLogger(XMediaChunk.class);

    public byte[] rawData;

    public XMediaChunk() {
        this.rawData = new byte[0];
    }

    public static XMediaChunk fromReader(BinaryReader reader) {
        ByteOrder originalEndian = reader.getEndian();
        reader.setEndian(ByteOrder.BIG_ENDIAN);

        XMediaChunk chunk = new XMediaChunk();
        chunk.rawData = reader.readBytes(reader.getLength() - reader.getPos());

        reader.setEndian(originalEndian);

        logger.debug("XMED raw_data ({} bytes)", chunk.rawData.length);

        return chunk;
    }

    public boolean isPfrFont() {
        if (rawData.length < 100) {
            return false;
        }

        // Check for "PFR1" magic
        if (rawData.length >= 4
            && rawData[0] == 'P' && rawData[1] == 'F' && rawData[2] == 'R' && rawData[3] == '1') {
            logger.debug("Found PFR1 magic header");
            return true;
        }

        // Reject styled text XMedia chunks
        if (rawData.length >= 4
            && rawData[0] == 'F' && rawData[1] == 'F' && rawData[2] == 'F' && rawData[3] == 'F') {
            logger.debug("This is styled text data (FFFF header), not a font");
            return false;
        }

        return false;
    }

    public String extractFontName() {
        int i = 0;
        while (i < rawData.length - 20) {
            if (Character.isLetter((char) (rawData[i] & 0xFF))) {
                StringBuilder name = new StringBuilder();
                int j = i;

                while (j < rawData.length && rawData[j] != 0) {
                    char c = (char) (rawData[j] & 0xFF);
                    if (Character.isLetterOrDigit(c) || c == ' ' || c == '*' || c == '_') {
                        name.append(c);
                        j++;
                    } else {
                        break;
                    }
                }

                if (name.length() > 3) {
                    String nameStr = name.toString();
                    if (nameStr.contains("FFF") || nameStr.contains("Reaction")) {
                        return nameStr;
                    }
                }
            }
            i++;
        }
        return null;
    }

    public PfrFont parsePfrFont() {
        if (!isPfrFont()) {
            return null;
        }

        logger.debug("Parsing PFR1 font format...");

        String fontName = extractFontName();
        if (fontName == null) {
            fontName = "Unknown_PFR_Font";
        }
        logger.debug("Font name: '{}'", fontName);

        // Character dimensions
        int charWidth = rawData.length > 0x56 ? rawData[0x56] & 0xFF : 8;
        int charHeight = rawData.length > 0x58 ? rawData[0x58] & 0xFF : 8;

        if (charWidth == 0 || charWidth > 32) charWidth = 8;
        if (charHeight == 0 || charHeight > 32) charHeight = 8;

        logger.debug("Char dimensions: {}x{} pixels", charWidth, charHeight);

        int gridColumns = 16;
        int gridRows = 8;

        int bytesPerRow = (charWidth + 7) / 8;
        int bytesPerGlyph = bytesPerRow * charHeight;
        int totalGlyphs = gridColumns * gridRows;
        int expectedBitmapBytes = bytesPerGlyph * totalGlyphs;

        logger.debug("Expected: {} bytes/glyph, {} total bytes needed", bytesPerGlyph, expectedBitmapBytes);

        // Try candidate offsets
        int[] candidateOffsets = { 0x200, 0x400, 0x800, 0xC00, rawData.length - expectedBitmapBytes };

        byte[] glyphData = null;
        int foundOffset = 0;

        for (int offset : candidateOffsets) {
            if (offset >= 0 && offset + expectedBitmapBytes <= rawData.length) {
                byte[] candidate = Arrays.copyOfRange(rawData, offset, offset + expectedBitmapBytes);

                if (looksLikeBitmapData(candidate, bytesPerGlyph)) {
                    logger.debug("Found bitmap data at offset 0x{}", Integer.toHexString(offset));
                    glyphData = candidate;
                    foundOffset = offset;
                    break;
                }
            }
        }

        if (glyphData == null) {
            logger.debug("No bitmap at standard offsets, trying brute-force scan...");

            int bestOffset = 0;
            float bestScore = 0;

            int scanOffset = 0x100;
            while (scanOffset + expectedBitmapBytes <= rawData.length) {
                byte[] candidate = Arrays.copyOfRange(rawData, scanOffset, scanOffset + expectedBitmapBytes);

                int sampleSize = Math.min(candidate.length, 256);
                int lowBytes = 0;
                for (int j = 0; j < sampleSize; j++) {
                    if ((candidate[j] & 0xFF) < 0x80) lowBytes++;
                }
                float score = (float) lowBytes / sampleSize;

                if (score > bestScore) {
                    bestScore = score;
                    bestOffset = scanOffset;
                }

                scanOffset += 8;
            }

            if (bestScore > 0.5f) {
                logger.debug("Brute-force found candidate at offset 0x{} (score: {:.1}%)",
                    Integer.toHexString(bestOffset), bestScore * 100);
                glyphData = Arrays.copyOfRange(rawData, bestOffset, bestOffset + expectedBitmapBytes);
                foundOffset = bestOffset;
            }
        }

        if (glyphData == null) {
            logger.warn("Could not find or render PFR font. System font will be used as fallback.");
            return null;
        }

        logger.debug("Extracted {} bytes of bitmap data from offset 0x{}",
            glyphData.length, Integer.toHexString(foundOffset));

        PfrFont font = new PfrFont();
        font.fontName = fontName;
        font.charWidth = charWidth;
        font.charHeight = charHeight;
        font.glyphData = glyphData;
        font.gridColumns = gridColumns;
        font.gridRows = gridRows;

        return font;
    }

    private boolean looksLikeBitmapData(byte[] data, int bytesPerGlyph) {
        if (data.length < bytesPerGlyph * 80) {
            return false;
        }

        int sampleSize = Math.min(data.length, 512);
        int veryLow = 0;
        int low = 0;

        for (int i = 0; i < sampleSize; i++) {
            int b = data[i] & 0xFF;
            if (b < 0x40) veryLow++;
            if (b < 0x80) low++;
        }

        float veryLowRatio = (float) veryLow / sampleSize;
        float lowRatio = (float) low / sampleSize;

        if (veryLowRatio < 0.4f || lowRatio < 0.6f) {
            return false;
        }

        // Check not all same value
        byte first = data[0];
        for (int i = 1; i < sampleSize; i++) {
            if (data[i] != first) {
                return true;
            }
        }
        return false;
    }

    public static class PfrFont {
        public String fontName;
        public int charWidth;
        public int charHeight;
        public byte[] glyphData;
        public int gridColumns;
        public int gridRows;
    }
}
