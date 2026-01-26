/**
 * Score keyframe animation system for Director player.
 *
 * This package provides the infrastructure for keyframe-based tween animations
 * in Director's Score (timeline). It is a port of the Rust score_keyframes.rs module.
 *
 * <h2>Key Components</h2>
 *
 * <h3>Core Interfaces</h3>
 * <ul>
 *   <li>{@link com.dirplayer.player.score.DirectorProperty} - Generic property extraction/resolution interface</li>
 *   <li>{@link com.dirplayer.player.score.KeyframeTrack} - Interface for keyframe animation tracks</li>
 *   <li>{@link com.dirplayer.player.score.KeyframeData} - Interface for keyframe data access</li>
 * </ul>
 *
 * <h3>Keyframe Types</h3>
 * <ul>
 *   <li>{@link com.dirplayer.player.score.SpriteKeyframe.PathKeyframe} - Position (x, y) keyframe</li>
 *   <li>{@link com.dirplayer.player.score.SpriteKeyframe.SizeKeyframe} - Size (width, height) keyframe</li>
 *   <li>{@link com.dirplayer.player.score.SpriteKeyframe.RotationKeyframe} - Rotation angle keyframe</li>
 *   <li>{@link com.dirplayer.player.score.SpriteKeyframe.SkewKeyframe} - Skew angle keyframe</li>
 *   <li>{@link com.dirplayer.player.score.SpriteKeyframe.BlendKeyframe} - Blend percentage keyframe</li>
 *   <li>{@link com.dirplayer.player.score.SpriteKeyframe.ColorKeyframe} - Color (fore/back) keyframe</li>
 * </ul>
 *
 * <h3>Track Types</h3>
 * <ul>
 *   <li>{@link com.dirplayer.player.score.SpritePathKeyframes} - Path (position) tween track</li>
 *   <li>{@link com.dirplayer.player.score.SpriteSizeKeyframes} - Size tween track</li>
 *   <li>{@link com.dirplayer.player.score.SpriteRotationKeyframes} - Rotation tween track</li>
 *   <li>{@link com.dirplayer.player.score.SpriteSkewKeyframes} - Skew tween track</li>
 *   <li>{@link com.dirplayer.player.score.SpriteBlendKeyframes} - Blend tween track</li>
 *   <li>{@link com.dirplayer.player.score.SpriteForeColorKeyframes} - Foreground color tween track</li>
 *   <li>{@link com.dirplayer.player.score.SpriteBackColorKeyframes} - Background color tween track</li>
 * </ul>
 *
 * <h3>Property Types (in properties subpackage)</h3>
 * <ul>
 *   <li>{@link com.dirplayer.player.score.properties.Position} - Combined position property</li>
 *   <li>{@link com.dirplayer.player.score.properties.Size} - Combined size property</li>
 *   <li>{@link com.dirplayer.player.score.properties.Rotation} - Rotation property</li>
 *   <li>{@link com.dirplayer.player.score.properties.Skew} - Skew property</li>
 *   <li>{@link com.dirplayer.player.score.properties.Blend} - Blend property</li>
 *   <li>{@link com.dirplayer.player.score.properties.ForeColor} - Foreground color property</li>
 *   <li>{@link com.dirplayer.player.score.properties.BackColor} - Background color property</li>
 *   <li>{@link com.dirplayer.player.score.properties.LocH} - Horizontal position property</li>
 *   <li>{@link com.dirplayer.player.score.properties.LocV} - Vertical position property</li>
 *   <li>{@link com.dirplayer.player.score.properties.Width} - Width property</li>
 *   <li>{@link com.dirplayer.player.score.properties.Height} - Height property</li>
 * </ul>
 *
 * <h3>Utilities</h3>
 * <ul>
 *   <li>{@link com.dirplayer.player.score.KeyframeUtils} - Channel conversion, easing, curvature functions</li>
 *   <li>{@link com.dirplayer.player.score.PropertyKeyframeCollector} - Generic keyframe collection</li>
 *   <li>{@link com.dirplayer.player.score.KeyframeBuilderFactory} - Factory for building keyframe caches</li>
 *   <li>{@link com.dirplayer.player.score.ChannelKeyframes} - Combined keyframes for a channel</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build keyframes cache from score data
 * Map<Integer, ChannelKeyframes> cache = KeyframeBuilderFactory.buildAllKeyframesCache(
 *     frameChannelData, intervalsByChannel);
 *
 * // Get keyframes for a specific channel
 * ChannelKeyframes keyframes = cache.get(channelNum);
 *
 * // Check if position tween is active at a frame
 * if (keyframes.path != null && keyframes.path.isActiveAtFrame(frame)) {
 *     int[] delta = keyframes.path.getDeltaAtFrame(frame, baseX, baseY);
 *     sprite.locH = baseX + delta[0];
 *     sprite.locV = baseY + delta[1];
 * }
 * }</pre>
 *
 * <h2>Interpolation</h2>
 * The system supports:
 * <ul>
 *   <li>Linear interpolation (CurvatureType.LINEAR)</li>
 *   <li>Smooth S-curve (CurvatureType.NORMAL)</li>
 *   <li>Extreme S-curve (CurvatureType.EXTREME)</li>
 *   <li>Ease-in/ease-out via applyEasing()</li>
 * </ul>
 *
 * @see com.dirplayer.player.score.Score
 * @see com.dirplayer.director.chunks.ScoreChunk
 */
package com.dirplayer.player.score;
