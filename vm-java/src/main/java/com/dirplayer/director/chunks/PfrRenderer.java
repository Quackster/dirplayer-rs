package com.dirplayer.director.chunks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.dirplayer.SimpleLogger;


/**
 * PFR Vector Renderer - Complete Implementation
 * Renders PFR (Portable Font Resource) vector glyphs to bitmaps.
 *
 * PFR bytecode format (inspired by PostScript Type 1):
 * - Stack-based VM with coordinates and drawing commands
 * - Coordinates are typically small signed values (fit in i8 or i16)
 * - Commands include MOVETO, LINETO, curve operations, and FILL
 *
 * Port of Rust pfr_renderer.rs.
 */
public class PfrRenderer {
    private static final SimpleLogger logger = SimpleLogger.getLogger(PfrRenderer.class);

    private int width;
    private int height;
    private List<Short> stack;
    private Point currentPos;
    private List<List<Point>> subpaths;
    private List<Point> currentPath;

    /**
     * A 2D point with float coordinates.
     */
    private static class Point {
        float x;
        float y;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        Point copy() {
            return new Point(this.x, this.y);
        }
    }

    /**
     * Creates a new PFR renderer with the specified dimensions.
     */
    public PfrRenderer(int width, int height) {
        this.width = width;
        this.height = height;
        this.stack = new ArrayList<>();
        this.currentPos = new Point(0.0f, 0.0f);
        this.subpaths = new ArrayList<>();
        this.currentPath = new ArrayList<>();
    }

    /**
     * Main rendering function - renders a single glyph from bytecode.
     *
     * @param data       The PFR bytecode data for the glyph
     * @param width      The width of the output bitmap
     * @param height     The height of the output bitmap
     * @return           The rendered bitmap as a byte array (1-bit per pixel, MSB first)
     */
    public static byte[] renderGlyph(byte[] data, int width, int height) {
        PfrRenderer vm = new PfrRenderer(width, height);
        vm.execute(data);
        return vm.rasterize();
    }

    /**
     * Push a value onto the stack.
     */
    private void push(short value) {
        stack.add(value);
    }

    /**
     * Pop a value from the stack.
     *
     * @return The popped value, or null if stack is empty
     */
    private Short pop() {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.remove(stack.size() - 1);
    }

    /**
     * Pop a coordinate pair (x, y) from the stack.
     *
     * @return A float array [x, y], or null if not enough values on stack
     */
    private float[] popCoord() {
        Short y = pop();
        if (y == null) {
            return null;
        }
        Short x = pop();
        if (x == null) {
            return null;
        }
        return new float[] { (float) x, (float) y };
    }

    /**
     * Execute PFR bytecode to build the path.
     */
    private void execute(byte[] data) {
        int pc = 0;

        while (pc < data.length) {
            int opcode = data[pc] & 0xFF;
            pc++;

            if (opcode == 0x00) {
                // Literal 0
                push((short) 0);
            } else if (opcode >= 0x01 && opcode <= 0x6F) {
                // Small positive literals
                push((short) opcode);
            } else if (opcode >= 0x70 && opcode <= 0x7F) {
                // Negative literals
                push((short) (opcode - 0x80));
            } else if (opcode >= 0x80 && opcode <= 0x8F) {
                // 16-bit literals (big-endian signed)
                if (pc < data.length) {
                    int high = opcode & 0x0F;
                    int low = data[pc] & 0xFF;
                    int value = (high << 8) | low;
                    // Sign extend
                    if (value >= 0x800) {
                        value -= 0x1000;
                    }
                    push((short) value);
                    pc++;
                }
            } else {
                // Path operators - check after literals since some overlap
                switch (opcode) {
                    case 0x01: // rmoveto - relative move (alternate encoding)
                    case 0x05: {
                        float[] coord = popCoord();
                        if (coord != null) {
                            currentPos.x += coord[0];
                            currentPos.y += coord[1];

                            if (!currentPath.isEmpty()) {
                                subpaths.add(new ArrayList<>(currentPath));
                                currentPath.clear();
                            }
                            currentPath.add(currentPos.copy());
                        }
                        break;
                    }

                    case 0x02: // rlineto - relative line
                    case 0x06: {
                        float[] coord = popCoord();
                        if (coord != null) {
                            currentPos.x += coord[0];
                            currentPos.y += coord[1];
                            currentPath.add(currentPos.copy());
                        }
                        break;
                    }

                    case 0x07: { // hmoveto - horizontal move
                        Short dx = pop();
                        if (dx != null) {
                            currentPos.x += (float) dx;

                            if (!currentPath.isEmpty()) {
                                subpaths.add(new ArrayList<>(currentPath));
                                currentPath.clear();
                            }
                            currentPath.add(currentPos.copy());
                        }
                        break;
                    }

                    case 0x08: { // vmoveto - vertical move
                        Short dy = pop();
                        if (dy != null) {
                            currentPos.y += (float) dy;

                            if (!currentPath.isEmpty()) {
                                subpaths.add(new ArrayList<>(currentPath));
                                currentPath.clear();
                            }
                            currentPath.add(currentPos.copy());
                        }
                        break;
                    }

                    case 0x0D: // hlineto - horizontal line
                    case 0x0E: {
                        Short dx = pop();
                        if (dx != null) {
                            currentPos.x += (float) dx;
                            currentPath.add(currentPos.copy());
                        }
                        break;
                    }

                    case 0x0F: { // vlineto - vertical line
                        Short dy = pop();
                        if (dy != null) {
                            currentPos.y += (float) dy;
                            currentPath.add(currentPos.copy());
                        }
                        break;
                    }

                    case 0x10: { // closepath + fill
                        if (!currentPath.isEmpty()) {
                            subpaths.add(new ArrayList<>(currentPath));
                            currentPath.clear();
                        }
                        // End of glyph - exit loop
                        pc = data.length;
                        break;
                    }

                    default: {
                        // Check for fill commands (0xE0-0xFF)
                        if (opcode >= 0xE0 && opcode <= 0xFF) {
                            if (!currentPath.isEmpty()) {
                                subpaths.add(new ArrayList<>(currentPath));
                                currentPath.clear();
                            }
                            // End of glyph
                            pc = data.length;
                        }
                        // Unknown opcode - ignore
                        break;
                    }
                }
            }
        }

        // Add any remaining path
        if (!currentPath.isEmpty()) {
            subpaths.add(new ArrayList<>(currentPath));
        }
    }

    /**
     * Rasterize the paths to a 1-bit bitmap using scanline algorithm.
     */
    private byte[] rasterize() {
        int bytesPerRow = (width + 7) / 8;
        byte[] bitmap = new byte[bytesPerRow * height];

        // Fill paths using scanline algorithm
        for (List<Point> path : subpaths) {
            if (path.size() < 2) {
                continue;
            }

            // Find bounding box
            float minY = path.get(0).y;
            float maxY = path.get(0).y;

            for (Point pt : path) {
                minY = Math.min(minY, pt.y);
                maxY = Math.max(maxY, pt.y);
            }

            // Rasterize each scanline
            for (int y = (int) Math.floor(minY); y <= (int) Math.ceil(maxY); y++) {
                if (y < 0 || y >= height) {
                    continue;
                }

                List<Float> intersections = new ArrayList<>();
                float yF = y + 0.5f;

                // Find edge intersections
                for (int i = 0; i < path.size(); i++) {
                    Point p1 = path.get(i);
                    Point p2 = path.get((i + 1) % path.size());

                    if ((p1.y <= yF && p2.y > yF) || (p2.y <= yF && p1.y > yF)) {
                        float t = (yF - p1.y) / (p2.y - p1.y);
                        float x = p1.x + t * (p2.x - p1.x);
                        intersections.add(x);
                    }
                }

                Collections.sort(intersections);

                // Fill between pairs
                for (int i = 0; i + 1 < intersections.size(); i += 2) {
                    int x1 = (int) Math.max(0, Math.min(width, intersections.get(i)));
                    int x2 = (int) Math.max(0, Math.min(width, intersections.get(i + 1)));

                    for (int x = x1; x < x2; x++) {
                        int byteIdx = y * bytesPerRow + x / 8;
                        int bitIdx = 7 - (x % 8);
                        if (byteIdx < bitmap.length) {
                            bitmap[byteIdx] |= (1 << bitIdx);
                        }
                    }
                }
            }
        }

        return bitmap;
    }

    /**
     * Bresenham's line algorithm - draws a line between two points.
     * Note: This is kept for potential future use but currently the scanline
     * fill algorithm is used for rasterization.
     */
    @SuppressWarnings("unused")
    private void drawLine(byte[] bitmap, int bytesPerRow, Point p0, Point p1) {
        int x0 = Math.round(p0.x);
        int y0 = Math.round(p0.y);
        int x1 = Math.round(p1.x);
        int y1 = Math.round(p1.y);

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            setPixel(bitmap, bytesPerRow, x0, y0);

            if (x0 == x1 && y0 == y1) {
                break;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /**
     * Set a single pixel in the bitmap.
     */
    private void setPixel(byte[] bitmap, int bytesPerRow, int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }

        int byteIdx = y * bytesPerRow + x / 8;
        int bitIdx = 7 - (x % 8);

        if (byteIdx < bitmap.length) {
            bitmap[byteIdx] |= (1 << bitIdx);
        }
    }

    /**
     * Find glyph boundaries in PFR bytecode.
     * Glyphs are terminated by FILL commands (0x10, 0xE0-0xFF).
     *
     * @param pfrData The PFR bytecode data
     * @return List of boundary offsets
     */
    public static List<Integer> findGlyphBoundaries(byte[] pfrData) {
        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(0);
        int i = 0;

        while (i < pfrData.length) {
            int cmd = pfrData[i] & 0xFF;

            // FILL commands (0x10, 0xE0-0xFF) mark glyph ends
            if (cmd == 0x10 || cmd >= 0xE0) {
                if (i + 1 < pfrData.length) {
                    boundaries.add(i + 1);
                }
                i++;
            } else if (cmd <= 0x7F) {
                // Small literal
                i++;
            } else if (cmd >= 0x80 && cmd <= 0x8F) {
                // 16-bit literal
                i += 2;
            } else {
                i++;
            }
        }

        return boundaries;
    }

    /**
     * Represents a glyph boundary as a start/end pair.
     */
    public static class GlyphBoundary {
        public int start;
        public int end;

        public GlyphBoundary(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * Analyze bytecode to find proper glyph boundaries.
     *
     * @param data The PFR bytecode data
     * @return List of glyph boundaries (start, end pairs)
     */
    public static List<GlyphBoundary> analyzePfrBytecode(byte[] data) {
        List<GlyphBoundary> boundaries = new ArrayList<>();
        int i = 0;
        int glyphStart = 0;

        while (i < data.length) {
            int cmd = data[i] & 0xFF;

            // Check for FILL/END commands
            if (cmd == 0x10 || cmd >= 0xE0) {
                // Found end of glyph
                boundaries.add(new GlyphBoundary(glyphStart, i + 1));
                i++;
                glyphStart = i;
            } else if (cmd <= 0x7F) {
                // Literal
                i++;
            } else if (cmd >= 0x80 && cmd <= 0x8F) {
                // 16-bit literal
                i += 2;
            } else {
                // Other command
                i++;
            }
        }

        // Handle last glyph if no terminator
        if (glyphStart < data.length) {
            boundaries.add(new GlyphBoundary(glyphStart, data.length));
        }

        return boundaries;
    }

    /**
     * Main rendering function with detailed logging.
     * Renders multiple glyphs from PFR font data.
     *
     * @param pfrData     The complete PFR bytecode data
     * @param charWidth   The width of each character cell
     * @param charHeight  The height of each character cell
     * @param glyphCount  The number of glyphs to render
     * @return            The complete bitmap data for all glyphs
     */
    public static byte[] renderPfrFont(byte[] pfrData, int charWidth, int charHeight, int glyphCount) {
        logger.debug("PFR VM Renderer: Interpreting bytecode...");

        // Try to find glyph boundaries by looking for fill commands
        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(0);
        int i = 0;

        while (i < pfrData.length) {
            int byteVal = pfrData[i] & 0xFF;

            // Fill commands end glyphs
            if (byteVal == 0x10 || byteVal >= 0xE0) {
                if (i + 1 < pfrData.length && boundaries.size() < glyphCount) {
                    boundaries.add(i + 1);
                }
            }

            // Skip multi-byte instructions
            if (byteVal >= 0x80 && byteVal <= 0x8F) {
                i += 2;
            } else {
                i++;
            }
        }

        logger.debug("  Found {} glyph boundaries", boundaries.size());

        int bytesPerGlyph = ((charWidth + 7) / 8) * charHeight;
        byte[] output = new byte[bytesPerGlyph * glyphCount];

        // Render each glyph
        int glyphsToRender = Math.min(glyphCount, boundaries.size());
        for (int glyphIdx = 0; glyphIdx < glyphsToRender; glyphIdx++) {
            int start = boundaries.get(glyphIdx);
            int end;
            if (glyphIdx + 1 < boundaries.size()) {
                end = boundaries.get(glyphIdx + 1);
            } else {
                end = pfrData.length;
            }

            if (start >= end || start >= pfrData.length) {
                continue;
            }

            // Extract bytecode for this glyph
            int bytecodeLen = end - start;
            byte[] bytecode = new byte[bytecodeLen];
            System.arraycopy(pfrData, start, bytecode, 0, bytecodeLen);

            byte[] glyphBitmap = renderGlyph(bytecode, charWidth, charHeight);

            int offset = glyphIdx * bytesPerGlyph;
            int copyLen = Math.min(glyphBitmap.length, bytesPerGlyph);

            if (offset + copyLen <= output.length) {
                System.arraycopy(glyphBitmap, 0, output, offset, copyLen);
            }
        }

        // Log samples
        logger.debug("  Sample glyphs:");
        int[] sampleIndices = { 32, 65, 72 };
        for (int idx : sampleIndices) {
            if (idx < glyphCount) {
                int offset = idx * bytesPerGlyph;
                if (offset + bytesPerGlyph <= output.length) {
                    int pixels = 0;
                    for (int b = offset; b < offset + bytesPerGlyph; b++) {
                        pixels += Integer.bitCount(output[b] & 0xFF);
                    }

                    String charName;
                    switch (idx) {
                        case 32:
                            charName = "space";
                            break;
                        case 65:
                            charName = "A";
                            break;
                        case 72:
                            charName = "H";
                            break;
                        default:
                            charName = "?";
                            break;
                    }

                    logger.debug("    Glyph #{} ('{}'):  {} pixels", idx, charName, pixels);

                    if (pixels > 0) {
                        int rowsToShow = Math.min(charHeight, 8);
                        for (int row = 0; row < rowsToShow; row++) {
                            if (row < bytesPerGlyph) {
                                int byteVal = output[offset + row] & 0xFF;
                                StringBuilder line = new StringBuilder("      ");
                                for (int bit = 0; bit < 8; bit++) {
                                    if ((byteVal & (1 << (7 - bit))) != 0) {
                                        line.append('#');
                                    } else {
                                        line.append('.');
                                    }
                                }
                                logger.debug("{}", line);
                            }
                        }
                    }
                }
            }
        }

        logger.debug("Rendered {} glyphs", glyphCount);
        return output;
    }
}
