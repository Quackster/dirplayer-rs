/**
 * Rendering system for Director movies.
 *
 * This package contains the rendering infrastructure for displaying Director
 * movie content. The rendering system is designed to be platform-agnostic
 * and can work with different canvas implementations (browser via TeaVM,
 * desktop, etc.).
 *
 * <h2>Key Components</h2>
 *
 * <h3>Core Renderer Classes</h3>
 * <ul>
 *   <li>{@link com.dirplayer.player.rendering.PlayerCanvasRenderer} - Main renderer
 *       that manages canvas contexts and coordinates frame rendering</li>
 *   <li>{@link com.dirplayer.player.rendering.StageRenderer} - Renders the main stage
 *       and score sprites to bitmaps</li>
 *   <li>{@link com.dirplayer.player.rendering.FilmLoopRenderer} - Handles rendering
 *       of film loop cast members with path interpolation</li>
 *   <li>{@link com.dirplayer.player.rendering.PreviewRenderer} - Renders preview
 *       images of cast members for the editor UI</li>
 * </ul>
 *
 * <h3>Platform Abstraction</h3>
 * <ul>
 *   <li>{@link com.dirplayer.player.rendering.CanvasContext} - Interface for
 *       platform-specific canvas rendering operations</li>
 * </ul>
 *
 * <h3>Utilities and Constants</h3>
 * <ul>
 *   <li>{@link com.dirplayer.player.rendering.RenderingUtils} - Helper methods for
 *       color resolution, sprite rect calculation, font loading, etc.</li>
 *   <li>{@link com.dirplayer.player.rendering.InkEffect} - Constants for Director
 *       ink effects (copy, matte, blend, etc.)</li>
 * </ul>
 *
 * <h2>Rendering Pipeline</h2>
 *
 * The rendering pipeline follows these steps:
 * <ol>
 *   <li>Clear the destination bitmap with the stage background color</li>
 *   <li>Get sorted sprite channels by z-order for the current frame</li>
 *   <li>For each visible sprite:
 *     <ul>
 *       <li>Get the sprite's cast member</li>
 *       <li>Calculate the sprite's bounding rectangle</li>
 *       <li>Apply ink effects and blending</li>
 *       <li>Copy pixels from source bitmap to destination</li>
 *     </ul>
 *   </li>
 *   <li>Draw debug overlays if requested</li>
 *   <li>Draw cursor at mouse position</li>
 * </ol>
 *
 * <h2>Film Loop Rendering</h2>
 *
 * Film loops are rendered recursively using their channel initialization data.
 * Key considerations:
 * <ul>
 *   <li>Film loops use keyframe interpolation for sprite positions</li>
 *   <li>Internal sprites inherit the parent sprite's ink settings</li>
 *   <li>Coordinates are translated relative to the film loop's initial_rect</li>
 *   <li>Film loops are rendered to a temporary bitmap then composited</li>
 * </ul>
 *
 * <h2>TeaVM Compatibility</h2>
 *
 * The rendering system is designed to work with TeaVM for browser deployment.
 * Platform-specific code (like HTML Canvas access) should implement the
 * {@link com.dirplayer.player.rendering.CanvasContext} interface.
 *
 * @see com.dirplayer.player.bitmap.Bitmap
 * @see com.dirplayer.rendering.CopyPixelsParams
 */
package com.dirplayer.player.rendering;
