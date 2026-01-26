package com.dirplayer.player.handlers;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.Movie;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.ScriptScope;
import com.dirplayer.player.Sprite;
import com.dirplayer.player.score.Score;
import com.dirplayer.director.chunks.FrameLabelsChunk.FrameLabel;
import com.dirplayer.SimpleLogger;


import java.util.List;
import java.util.Map;

/**
 * Movie-related handler functions.
 * Port of Rust MovieHandlers struct from movie.rs.
 *
 * This class provides all movie-related Lingo built-in handlers including:
 * - Frame navigation (go, puppetTempo)
 * - Sprite manipulation (sprite, puppetSprite, sendSprite, sendAllSprites)
 * - Member/script references (member, script)
 * - External parameters (externalParamCount, externalParamName, externalParamValue)
 * - Stage/playback control (updateStage, halt, rollover)
 * - Sound control (puppetSound)
 * - Event control (stopEvent, pass)
 * - Preferences (getPref, setPref)
 * - Miscellaneous (goToNetPage)
 */
public class MovieHandlers {
    private static final SimpleLogger logger = SimpleLogger.getLogger(MovieHandlers.class);

    /**
     * Invalid cast member reference constant.
     */
    public static final CastMemberRef INVALID_CAST_MEMBER_REF = new CastMemberRef(0, 0);

    /**
     * puppetTempo - Set the puppet tempo for movie playback.
     *
     * Lingo syntax: puppetTempo tempo
     *
     * @param player The DirPlayer instance
     * @param args List containing the tempo value
     * @return 0 (void)
     * @throws ScriptError if tempo value cannot be read
     */
    public static int puppetTempo(DirPlayer player, List<Integer> args) throws ScriptError {
        int tempo = player.getDatum(args.get(0)).intValue();
        player.movie.puppetTempo = tempo;
        return 0; // Void
    }

    /**
     * script - Get a reference to a script by name or number.
     *
     * Lingo syntax: script(identifier)
     *
     * @param player The DirPlayer instance
     * @param args List containing the script identifier (name, number, or member ref)
     * @return DatumRef to the script reference
     * @throws ScriptError if script not found or invalid identifier
     */
    public static int script(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum identifier = player.getDatum(args.get(0));
        String formattedId = player.formatDatum(identifier);

        CastMemberRef memberRef;
        DatumType identType = identifier.getType();

        switch (identType) {
            case String:
                String scriptName = identifier.stringValue();
                memberRef = player.movie.castManager.findMemberRefByName(scriptName);
                break;
            case Int:
                int scriptNum = identifier.intValue();
                memberRef = player.movie.castManager.findMemberRefByNumber(scriptNum);
                break;
            case CastMemberRef:
                memberRef = identifier.toMemberRef();
                break;
            default:
                throw new ScriptError("Invalid identifier for script: " + formattedId);
        }

        if (memberRef == null) {
            throw new ScriptError("Script not found " + formattedId);
        }

        // Verify script exists
        Object script = player.movie.castManager.getScriptByRef(memberRef);
        if (script == null) {
            throw new ScriptError("Script not found " + formattedId);
        }

        return player.allocDatum(Datum.ofScriptRef(memberRef));
    }

    /**
     * member - Get a reference to a cast member.
     *
     * Lingo syntax: member(nameOrNum) or member(nameOrNum, castNameOrNum)
     *
     * @param player The DirPlayer instance
     * @param args List containing member identifier and optional cast identifier
     * @return DatumRef to the cast member reference
     * @throws ScriptError if too many arguments or member lookup fails
     */
    public static int member(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() > 2) {
            throw new ScriptError("Too many arguments for member");
        }

        int memberNameOrNumRef = args.get(0);
        Datum memberNameOrNum = player.getDatum(memberNameOrNumRef);

        // If already a CastMember, return it directly
        if (memberNameOrNum.isCastMemberRef()) {
            return memberNameOrNumRef;
        }

        Datum castNameOrNum = null;
        if (args.size() > 1) {
            castNameOrNum = player.getDatum(args.get(1));
        }

        CastMemberRef memberRef = player.movie.castManager.findMemberRefByIdentifiers(
            memberNameOrNum, castNameOrNum
        );

        if (memberRef != null) {
            return player.allocDatum(Datum.ofCastMember(memberRef));
        } else {
            return player.allocDatum(Datum.ofCastMember(INVALID_CAST_MEMBER_REF));
        }
    }

    /**
     * go - Navigate to a frame.
     *
     * Lingo syntax: go frame, go "labelName", go #next, go #previous, go #loop
     *
     * This is a simplified synchronous version of the Rust async implementation.
     * In the full implementation, this would handle frame change events (endSprite,
     * beginSprite, prepareFrame, enterFrame) and actorList stepFrame dispatching.
     *
     * @param player The DirPlayer instance
     * @param args List containing the destination frame (int, string label, or symbol)
     * @return 0 (void)
     * @throws ScriptError if frame destination is invalid
     */
    public static int go(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(args.get(0));
        DatumType datumType = datum.getType();
        int enterFrame = player.movie.currentFrame;

        logger.debug("Function go() called with datum: {}", player.formatDatum(datum));

        Integer dest = null;

        switch (datumType) {
            case Int:
                dest = datum.intValue();
                break;

            case String:
                String label = datum.stringValue();
                dest = findFrameByLabel(player, label);
                break;

            case Symbol:
                String symbol = datum.symbolValue();
                switch (symbol.toLowerCase()) {
                    case "next":
                        dest = player.movie.currentFrame + 1;
                        break;
                    case "previous":
                        dest = Math.max(1, player.movie.currentFrame - 1);
                        break;
                    case "loop":
                        dest = player.movie.currentFrame;
                        break;
                    default:
                        // Try as frame label
                        dest = findFrameByLabel(player, symbol);
                        break;
                }
                break;

            default:
                break;
        }

        if (dest == null) {
            throw new ScriptError("Unsupported or invalid frame label passed to go()");
        }

        int destinationFrame = dest;

        // Set next frame if different from current
        if (player.nextFrame == null || destinationFrame != player.movie.currentFrame) {
            player.nextFrame = destinationFrame;

            if (destinationFrame != enterFrame) {
                // Set direction for frame change
                if (enterFrame < destinationFrame) {
                    player.goDirection = 2; // forwards
                } else if (enterFrame > destinationFrame) {
                    player.goDirection = 1; // backwards
                }

                // In the full implementation, this would:
                // 1. End all sprites (dispatch endSprite events)
                // 2. Advance the frame
                // 3. Begin new sprites (dispatch beginSprite events)
                // 4. Dispatch stepFrame to actorList
                // 5. Dispatch prepareFrame and enterFrame events
                // For now, we simply set the frame change flags
            }
        }

        player.hasFrameChangedInGo = true;

        return 0; // Void
    }

    /**
     * Find a frame number by its label name.
     */
    private static Integer findFrameByLabel(DirPlayer player, String label) {
        for (FrameLabel fl : player.movie.score.frameLabels) {
            if (fl.label.equalsIgnoreCase(label)) {
                return fl.frameNum;
            }
        }
        return null;
    }

    /**
     * puppetSprite - Set a sprite's puppet state.
     *
     * Lingo syntax: puppetSprite spriteNum, TRUE/FALSE
     *
     * @param player The DirPlayer instance
     * @param args List containing sprite number and puppet state
     * @return 0 (void)
     * @throws ScriptError if sprite number or state cannot be read
     */
    public static int puppetSprite(DirPlayer player, List<Integer> args) throws ScriptError {
        int spriteNumber = player.getDatum(args.get(0)).intValue();
        boolean isPuppet = player.getDatum(args.get(1)).intValue() == 1;
        Sprite sprite = player.movie.score.getSpriteMut((short) spriteNumber);
        if (sprite != null) {
            sprite.puppet = isPuppet;
        }
        return 0; // Void
    }

    /**
     * sprite - Get a reference to a sprite by channel number.
     *
     * Lingo syntax: sprite(spriteNum)
     *
     * @param player The DirPlayer instance
     * @param args List containing the sprite number
     * @return DatumRef to the sprite reference
     * @throws ScriptError if sprite number cannot be read
     */
    public static int sprite(DirPlayer player, List<Integer> args) throws ScriptError {
        int spriteNumber = player.getDatum(args.get(0)).intValue();
        return player.allocDatum(Datum.ofSpriteRef(spriteNumber));
    }

    /**
     * sendSprite - Send a message to a specific sprite.
     *
     * Lingo syntax: sendSprite(spriteNum, #message, args...)
     *
     * This is a simplified synchronous version. The full implementation would
     * dispatch the event to sprite behaviors and fall back to movie scripts.
     *
     * @param player The DirPlayer instance
     * @param args List containing sprite number, message symbol, and optional arguments
     * @return DatumRef containing 1 if handled by sprite, 0 otherwise
     * @throws ScriptError if sprite number or message cannot be read
     */
    public static int sendSprite(DirPlayer player, List<Integer> args) throws ScriptError {
        int spriteNum = player.getDatum(args.get(0)).intValue();
        String message = player.getDatum(args.get(1)).symbolValue();

        Sprite sprite = player.movie.score.getSprite((short) spriteNum);
        if (sprite == null) {
            throw new ScriptError("sendSprite: sprite " + spriteNum + " not found");
        }

        // Get remaining args (args[2:])
        List<Integer> remainingArgs = args.subList(2, args.size());
        List<Integer> receivers = sprite.scriptInstanceList;

        boolean handledBySprite = false;

        // In full implementation, this would invoke the event to each behavior instance
        // and fall back to static event handlers if not handled
        // For now, we return whether there are any potential handlers
        if (!receivers.isEmpty()) {
            handledBySprite = true;
            logger.debug("sendSprite: {} handlers for sprite {} message {}",
                receivers.size(), spriteNum, message);
        }

        return player.allocDatum(Datum.ofInt(handledBySprite ? 1 : 0));
    }

    /**
     * sendAllSprites - Send a message to all sprites.
     *
     * Lingo syntax: sendAllSprites(#message, args...)
     *
     * This is a simplified synchronous version. The full implementation would
     * dispatch the event to all sprite behaviors and filmloop sprites.
     *
     * @param player The DirPlayer instance
     * @param args List containing message symbol and optional arguments
     * @return DatumRef containing 1 if handled by any sprite, 0 otherwise
     * @throws ScriptError if message cannot be read
     */
    public static int sendAllSprites(DirPlayer player, List<Integer> args) throws ScriptError {
        // Check for re-entrant call
        if (player.isInSendAllSprites) {
            logger.warn("Blocking re-entrant sendAllSprites call to prevent infinite recursion");
            return 0; // Void
        }

        player.isInSendAllSprites = true;

        try {
            String message = player.getDatum(args.get(0)).symbolValue();

            // Get remaining args (args[1:])
            List<Integer> remainingArgs = args.subList(1, args.size());

            // Collect receivers from stage score
            List<Integer> receivers = player.movie.score.getActiveScriptInstanceList();

            boolean handledBySprite = !receivers.isEmpty();

            // In full implementation, this would also collect filmloop receivers
            // and invoke the event to each behavior instance

            logger.debug("sendAllSprites: {} handlers for message {}", receivers.size(), message);

            return player.allocDatum(Datum.ofInt(handledBySprite ? 1 : 0));
        } finally {
            player.isInSendAllSprites = false;
        }
    }

    /**
     * externalParamCount - Get the count of external parameters.
     *
     * Lingo syntax: externalParamCount()
     *
     * @param player The DirPlayer instance
     * @param args Empty list
     * @return DatumRef containing the count
     * @throws ScriptError never
     */
    public static int externalParamCount(DirPlayer player, List<Integer> args) throws ScriptError {
        int count = player.externalParams.size();
        return player.allocDatum(Datum.ofInt(count));
    }

    /**
     * externalParamName - Get the name of an external parameter.
     *
     * Lingo syntax: externalParamName(indexOrName)
     *
     * @param player The DirPlayer instance
     * @param args List containing the index (1-based) or name to look up
     * @return DatumRef containing the parameter name or void if not found
     * @throws ScriptError never
     */
    public static int externalParamName(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(args.get(0));

        // Case 1: argument is a string (lookup by name, case-insensitive)
        if (datum.isString()) {
            String key = datum.stringValue();
            for (String paramKey : player.externalParams.keySet()) {
                if (paramKey.equalsIgnoreCase(key)) {
                    return player.allocDatum(Datum.ofString(key));
                }
            }
            return player.allocDatum(Datum.VOID);
        }

        // Case 2: argument is an integer (index)
        if (datum.isInt()) {
            int index = datum.intValue();
            if (index > 0 && index <= player.externalParams.size()) {
                int i = 1;
                for (String paramKey : player.externalParams.keySet()) {
                    if (i == index) {
                        return player.allocDatum(Datum.ofString(paramKey));
                    }
                    i++;
                }
            }
            return player.allocDatum(Datum.VOID);
        }

        // Invalid argument type
        logger.info("externalParamName(): invalid argument type, returning Void");
        return player.allocDatum(Datum.VOID);
    }

    /**
     * externalParamValue - Get the value of an external parameter.
     *
     * Lingo syntax: externalParamValue(indexOrName)
     *
     * @param player The DirPlayer instance
     * @param args List containing the index (1-based) or name to look up
     * @return DatumRef containing the parameter value or void if not found
     * @throws ScriptError never
     */
    public static int externalParamValue(DirPlayer player, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(args.get(0));

        // Case 1: argument is a string (lookup by name)
        if (datum.isString()) {
            String key = datum.stringValue();
            for (Map.Entry<String, String> entry : player.externalParams.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    return player.allocDatum(Datum.ofString(entry.getValue()));
                }
            }
            return player.allocDatum(Datum.VOID);
        }

        // Case 2: argument is an integer (index)
        if (datum.isInt()) {
            int index = datum.intValue();
            if (index > 0 && index <= player.externalParams.size()) {
                int i = 1;
                for (String value : player.externalParams.values()) {
                    if (i == index) {
                        return player.allocDatum(Datum.ofString(value));
                    }
                    i++;
                }
            }
            return player.allocDatum(Datum.VOID);
        }

        // Invalid type
        logger.info("externalParamValue(): invalid argument type, returning Void");
        return player.allocDatum(Datum.VOID);
    }

    /**
     * stopEvent - Stop propagation of the current event.
     *
     * Lingo syntax: stopEvent
     *
     * @param player The DirPlayer instance
     * @param args Empty list
     * @return 0 (void)
     * @throws ScriptError never
     */
    public static int stopEvent(DirPlayer player, List<Integer> args) throws ScriptError {
        // Stop event propagation by marking the current scope as not passed
        int scopeRef = player.currentScopeRef();
        if (scopeRef >= 0 && scopeRef < player.scopes.size()) {
            player.scopes.get(scopeRef).passed = false;
        }
        return 0; // Void
    }

    /**
     * getPref - Get a preference value.
     *
     * Lingo syntax: getPref(prefName)
     *
     * @param player The DirPlayer instance
     * @param args List containing the preference name
     * @return DatumRef containing empty string (Lingo code handles fallback)
     * @throws ScriptError never
     */
    public static int getPref(DirPlayer player, List<Integer> args) throws ScriptError {
        // Return empty string - Lingo code handles the fallback
        return player.allocDatum(Datum.ofString(""));
    }

    /**
     * setPref - Set a preference value.
     *
     * Lingo syntax: setPref prefName, value
     *
     * @param player The DirPlayer instance
     * @param args List containing the preference name and value
     * @return 0 (void)
     * @throws ScriptError never
     */
    public static int setPref(DirPlayer player, List<Integer> args) throws ScriptError {
        // No-op in this implementation
        return 0; // Void
    }

    /**
     * goToNetPage - Navigate to a URL in the browser.
     *
     * Lingo syntax: goToNetPage(url)
     *
     * @param player The DirPlayer instance
     * @param args List containing the URL
     * @return 0 (void)
     * @throws ScriptError never
     */
    public static int goToNetPage(DirPlayer player, List<Integer> args) throws ScriptError {
        // No-op in this implementation
        return 0; // Void
    }

    /**
     * pass - Mark the current handler as passed (event should continue propagation).
     *
     * Lingo syntax: pass
     *
     * @param player The DirPlayer instance
     * @param args Empty list
     * @return 0 (void)
     * @throws ScriptError never
     */
    public static int pass(DirPlayer player, List<Integer> args) throws ScriptError {
        int scopeRef = player.currentScopeRef();
        if (scopeRef >= 0 && scopeRef < player.scopes.size()) {
            ScriptScope scope = player.scopes.get(scopeRef);
            scope.passed = true;
        }
        return 0; // Void
    }

    /**
     * updateStage - Force a stage redraw.
     *
     * Lingo syntax: updateStage
     *
     * This is a simplified synchronous version. The full implementation would
     * check if yielding is safe and perform a synchronous render.
     *
     * @param player The DirPlayer instance
     * @param args Empty list
     * @return 0 (void)
     * @throws ScriptError never
     */
    public static int updateStage(DirPlayer player, List<Integer> args) throws ScriptError {
        logger.debug("updateStage: handlerStackDepth = {}, isInFrameUpdate = {}, " +
                "inFrameScript = {}, inEnterFrame = {}, inPrepareFrame = {}, inEventDispatch = {}",
            player.handlerStackDepth,
            player.isInFrameUpdate,
            player.inFrameScript,
            player.inEnterFrame,
            player.inPrepareFrame,
            player.inEventDispatch
        );

        // In full implementation, this would:
        // 1. Check if yielding is safe
        // 2. Perform synchronous render
        // 3. Yield control briefly
        // For now, we just mark that an update was requested

        return 0; // Void
    }

    /**
     * rollover - Get the sprite number under the mouse cursor.
     *
     * Lingo syntax: rollover()
     *
     * @param player The DirPlayer instance
     * @param args Empty list
     * @return DatumRef containing the sprite number (0 if none)
     * @throws ScriptError never
     */
    public static int rollover(DirPlayer player, List<Integer> args) throws ScriptError {
        int spriteNum = getSpriteAtPoint(player, player.mouseLocX, player.mouseLocY, false);
        return player.allocDatum(Datum.ofInt(spriteNum));
    }

    /**
     * Get the topmost sprite at a given point.
     *
     * @param player The DirPlayer instance
     * @param x X coordinate
     * @param y Y coordinate
     * @param includeInvisible Whether to include invisible sprites
     * @return Sprite number or 0 if none found
     */
    private static int getSpriteAtPoint(DirPlayer player, int x, int y, boolean includeInvisible) {
        Score score = player.movie.score;

        // Search from highest to lowest channel (back to front in z-order)
        for (int i = score.channels.size() - 1; i >= 1; i--) {
            Sprite sprite = score.getSprite((short) i);
            if (sprite == null) continue;

            if (!includeInvisible && !sprite.visible) continue;
            if (!sprite.memberRef.isValid()) continue;

            if (sprite.containsPoint(x, y)) {
                return sprite.number;
            }
        }

        return 0;
    }

    /**
     * puppetSound - Play a sound in puppet mode.
     *
     * Lingo syntax: puppetSound channel, member OR puppetSound member (channel 1)
     *
     * @param player The DirPlayer instance
     * @param args List containing optional channel number and member reference
     * @return 0 (void)
     * @throws ScriptError if no arguments provided
     */
    public static int puppetSound(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("puppetSound requires at least 1 argument");
        }

        int channelNum;
        int memberRef;

        // If only one argument, use channel 1 by default
        if (args.size() == 1) {
            channelNum = 1;
            memberRef = args.get(0);
        } else {
            channelNum = player.getDatum(args.get(0)).intValue();
            memberRef = args.get(1);
        }

        // In full implementation, this would call player.puppetSound(channelNum, memberRef)
        // For now, we just log the request
        logger.debug("puppetSound: channel {}, memberRef {}", channelNum, memberRef);

        return 0; // Void
    }

    /**
     * halt - Stop movie playback.
     *
     * Lingo syntax: halt
     *
     * @param player The DirPlayer instance
     * @param args Empty list
     * @return 0 (void)
     * @throws ScriptError never
     */
    public static int halt(DirPlayer player, List<Integer> args) throws ScriptError {
        // Stop movie playback
        player.movie.currentFrame = 1;
        player.isPlaying = false;
        return 0; // Void
    }

    /**
     * executeFrameUpdate - Execute a frame update cycle.
     *
     * This is an internal method that handles the frame update sequence:
     * 1. Apply tween modifiers
     * 2. Dispatch stepFrame to actorList
     * 3. Dispatch prepareFrame to timeouts and behaviors
     * 4. Dispatch enterFrame to behaviors
     *
     * This is a simplified synchronous version of the Rust async implementation.
     *
     * @param player The DirPlayer instance
     * @throws ScriptError if frame update encounters an error
     */
    public static void executeFrameUpdate(DirPlayer player) throws ScriptError {
        // Prevent re-entrant calls
        if (player.isInFrameUpdate || player.hasPlayerFrameChanged) {
            return; // Exit early if already updating
        }

        player.isInFrameUpdate = true;

        try {
            // Apply tweening
            player.movie.score.applyTweenModifiers(player.movie.currentFrame);

            // In full implementation, this would:
            // 1. Dispatch stepFrame to actorList items
            // 2. Dispatch prepareFrame to timeout targets
            // 3. Dispatch prepareFrame to all behaviors
            // 4. Dispatch enterFrame to all behaviors

            player.inPrepareFrame = true;
            // dispatchSystemEventToTimeouts("prepareFrame", [])
            // dispatchEventToAllBehaviors("prepareFrame", [])
            player.inPrepareFrame = false;

            player.inEnterFrame = true;
            // dispatchEventToAllBehaviors("enterFrame", [])
            player.inEnterFrame = false;

        } finally {
            player.isInFrameUpdate = false;
        }
    }
}
