/**
 * Director property types for keyframe animation.
 *
 * This package contains property implementations that correspond to
 * animatable sprite properties in Director's Score. Each property
 * implements the DirectorProperty interface for generic keyframe
 * collection and animation.
 *
 * Port of Rust property structs from score_keyframes.rs.
 *
 * Property types:
 * - Position: Combined (x, y) position for path tweens
 * - Size: Combined (width, height) for size tweens
 * - ForeColor: Foreground color (palette or RGB)
 * - BackColor: Background color (palette or RGB)
 * - Rotation: Rotation angle in degrees
 * - Skew: Skew angle in degrees
 * - Blend: Blend percentage (0-100)
 * - LocH: Horizontal position only
 * - LocV: Vertical position only
 * - Width: Width only
 * - Height: Height only
 */
package com.dirplayer.player.score.properties;
