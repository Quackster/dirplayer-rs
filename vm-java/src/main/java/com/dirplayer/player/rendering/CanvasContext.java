package com.dirplayer.player.rendering;

/**
 * Platform abstraction for canvas rendering.
 * This interface allows the rendering system to work with different
 * platforms (browser via TeaVM, desktop, etc.).
 *
 * Port of the web_sys canvas rendering APIs from Rust.
 */
public interface CanvasContext {

    /**
     * Set the canvas size.
     *
     * @param width Canvas width in pixels
     * @param height Canvas height in pixels
     */
    void setSize(int width, int height);

    /**
     * Get the canvas width.
     */
    int getWidth();

    /**
     * Get the canvas height.
     */
    int getHeight();

    /**
     * Put image data to the canvas.
     * The data is expected to be in RGBA format (4 bytes per pixel).
     *
     * @param data RGBA pixel data
     * @param width Image width
     * @param height Image height
     */
    void putImageData(byte[] data, int width, int height);

    /**
     * Put image data to the canvas at a specific position.
     *
     * @param data RGBA pixel data
     * @param width Image width
     * @param height Image height
     * @param x Destination X position
     * @param y Destination Y position
     */
    void putImageData(byte[] data, int width, int height, int x, int y);

    /**
     * Clear the canvas with the specified color.
     *
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     */
    void clear(int r, int g, int b);

    /**
     * Clear a rectangular region.
     *
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     */
    void clearRect(int x, int y, int width, int height);

    /**
     * Set image smoothing enabled state.
     * When disabled, pixels are rendered without interpolation (crisp edges).
     *
     * @param enabled Whether to enable image smoothing
     */
    void setImageSmoothingEnabled(boolean enabled);

    /**
     * Set the fill style for subsequent fill operations.
     *
     * @param color Color string (e.g., "white", "#FFFFFF", "rgb(255,255,255)")
     */
    void setFillStyle(String color);

    /**
     * Fill a rectangle with the current fill style.
     *
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     */
    void fillRect(int x, int y, int width, int height);

    /**
     * Check if the context is valid/initialized.
     */
    boolean isValid();
}
