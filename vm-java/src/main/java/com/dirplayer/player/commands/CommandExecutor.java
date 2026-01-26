package com.dirplayer.player.commands;

import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.SimpleLogger;


/**
 * Command executor for player VM commands.
 * Port of Rust run_command_loop function.
 */
public class CommandExecutor {
    private static final SimpleLogger logger = SimpleLogger.getLogger(CommandExecutor.class);

    private final DirPlayer player;

    public CommandExecutor(DirPlayer player) {
        this.player = player;
    }

    /**
     * Execute a player command.
     */
    public Object executeCommand(PlayerCommand command) throws ScriptError {
        logger.debug("Executing command: {}", command.format());

        if (command instanceof PlayerCommand.LoadMovieFromFile) {
            return executeLoadMovieFromFile((PlayerCommand.LoadMovieFromFile) command);
        } else if (command instanceof PlayerCommand.SetExternalParams) {
            return executeSetExternalParams((PlayerCommand.SetExternalParams) command);
        } else if (command instanceof PlayerCommand.SetBasePath) {
            return executeSetBasePath((PlayerCommand.SetBasePath) command);
        } else if (command instanceof PlayerCommand.SetStageSize) {
            return executeSetStageSize((PlayerCommand.SetStageSize) command);
        } else if (command instanceof PlayerCommand.MouseDown) {
            return executeMouseDown((PlayerCommand.MouseDown) command);
        } else if (command instanceof PlayerCommand.MouseUp) {
            return executeMouseUp((PlayerCommand.MouseUp) command);
        } else if (command instanceof PlayerCommand.MouseMove) {
            return executeMouseMove((PlayerCommand.MouseMove) command);
        } else if (command instanceof PlayerCommand.KeyDown) {
            return executeKeyDown((PlayerCommand.KeyDown) command);
        } else if (command instanceof PlayerCommand.KeyUp) {
            return executeKeyUp((PlayerCommand.KeyUp) command);
        } else if (command instanceof PlayerCommand.GoToFrame) {
            return executeGoToFrame((PlayerCommand.GoToFrame) command);
        } else if (command instanceof PlayerCommand.Play) {
            return executePlay();
        } else if (command instanceof PlayerCommand.Stop) {
            return executeStop();
        } else if (command instanceof PlayerCommand.TimeoutTriggered) {
            return executeTimeoutTriggered((PlayerCommand.TimeoutTriggered) command);
        } else if (command instanceof PlayerCommand.TriggerAlertHook) {
            return executeTriggerAlertHook();
        }

        logger.warn("Unknown command type: {}", command.getClass().getName());
        return null;
    }

    private Object executeLoadMovieFromFile(PlayerCommand.LoadMovieFromFile cmd) throws ScriptError {
        logger.info("Loading movie from file: {}", cmd.path);
        // TODO: Implement movie loading
        return null;
    }

    private Object executeSetExternalParams(PlayerCommand.SetExternalParams cmd) {
        player.externalParams.putAll(cmd.params);
        return null;
    }

    private Object executeSetBasePath(PlayerCommand.SetBasePath cmd) {
        player.movie.basePath = cmd.path;
        return null;
    }

    private Object executeSetStageSize(PlayerCommand.SetStageSize cmd) {
        player.setStageSize(cmd.width, cmd.height);
        return null;
    }

    private Object executeMouseDown(PlayerCommand.MouseDown cmd) throws ScriptError {
        player.mouseDown(cmd.x, cmd.y);
        // TODO: Dispatch mouse events
        return null;
    }

    private Object executeMouseUp(PlayerCommand.MouseUp cmd) throws ScriptError {
        player.mouseUp(cmd.x, cmd.y);
        // TODO: Dispatch mouse events
        return null;
    }

    private Object executeMouseMove(PlayerCommand.MouseMove cmd) {
        player.mouseMove(cmd.x, cmd.y);
        return null;
    }

    private Object executeKeyDown(PlayerCommand.KeyDown cmd) throws ScriptError {
        player.keyDown(cmd.key, cmd.keyCode);
        // TODO: Dispatch key events
        return null;
    }

    private Object executeKeyUp(PlayerCommand.KeyUp cmd) throws ScriptError {
        player.keyUp(cmd.key, cmd.keyCode);
        return null;
    }

    private Object executeGoToFrame(PlayerCommand.GoToFrame cmd) {
        player.goToFrame(cmd.frame);
        return null;
    }

    private Object executePlay() {
        player.play();
        return null;
    }

    private Object executeStop() {
        player.stop();
        return null;
    }

    private Object executeTimeoutTriggered(PlayerCommand.TimeoutTriggered cmd) throws ScriptError {
        // TODO: Handle timeout callback
        logger.debug("Timeout triggered: {}", cmd.timeoutRef);
        return null;
    }

    private Object executeTriggerAlertHook() throws ScriptError {
        // TODO: Trigger alert hook
        logger.debug("Alert hook triggered");
        return null;
    }
}
