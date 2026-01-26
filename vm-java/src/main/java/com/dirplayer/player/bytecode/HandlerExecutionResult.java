package com.dirplayer.player.bytecode;

/**
 * Result of bytecode handler execution.
 * Port of Rust HandlerExecutionResult enum.
 */
public enum HandlerExecutionResult {
    /** Advance to the next bytecode instruction */
    ADVANCE,
    /** Stop execution and return */
    STOP,
    /** Jump to a specific bytecode position */
    JUMP,
    /** Handler was awaited (async) */
    AWAITED,
    /** Error occurred */
    ERROR
}
