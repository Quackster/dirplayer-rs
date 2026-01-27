package com.dirplayer.swing;

import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Movie;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.rendering.Renderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Custom canvas component for rendering Director movies.
 * Handles rendering, scaling, and input events.
 */
public class SwingCanvas extends JPanel implements MouseListener, MouseMotionListener, KeyListener {
    private static final long serialVersionUID = 1L;

    private final SwingPlayer swingPlayer;

    // Rendering
    private BufferedImage renderBuffer;
    private Bitmap stageBitmap;
    private int movieWidth = 640;
    private int movieHeight = 480;
    private double scale = 1.0;
    private boolean fitToWindow = false;

    // Debug rendering
    private boolean showDebugInfo = false;
    private Integer debugSelectedChannel = null;
    private int currentFrame = 0;
    private long lastRenderTime = 0;
    private double fps = 0;

    // Input state
    private Point lastMousePos = new Point(0, 0);

    public SwingCanvas(SwingPlayer swingPlayer) {
        this.swingPlayer = swingPlayer;

        setBackground(Color.BLACK);
        setFocusable(true);
        setPreferredSize(new Dimension(movieWidth, movieHeight));

        // Initialize render buffer
        createRenderBuffer(movieWidth, movieHeight);

        // Register input listeners
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
    }

    private void createRenderBuffer(int width, int height) {
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;

        renderBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        stageBitmap = new Bitmap(width, height, 32, 32, 8,
            com.dirplayer.player.bitmap.PaletteRef.ofBuiltIn(
                com.dirplayer.player.bitmap.BuiltInPalette.SystemWin));
        stageBitmap.useAlpha = true;
    }

    public void setMovieSize(int width, int height) {
        this.movieWidth = width;
        this.movieHeight = height;
        createRenderBuffer(width, height);
        updatePreferredSize();
        revalidate();
    }

    public void setScale(double scale) {
        this.scale = scale;
        this.fitToWindow = false;
        updatePreferredSize();
        revalidate();
    }

    public void setFitToWindow(boolean fit) {
        this.fitToWindow = fit;
        revalidate();
        repaint();
    }

    private void updatePreferredSize() {
        if (!fitToWindow) {
            int w = (int) (movieWidth * scale);
            int h = (int) (movieHeight * scale);
            setPreferredSize(new Dimension(w, h));
        }
    }

    public void render(DirPlayer player, Integer debugChannel, boolean showDebug) {
        this.debugSelectedChannel = debugChannel;
        this.showDebugInfo = showDebug;

        if (player == null) {
            repaint();
            return;
        }

        try {
            // Get current frame
            Movie movie = player.getMovie();
            if (movie != null) {
                currentFrame = movie.currentFrame;
            }

            // Clear the bitmap
            stageBitmap.clear(player.bgColor);

            // Render stage to bitmap
            Renderer.renderStageToBitmap(player, stageBitmap, debugChannel);

            // Copy bitmap data to BufferedImage
            copyBitmapToImage();

            // Calculate FPS
            long now = System.currentTimeMillis();
            if (lastRenderTime > 0) {
                long elapsed = now - lastRenderTime;
                if (elapsed > 0) {
                    fps = 1000.0 / elapsed;
                }
            }
            lastRenderTime = now;

            repaint();

        } catch (Exception e) {
            System.err.println("Render error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void copyBitmapToImage() {
        if (stageBitmap == null || renderBuffer == null) return;

        int[] imageData = ((DataBufferInt) renderBuffer.getRaster().getDataBuffer()).getData();
        byte[] bitmapData = stageBitmap.data;

        int width = Math.min(stageBitmap.getWidth(), renderBuffer.getWidth());
        int height = Math.min(stageBitmap.getHeight(), renderBuffer.getHeight());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int srcIdx = (y * stageBitmap.getWidth() + x) * 4;
                int dstIdx = y * renderBuffer.getWidth() + x;

                if (srcIdx + 3 < bitmapData.length) {
                    int r = bitmapData[srcIdx] & 0xFF;
                    int g = bitmapData[srcIdx + 1] & 0xFF;
                    int b = bitmapData[srcIdx + 2] & 0xFF;
                    int a = bitmapData[srcIdx + 3] & 0xFF;

                    imageData[dstIdx] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // Set rendering hints for quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Calculate drawing rectangle
        Rectangle destRect = calculateDestRect();

        // Fill background
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw the rendered image
        if (renderBuffer != null) {
            g2d.drawImage(renderBuffer,
                destRect.x, destRect.y, destRect.width, destRect.height,
                null);

            // Draw border around movie area
            g2d.setColor(Color.GRAY);
            g2d.drawRect(destRect.x - 1, destRect.y - 1,
                destRect.width + 1, destRect.height + 1);
        }

        // Draw debug info overlay
        if (showDebugInfo) {
            drawDebugInfo(g2d, destRect);
        }
    }

    private Rectangle calculateDestRect() {
        if (fitToWindow) {
            // Calculate scale to fit while maintaining aspect ratio
            double scaleX = (double) getWidth() / movieWidth;
            double scaleY = (double) getHeight() / movieHeight;
            double fitScale = Math.min(scaleX, scaleY);

            int w = (int) (movieWidth * fitScale);
            int h = (int) (movieHeight * fitScale);
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;

            return new Rectangle(x, y, w, h);
        } else {
            int w = (int) (movieWidth * scale);
            int h = (int) (movieHeight * scale);
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;

            return new Rectangle(x, y, w, h);
        }
    }

    private void drawDebugInfo(Graphics2D g, Rectangle destRect) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(destRect.x + 5, destRect.y + 5, 150, 80);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        int y = destRect.y + 20;
        g.drawString(String.format("Frame: %d", currentFrame), destRect.x + 10, y);
        y += 15;
        g.drawString(String.format("FPS: %.1f", fps), destRect.x + 10, y);
        y += 15;
        g.drawString(String.format("Size: %dx%d", movieWidth, movieHeight), destRect.x + 10, y);
        y += 15;
        g.drawString(String.format("Mouse: %d,%d", lastMousePos.x, lastMousePos.y), destRect.x + 10, y);
        y += 15;
        if (debugSelectedChannel != null) {
            g.drawString(String.format("Channel: %d", debugSelectedChannel), destRect.x + 10, y);
        }
    }

    // ==================== Coordinate Conversion ====================

    private Point screenToMovie(int screenX, int screenY) {
        Rectangle destRect = calculateDestRect();

        double movieX = (screenX - destRect.x) * movieWidth / (double) destRect.width;
        double movieY = (screenY - destRect.y) * movieHeight / (double) destRect.height;

        return new Point(
            Math.max(0, Math.min(movieWidth - 1, (int) movieX)),
            Math.max(0, Math.min(movieHeight - 1, (int) movieY))
        );
    }

    // ==================== Mouse Events ====================

    @Override
    public void mouseClicked(MouseEvent e) {
        requestFocusInWindow();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        Point moviePos = screenToMovie(e.getX(), e.getY());
        lastMousePos = moviePos;
        swingPlayer.handleMousePressed(moviePos.x, moviePos.y, e.getButton());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Point moviePos = screenToMovie(e.getX(), e.getY());
        lastMousePos = moviePos;
        swingPlayer.handleMouseReleased(moviePos.x, moviePos.y, e.getButton());
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point moviePos = screenToMovie(e.getX(), e.getY());
        lastMousePos = moviePos;
        swingPlayer.handleMouseMoved(moviePos.x, moviePos.y);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point moviePos = screenToMovie(e.getX(), e.getY());
        lastMousePos = moviePos;
        swingPlayer.handleMouseMoved(moviePos.x, moviePos.y);
    }

    // ==================== Keyboard Events ====================

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        swingPlayer.handleKeyPressed(e.getKeyCode(), e.getKeyChar());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        swingPlayer.handleKeyReleased(e.getKeyCode(), e.getKeyChar());
    }
}
