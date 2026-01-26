/**
 * Event dispatching system for Director player.
 *
 * <p>This package handles Director event dispatching including:
 * <ul>
 *   <li>Event types (mouseDown, mouseUp, keyDown, enterFrame, exitFrame, etc.)</li>
 *   <li>Event propagation to sprites and scripts</li>
 *   <li>Behavior script event handling</li>
 *   <li>Frame events (prepareFrame, enterFrame, exitFrame, stepFrame)</li>
 * </ul>
 *
 * <p>Port of Rust vm-rust/src/player/events.rs module.
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.dirplayer.player.events.EventDispatcher} - Main event dispatching logic</li>
 *   <li>{@link com.dirplayer.player.events.EventType} - Enumeration of Director event types</li>
 *   <li>{@link com.dirplayer.player.events.PlayerVMEvent} - Event queue message types</li>
 *   <li>{@link com.dirplayer.player.events.ScoreRef} - Reference to stage or filmloop score</li>
 *   <li>{@link com.dirplayer.player.events.HandlerRef} - Reference to a script handler</li>
 *   <li>{@link com.dirplayer.player.events.EventResult} - Result of event dispatch</li>
 * </ul>
 *
 * <h2>Event Flow</h2>
 * <p>Events in Director follow this propagation order:
 * <ol>
 *   <li>Behavior scripts on sprites (in channel order)</li>
 *   <li>Frame script (channel 0)</li>
 *   <li>Movie scripts</li>
 * </ol>
 * <p>Events can be stopped from propagating by not calling "pass" in a handler.
 *
 * @see com.dirplayer.player.events.EventDispatcher
 * @see com.dirplayer.player.events.EventType
 */
package com.dirplayer.player.events;
