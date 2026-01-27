package com.dirplayer.player.events;

import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.script.Script;
import com.dirplayer.player.script.ScriptInstance;
import com.dirplayer.player.script.ScriptInstanceRef;
import com.dirplayer.player.score.ScoreBehaviorReference;
import com.dirplayer.SimpleLogger;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Dispatches events to sprites, behaviors, and scripts.
 * Port of Rust events.rs functions.
 */
public class EventDispatcher {
    private static final SimpleLogger logger = SimpleLogger.getLogger(EventDispatcher.class);

    private final BlockingQueue<PlayerVMEvent> eventQueue;
    private volatile boolean running;

    // Callbacks for player access (set by DirPlayer)
    private Supplier<DirPlayer> playerSupplier;
    private Function<HandlerInvocation, EventResult> handlerInvoker;
    private Function<DatumHandlerInvocation, EventResult> datumHandlerInvoker;
    private Consumer<ScriptError> errorHandler;

    public EventDispatcher() {
        this.eventQueue = new LinkedBlockingQueue<>();
        this.running = false;
    }

    /**
     * Set the player supplier for accessing player state.
     */
    public void setPlayerSupplier(Supplier<DirPlayer> playerSupplier) {
        this.playerSupplier = playerSupplier;
    }

    /**
     * Set the handler invoker for calling script handlers.
     */
    public void setHandlerInvoker(Function<HandlerInvocation, EventResult> handlerInvoker) {
        this.handlerInvoker = handlerInvoker;
    }

    /**
     * Set the datum handler invoker for calling handlers on datum objects.
     */
    public void setDatumHandlerInvoker(Function<DatumHandlerInvocation, EventResult> datumHandlerInvoker) {
        this.datumHandlerInvoker = datumHandlerInvoker;
    }

    /**
     * Set the error handler for script errors.
     */
    public void setErrorHandler(Consumer<ScriptError> errorHandler) {
        this.errorHandler = errorHandler;
    }

    // ========== Event Dispatch Functions ==========

    /**
     * Dispatch a global event to the event queue.
     * Port of Rust player_dispatch_global_event.
     */
    public void dispatchGlobalEvent(String handlerName, List<Integer> args) {
        eventQueue.offer(new PlayerVMEvent.Global(handlerName, new ArrayList<>(args)));
    }

    /**
     * Dispatch a callback event to a specific receiver.
     * Port of Rust player_dispatch_callback_event.
     */
    public void dispatchCallbackEvent(int receiverRef, String handlerName, List<Integer> args) {
        eventQueue.offer(new PlayerVMEvent.Callback(receiverRef, handlerName, new ArrayList<>(args)));
    }

    /**
     * Dispatch a targeted event to specific script instances.
     * Port of Rust player_dispatch_targeted_event.
     */
    public void dispatchTargetedEvent(String handlerName, List<Integer> args, List<ScriptInstanceRef> instanceRefs) {
        List<ScriptInstanceRef> refsCopy = instanceRefs != null ? new ArrayList<>(instanceRefs) : null;
        eventQueue.offer(new PlayerVMEvent.Targeted(handlerName, new ArrayList<>(args), refsCopy));
    }

    /**
     * Dispatch an event to a specific sprite's behaviors.
     * Port of Rust player_dispatch_event_to_sprite.
     */
    public void dispatchEventToSprite(String handlerName, List<Integer> args, int spriteNum) {
        DirPlayer player = playerSupplier.get();
        if (player == null) return;

        List<ScriptInstanceRef> instanceRefs = getSpriteScriptInstances(player, spriteNum);
        if (instanceRefs == null || instanceRefs.isEmpty()) {
            return;
        }

        eventQueue.offer(new PlayerVMEvent.Targeted(handlerName, new ArrayList<>(args), instanceRefs));
    }

    // ========== Event Invocation Functions ==========

    /**
     * Invoke an event to specific script instances.
     * Port of Rust player_invoke_event_to_instances.
     */
    public boolean invokeEventToInstances(String handlerName, List<Integer> args,
                                          List<ScriptInstanceRef> instanceRefs) throws ScriptError {
        DirPlayer player = playerSupplier.get();
        if (player == null || instanceRefs == null) {
            return false;
        }

        // Find handlers for each instance
        List<InstanceHandlerPair> handlerPairs = new ArrayList<>();
        for (ScriptInstanceRef instanceRef : instanceRefs) {
            HandlerRef handlerRef = getScriptInstanceHandler(player, handlerName, instanceRef);
            if (handlerRef != null) {
                handlerPairs.add(new InstanceHandlerPair(instanceRef, handlerRef));
            }
        }

        boolean handled = false;
        for (InstanceHandlerPair pair : handlerPairs) {
            try {
                HandlerInvocation invocation = new HandlerInvocation(
                        pair.instanceRef, pair.handlerRef, args);
                EventResult result = handlerInvoker.apply(invocation);

                if (!result.passed) {
                    handled = true;
                    break;
                }
            } catch (Exception e) {
                ScriptError error = new ScriptError("Error in handler '" + handlerName + "': " + e.getMessage(), e);
                logger.error("Error in handler '{}': {}", handlerName, e.getMessage());
                if (errorHandler != null) {
                    errorHandler.accept(error);
                }
                throw error;
            }
        }

        return handled;
    }

    /**
     * Invoke a targeted event to optional script instances, falling back to static scripts.
     * Port of Rust player_invoke_targeted_event.
     */
    public int invokeTargetedEvent(String handlerName, List<Integer> args,
                                   List<ScriptInstanceRef> instanceRefs) throws ScriptError {
        boolean handled = false;
        if (instanceRefs != null && !instanceRefs.isEmpty()) {
            handled = invokeEventToInstances(handlerName, args, instanceRefs);
        }

        if (!handled) {
            invokeStaticEvent(handlerName, args);
        }

        return 0; // DatumRef.Void
    }

    /**
     * Invoke event to frame and movie scripts.
     * Port of Rust player_invoke_frame_and_movie_scripts.
     */
    public int invokeFrameAndMovieScripts(String handlerName, List<Integer> args) throws ScriptError {
        DirPlayer player = playerSupplier.get();
        if (player == null) return 0;

        List<CastMemberRef> activeStaticScripts = getActiveStaticScripts(player);

        for (CastMemberRef scriptMemberRef : activeStaticScripts) {
            if (!hasHandler(player, scriptMemberRef, handlerName)) {
                continue;
            }

            // Check if this is the frame script
            ScriptInstanceRef receiver = getFrameScriptInstance(player, scriptMemberRef);

            try {
                HandlerInvocation invocation = new HandlerInvocation(
                        receiver,
                        new HandlerRef(scriptMemberRef, handlerName),
                        args);
                EventResult result = handlerInvoker.apply(invocation);

                if (!result.passed) {
                    break;
                }
            } catch (Exception e) {
                throw new ScriptError("Error in handler '" + handlerName + "': " + e.getMessage(), e);
            }
        }

        return 0; // DatumRef.Void
    }

    /**
     * Invoke a static event to frame and movie scripts.
     * Port of Rust player_invoke_static_event.
     */
    public boolean invokeStaticEvent(String handlerName, List<Integer> args) throws ScriptError {
        DirPlayer player = playerSupplier.get();
        if (player == null) return false;

        List<CastMemberRef> activeStaticScripts = getActiveStaticScripts(player);

        boolean handled = false;
        for (CastMemberRef scriptMemberRef : activeStaticScripts) {
            if (!hasHandler(player, scriptMemberRef, handlerName)) {
                continue;
            }

            // Check if this is the frame script
            ScriptInstanceRef receiver = getFrameScriptInstance(player, scriptMemberRef);

            try {
                HandlerInvocation invocation = new HandlerInvocation(
                        receiver,
                        new HandlerRef(scriptMemberRef, handlerName),
                        args);
                EventResult result = handlerInvoker.apply(invocation);

                if (!result.passed) {
                    handled = true;
                    break;
                }
            } catch (Exception e) {
                throw new ScriptError("Error in handler '" + handlerName + "': " + e.getMessage(), e);
            }
        }

        return handled;
    }

    /**
     * Invoke a global event to all active scripts.
     * Port of Rust player_invoke_global_event.
     */
    public int invokeGlobalEvent(String handlerName, List<Integer> args) throws ScriptError {
        DirPlayer player = playerSupplier.get();
        if (player == null) return 0;

        // First: behavior scripts on sprites
        // Then: frame behavior script
        // Then: movie scripts
        // If frame is changed during exitFrame, event is no longer propagated

        List<ScriptInstanceRef> activeInstanceScripts = getActiveInstanceScripts(player);

        boolean handled = invokeEventToInstances(handlerName, args, activeInstanceScripts);
        if (handled) {
            return 0; // DatumRef.Void
        }

        invokeStaticEvent(handlerName, args);

        return 0; // DatumRef.Void
    }

    // ========== Begin/End Sprite Events ==========

    /**
     * Dispatch beginSprite event to all eligible sprites.
     * Port of Rust player_dispatch_event_beginsprite.
     */
    public List<SpriteChannelRef> dispatchBeginSpriteEvent(String handlerName, List<Integer> args) throws ScriptError {
        DirPlayer player = playerSupplier.get();
        if (player == null) return new ArrayList<>();

        BeginSpriteData data = collectBeginSpriteData(player);

        if (data.spriteInstances.isEmpty() && data.frameInstances.isEmpty()) {
            return new ArrayList<>();
        }

        // Dispatch to frame behaviors first (channel 0)
        if (!data.frameInstances.isEmpty()) {
            invokeFrameAndMovieScripts(handlerName, args);
        }

        // Dispatch to sprite behaviors (channel > 0)
        for (SpriteInstanceData spriteData : data.spriteInstances) {
            // Set score context for this sprite's behavior
            player.currentScoreContext = toPlayerScoreRef(spriteData.scoreRef);

            List<ScriptInstanceRef> receivers = new ArrayList<>();
            receivers.add(spriteData.instanceRef);

            try {
                invokeTargetedEvent(handlerName, args, receivers);
            } catch (ScriptError err) {
                logger.error("Error in {} for sprite {}: {}", handlerName, spriteData.spriteNumber, err.getMessage());
                if (errorHandler != null) {
                    errorHandler.accept(err);
                }
            }

            // Reset score context to Stage
            player.currentScoreContext = new DirPlayer.ScoreRef();
        }

        return data.allChannels;
    }

    /**
     * Dispatch endSprite event to specified sprites.
     * Port of Rust dispatch_event_endsprite.
     */
    public void dispatchEndSpriteEvent(List<Integer> spriteNums) {
        dispatchEndSpriteEventForScore(ScoreRef.stage(), spriteNums);
    }

    /**
     * Dispatch endSprite event to specified sprites in a specific score context.
     * Port of Rust dispatch_event_endsprite_for_score.
     */
    public void dispatchEndSpriteEventForScore(ScoreRef scoreRef, List<Integer> spriteNums) {
        DirPlayer player = playerSupplier.get();
        if (player == null) return;

        EndSpriteData data = collectEndSpriteData(player, scoreRef, spriteNums);

        // Dispatch to frame behaviors first (channel 0)
        if (!data.frameTuples.isEmpty()) {
            try {
                invokeFrameAndMovieScripts("endSprite", new ArrayList<>());
            } catch (ScriptError err) {
                logger.error("Error in endSprite for frame: {}", err.getMessage());
            }
        }

        // Set score context for this dispatch
        player.currentScoreContext = toPlayerScoreRef(scoreRef);

        // Dispatch to sprite behaviors (channel > 0)
        for (SpriteBehaviorTuple tuple : data.spriteTuples) {
            for (ScriptInstanceRef behavior : tuple.behaviors) {
                List<ScriptInstanceRef> receivers = new ArrayList<>();
                receivers.add(behavior);

                try {
                    invokeEventToInstances("endSprite", new ArrayList<>(), receivers);
                } catch (ScriptError err) {
                    logger.error("Error in endSprite for sprite {}: {}", tuple.spriteNum, err.getMessage());
                    if (errorHandler != null) {
                        errorHandler.accept(err);
                    }
                }
            }
        }

        // Reset score context to Stage
        player.currentScoreContext = new DirPlayer.ScoreRef();
    }

    // ========== Dispatch to All Behaviors ==========

    /**
     * Dispatch an event to all active behavior scripts.
     * Port of Rust dispatch_event_to_all_behaviors.
     */
    public void dispatchEventToAllBehaviors(String handlerName, List<Integer> args) {
        DirPlayer player = playerSupplier.get();
        if (player == null) return;

        // Skip event dispatch if we're initializing behavior properties
        if (player.isInitializingBehaviorProps) {
            logger.warn("Blocking event '{}' during property initialization", handlerName);
            return;
        }

        // Prevent re-entrant event dispatch
        if (player.isDispatchingEvents) {
            logger.warn("Blocking re-entrant event dispatch for '{}'", handlerName);
            return;
        }

        player.isDispatchingEvents = true;

        try {
            BehaviorData behaviorData = collectBehaviorData(player);

            // Dispatch to sprite behaviors first (channel order)
            for (SpriteBehaviorData spriteData : behaviorData.spriteBehaviors) {
                // Set score context for this sprite's behaviors
                player.currentScoreContext = toPlayerScoreRef(spriteData.scoreRef);

                for (ScriptInstanceRef behavior : spriteData.behaviors) {
                    logger.debug("Invoking '{}' on sprite {} behavior", handlerName, spriteData.spriteNumber);

                    List<ScriptInstanceRef> receivers = new ArrayList<>();
                    receivers.add(behavior);

                    try {
                        invokeEventToInstances(handlerName, args, receivers);
                    } catch (ScriptError err) {
                        logger.error("Error in {} for sprite {}: {}", handlerName, spriteData.spriteNumber, err.getMessage());
                        if (errorHandler != null) {
                            errorHandler.accept(err);
                        }
                    }
                }

                // Reset score context to Stage
                player.currentScoreContext = new DirPlayer.ScoreRef();
            }

            // Dispatch event to frame/movie scripts
            try {
                invokeFrameAndMovieScripts(handlerName, args);
            } catch (ScriptError err) {
                if (errorHandler != null) {
                    errorHandler.accept(err);
                }
            }
        } finally {
            // Reset the flag after dispatching
            player.isDispatchingEvents = false;
        }
    }

    // ========== System Events ==========

    /**
     * Dispatch system events to all timeout targets.
     * Port of Rust dispatch_system_event_to_timeouts.
     */
    public void dispatchSystemEventToTimeouts(String handlerName, List<Integer> args) {
        DirPlayer player = playerSupplier.get();
        if (player == null) return;

        // Get all timeout targets that are currently scheduled
        List<Integer> timeoutTargets = new ArrayList<>();
        for (var entry : player.timeoutManager.timeouts.entrySet()) {
            var timeout = entry.getValue();
            if (timeout.isActive) {
                timeoutTargets.add(timeout.target);
            }
        }

        // Dispatch the event to each timeout target
        for (int targetRef : timeoutTargets) {
            try {
                if (datumHandlerInvoker != null) {
                    DatumHandlerInvocation invocation = new DatumHandlerInvocation(targetRef, handlerName, args);
                    datumHandlerInvoker.apply(invocation);
                }
            } catch (Exception e) {
                // HandlerNotFound is expected when a script doesn't have the event handler
                // This is normal Director behavior - just silently skip
                if (!e.getMessage().contains("HandlerNotFound")) {
                    logger.error("Timeout system event {} error: {}", handlerName, e.getMessage());
                }
            }
        }
    }

    // ========== Event Loop ==========

    /**
     * Run the event loop processing queued events.
     * Port of Rust run_event_loop.
     */
    public void runEventLoop() {
        logger.warn("Starting event loop");
        running = true;

        while (running) {
            try {
                PlayerVMEvent event = eventQueue.take();

                DirPlayer player = playerSupplier.get();
                if (player == null || !player.isPlaying) {
                    continue;
                }

                try {
                    if (event instanceof PlayerVMEvent.Global) {
                        PlayerVMEvent.Global globalEvent = (PlayerVMEvent.Global) event;
                        invokeGlobalEvent(globalEvent.handlerName, globalEvent.args);
                    } else if (event instanceof PlayerVMEvent.Targeted) {
                        PlayerVMEvent.Targeted targetedEvent = (PlayerVMEvent.Targeted) event;
                        invokeTargetedEvent(targetedEvent.handlerName, targetedEvent.args, targetedEvent.instanceRefs);
                    } else if (event instanceof PlayerVMEvent.Callback) {
                        PlayerVMEvent.Callback callbackEvent = (PlayerVMEvent.Callback) event;
                        if (datumHandlerInvoker != null) {
                            DatumHandlerInvocation invocation = new DatumHandlerInvocation(
                                    callbackEvent.receiverRef, callbackEvent.handlerName, callbackEvent.args);
                            datumHandlerInvoker.apply(invocation);
                        }
                    }
                } catch (ScriptError err) {
                    if (errorHandler != null) {
                        errorHandler.accept(err);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.warn("Event loop stopped!");
    }

    /**
     * Stop the event loop.
     */
    public void stopEventLoop() {
        running = false;
        // Add a dummy event to unblock the take()
        eventQueue.offer(new PlayerVMEvent.Global("__stop__", new ArrayList<>()));
    }

    // ========== Helper Methods ==========

    private List<ScriptInstanceRef> getSpriteScriptInstances(DirPlayer player, int spriteNum) {
        var sprite = player.movie.score.getSprite((short) spriteNum);
        if (sprite == null) {
            return null;
        }
        List<ScriptInstanceRef> result = new ArrayList<>();
        for (int id : sprite.scriptInstanceList) {
            result.add(new ScriptInstanceRef(id));
        }
        return result;
    }

    private List<CastMemberRef> getActiveStaticScripts(DirPlayer player) {
        List<CastMemberRef> result = new ArrayList<>();

        // Frame script first
        ScoreBehaviorReference frameScript = player.movie.score.getScriptInFrame(player.movie.currentFrame);
        if (frameScript != null) {
            CastMemberRef scriptRef = new CastMemberRef(frameScript.castLib, frameScript.castMember);
            result.add(scriptRef);
        }

        // Then movie scripts
        List<Script> movieScripts = player.movie.castManager.getMovieScripts();
        if (movieScripts != null) {
            for (Script script : movieScripts) {
                result.add(script.memberRef);
            }
        }

        return result;
    }

    private List<ScriptInstanceRef> getActiveInstanceScripts(DirPlayer player) {
        List<ScriptInstanceRef> result = new ArrayList<>();

        // Collect active sprite script instances from active channels
        result.addAll(getActiveScriptInstanceList(player));

        // Collect global script instances (including parent scripts)
        for (var entry : player.getHydratedGlobals().entrySet()) {
            com.dirplayer.director.lingo.Datum datum = entry.getValue();
            if (datum.isScriptInstanceRef()) {
                try {
                    result.add(new ScriptInstanceRef(datum.intValue()));
                } catch (ScriptError e) {
                    // Skip invalid values
                }
            }
        }

        return result;
    }

    private List<ScriptInstanceRef> getActiveScriptInstanceList(DirPlayer player) {
        List<ScriptInstanceRef> result = new ArrayList<>();
        for (var channel : player.movie.score.channels) {
            if (channel.sprite != null && !channel.sprite.scriptInstanceList.isEmpty()) {
                for (int id : channel.sprite.scriptInstanceList) {
                    result.add(new ScriptInstanceRef(id));
                }
            }
        }
        return result;
    }

    private HandlerRef getScriptInstanceHandler(DirPlayer player, String handlerName, ScriptInstanceRef instanceRef) {
        ScriptInstance instance = player.allocator.getScriptInstance(instanceRef);
        if (instance == null) {
            return null;
        }

        // Get the script for this instance
        Script script = player.movie.castManager.getScriptByRef(instance.script);
        if (script == null) {
            return null;
        }

        // Check if this script has the handler
        if (script.hasHandler(handlerName)) {
            return new HandlerRef(script.memberRef, handlerName);
        }

        // If not found, check ancestor
        if (instance.ancestor != 0) {
            // ancestor is a DatumRef ID - we need to get the ScriptInstanceRef from the datum
            com.dirplayer.director.lingo.Datum ancestorDatum = player.getDatum(instance.ancestor);
            if (ancestorDatum != null && ancestorDatum.isScriptInstanceRef()) {
                try {
                    ScriptInstanceRef ancestorRef = new ScriptInstanceRef(ancestorDatum.intValue());
                    return getScriptInstanceHandler(player, handlerName, ancestorRef);
                } catch (ScriptError e) {
                    // If we can't get the ancestor instance ID, skip it
                    return null;
                }
            }
        }

        return null;
    }

    private boolean hasHandler(DirPlayer player, CastMemberRef scriptRef, String handlerName) {
        Script script = player.movie.castManager.getScriptByRef(scriptRef);
        if (script == null) {
            return false;
        }
        return script.hasHandler(handlerName);
    }

    private ScriptInstanceRef getFrameScriptInstance(DirPlayer player, CastMemberRef scriptRef) {
        // Check if scriptRef matches frame_script_member
        if (player.movie.frameScriptMember != null &&
            player.movie.frameScriptMember.equals(scriptRef)) {
            // Return the frame script instance
            if (player.movie.frameScriptInstance != null) {
                return new ScriptInstanceRef(player.movie.frameScriptInstance);
            }
        }
        return null;
    }

    private BeginSpriteData collectBeginSpriteData(DirPlayer player) {
        BeginSpriteData data = new BeginSpriteData();

        // Collect active channel numbers from sprite_spans
        Set<Integer> activeChannelNumbers = new HashSet<>();
        int currentFrame = player.movie.currentFrame;
        for (var span : player.movie.score.spriteSpans) {
            if (com.dirplayer.player.score.Score.isSpanInFrame(span, currentFrame)) {
                activeChannelNumbers.add(span.channelNumber);
            }
        }

        for (var channel : player.movie.score.channels) {
            if (channel.sprite == null || channel.sprite.scriptInstanceList.isEmpty()) {
                continue;
            }

            // Skip if not in an active span
            if (!activeChannelNumbers.contains(channel.number)) {
                continue;
            }

            // Skip if sprite hasn't entered or has already had beginSprite called
            if (!channel.sprite.entered) {
                continue;
            }

            // Check if beginSprite was already called for all behaviors
            boolean allBeginSpriteCalled = true;
            for (int id : channel.sprite.scriptInstanceList) {
                ScriptInstance instance = player.allocator.getScriptInstance(id);
                if (instance != null && !instance.beginSpriteCalled) {
                    allBeginSpriteCalled = false;
                    break;
                }
            }
            if (allBeginSpriteCalled) {
                continue;
            }

            for (int id : channel.sprite.scriptInstanceList) {
                ScriptInstanceRef ref = new ScriptInstanceRef(id);
                if (channel.number == 0) {
                    data.frameInstances.add(new FrameInstanceData(channel.number, ref));
                } else {
                    data.spriteInstances.add(new SpriteInstanceData(
                            ScoreRef.stage(), channel.number, ref));
                }
            }

            data.allChannels.add(new SpriteChannelRef(ScoreRef.stage(), channel.number));
        }

        // Collect filmloop sprites
        for (var channel : player.movie.score.channels) {
            if (channel.sprite == null || channel.sprite.memberRef == null) {
                continue;
            }
            var member = player.movie.castManager.findMemberByRef(channel.sprite.memberRef);
            if (member != null && member.memberType == com.dirplayer.director.MemberType.FilmLoop) {
                if (member.specificData instanceof com.dirplayer.player.cast.FilmLoopMember) {
                    com.dirplayer.player.cast.FilmLoopMember filmLoop =
                        (com.dirplayer.player.cast.FilmLoopMember) member.specificData;
                    ScoreRef filmLoopRef = ScoreRef.filmLoop(channel.sprite.memberRef);
                    // Add filmloop channels to allChannels
                    for (var filmLoopChannel : filmLoop.getScore().channels) {
                        data.allChannels.add(new SpriteChannelRef(filmLoopRef, filmLoopChannel.number));
                    }
                }
            }
        }

        return data;
    }

    private EndSpriteData collectEndSpriteData(DirPlayer player, ScoreRef scoreRef, List<Integer> spriteNums) {
        EndSpriteData data = new EndSpriteData();

        // Get appropriate score based on scoreRef
        com.dirplayer.player.score.Score score;
        if (scoreRef instanceof ScoreRef.FilmLoop) {
            ScoreRef.FilmLoop filmLoop = (ScoreRef.FilmLoop) scoreRef;
            var member = player.movie.castManager.findMemberByRef(filmLoop.memberRef);
            if (member != null && member.specificData instanceof com.dirplayer.player.cast.FilmLoopMember) {
                com.dirplayer.player.cast.FilmLoopMember flMember =
                    (com.dirplayer.player.cast.FilmLoopMember) member.specificData;
                score = flMember.getScore();
            } else {
                score = player.movie.score;
            }
        } else {
            score = player.movie.score;
        }

        for (var channel : score.channels) {
            if (!spriteNums.contains(channel.number)) {
                continue;
            }
            if (channel.sprite == null || channel.sprite.scriptInstanceList.isEmpty()) {
                continue;
            }

            List<ScriptInstanceRef> behaviors = new ArrayList<>();
            for (int id : channel.sprite.scriptInstanceList) {
                behaviors.add(new ScriptInstanceRef(id));
            }

            if (channel.number > 0) {
                data.spriteTuples.add(new SpriteBehaviorTuple(channel.sprite.number, behaviors));
            } else {
                data.frameTuples.add(new SpriteBehaviorTuple(channel.sprite.number, behaviors));
            }
        }

        return data;
    }

    private BehaviorData collectBehaviorData(DirPlayer player) {
        BehaviorData data = new BehaviorData();

        // Collect active channel numbers from sprite_spans
        Set<Integer> activeChannelNumbers = new HashSet<>();
        int currentFrame = player.movie.currentFrame;
        for (var span : player.movie.score.spriteSpans) {
            if (com.dirplayer.player.score.Score.isSpanInFrame(span, currentFrame)) {
                activeChannelNumbers.add(span.channelNumber);
            }
        }

        for (var channel : player.movie.score.channels) {
            if (channel.sprite == null || channel.sprite.scriptInstanceList.isEmpty()) {
                continue;
            }

            // Skip if sprite hasn't entered or isn't in an active span
            if (!channel.sprite.entered || !activeChannelNumbers.contains(channel.number)) {
                continue;
            }

            List<ScriptInstanceRef> behaviors = new ArrayList<>();
            for (int id : channel.sprite.scriptInstanceList) {
                behaviors.add(new ScriptInstanceRef(id));
            }

            if (channel.number > 0) {
                data.spriteBehaviors.add(new SpriteBehaviorData(
                        ScoreRef.stage(), channel.number, behaviors));
            } else if (channel.number == 0) {
                data.frameBehaviors.add(new FrameBehaviorData(channel.number, behaviors));
            }
        }

        // Collect filmloop sprite behaviors
        for (var channel : player.movie.score.channels) {
            if (channel.sprite == null || channel.sprite.memberRef == null) {
                continue;
            }
            var member = player.movie.castManager.findMemberByRef(channel.sprite.memberRef);
            if (member != null && member.memberType == com.dirplayer.director.MemberType.FilmLoop) {
                if (member.specificData instanceof com.dirplayer.player.cast.FilmLoopMember) {
                    com.dirplayer.player.cast.FilmLoopMember filmLoop =
                        (com.dirplayer.player.cast.FilmLoopMember) member.specificData;
                    ScoreRef filmLoopRef = ScoreRef.filmLoop(channel.sprite.memberRef);

                    for (var filmLoopChannel : filmLoop.getScore().channels) {
                        if (filmLoopChannel.sprite == null ||
                            filmLoopChannel.sprite.scriptInstanceList.isEmpty()) {
                            continue;
                        }
                        if (!filmLoopChannel.sprite.entered) {
                            continue;
                        }

                        List<ScriptInstanceRef> filmLoopBehaviors = new ArrayList<>();
                        for (int id : filmLoopChannel.sprite.scriptInstanceList) {
                            filmLoopBehaviors.add(new ScriptInstanceRef(id));
                        }

                        if (filmLoopChannel.number > 0) {
                            data.spriteBehaviors.add(new SpriteBehaviorData(
                                    filmLoopRef, filmLoopChannel.number, filmLoopBehaviors));
                        }
                    }
                }
            }
        }

        return data;
    }

    private DirPlayer.ScoreRef toPlayerScoreRef(ScoreRef scoreRef) {
        DirPlayer.ScoreRef result = new DirPlayer.ScoreRef();
        if (scoreRef instanceof ScoreRef.Stage) {
            result.isMainScore = true;
        } else if (scoreRef instanceof ScoreRef.FilmLoop) {
            ScoreRef.FilmLoop filmLoop = (ScoreRef.FilmLoop) scoreRef;
            result.isMainScore = false;
            result.castLib = filmLoop.memberRef.castLib;
            result.castMember = filmLoop.memberRef.castMember;
        }
        return result;
    }

    /**
     * Unwrap a result, handling errors.
     * Port of Rust player_unwrap_result.
     */
    public int unwrapResult(EventResult result, ScriptError error) {
        if (error != null) {
            if (errorHandler != null) {
                errorHandler.accept(error);
            }
            return 0; // DatumRef.Void
        }
        return result.resultRef;
    }

    // ========== Helper Classes ==========

    /**
     * Invocation data for calling a script handler.
     */
    public static class HandlerInvocation {
        public final ScriptInstanceRef instanceRef;
        public final HandlerRef handlerRef;
        public final List<Integer> args;

        public HandlerInvocation(ScriptInstanceRef instanceRef, HandlerRef handlerRef, List<Integer> args) {
            this.instanceRef = instanceRef;
            this.handlerRef = handlerRef;
            this.args = args;
        }
    }

    /**
     * Invocation data for calling a handler on a datum.
     */
    public static class DatumHandlerInvocation {
        public final int receiverRef;
        public final String handlerName;
        public final List<Integer> args;

        public DatumHandlerInvocation(int receiverRef, String handlerName, List<Integer> args) {
            this.receiverRef = receiverRef;
            this.handlerName = handlerName;
            this.args = args;
        }
    }

    private static class InstanceHandlerPair {
        final ScriptInstanceRef instanceRef;
        final HandlerRef handlerRef;

        InstanceHandlerPair(ScriptInstanceRef instanceRef, HandlerRef handlerRef) {
            this.instanceRef = instanceRef;
            this.handlerRef = handlerRef;
        }
    }

    /**
     * Reference to a sprite channel in a score.
     */
    public static class SpriteChannelRef {
        public final ScoreRef scoreRef;
        public final int channelNumber;

        public SpriteChannelRef(ScoreRef scoreRef, int channelNumber) {
            this.scoreRef = scoreRef;
            this.channelNumber = channelNumber;
        }
    }

    // Data collection helper classes
    private static class BeginSpriteData {
        List<SpriteInstanceData> spriteInstances = new ArrayList<>();
        List<FrameInstanceData> frameInstances = new ArrayList<>();
        List<SpriteChannelRef> allChannels = new ArrayList<>();
    }

    private static class SpriteInstanceData {
        final ScoreRef scoreRef;
        final int spriteNumber;
        final ScriptInstanceRef instanceRef;

        SpriteInstanceData(ScoreRef scoreRef, int spriteNumber, ScriptInstanceRef instanceRef) {
            this.scoreRef = scoreRef;
            this.spriteNumber = spriteNumber;
            this.instanceRef = instanceRef;
        }
    }

    private static class FrameInstanceData {
        final int channelNumber;
        final ScriptInstanceRef instanceRef;

        FrameInstanceData(int channelNumber, ScriptInstanceRef instanceRef) {
            this.channelNumber = channelNumber;
            this.instanceRef = instanceRef;
        }
    }

    private static class EndSpriteData {
        List<SpriteBehaviorTuple> spriteTuples = new ArrayList<>();
        List<SpriteBehaviorTuple> frameTuples = new ArrayList<>();
    }

    private static class SpriteBehaviorTuple {
        final int spriteNum;
        final List<ScriptInstanceRef> behaviors;

        SpriteBehaviorTuple(int spriteNum, List<ScriptInstanceRef> behaviors) {
            this.spriteNum = spriteNum;
            this.behaviors = behaviors;
        }
    }

    private static class BehaviorData {
        List<SpriteBehaviorData> spriteBehaviors = new ArrayList<>();
        List<FrameBehaviorData> frameBehaviors = new ArrayList<>();
    }

    private static class SpriteBehaviorData {
        final ScoreRef scoreRef;
        final int spriteNumber;
        final List<ScriptInstanceRef> behaviors;

        SpriteBehaviorData(ScoreRef scoreRef, int spriteNumber, List<ScriptInstanceRef> behaviors) {
            this.scoreRef = scoreRef;
            this.spriteNumber = spriteNumber;
            this.behaviors = behaviors;
        }
    }

    private static class FrameBehaviorData {
        final int channelNumber;
        final List<ScriptInstanceRef> behaviors;

        FrameBehaviorData(int channelNumber, List<ScriptInstanceRef> behaviors) {
            this.channelNumber = channelNumber;
            this.behaviors = behaviors;
        }
    }
}
