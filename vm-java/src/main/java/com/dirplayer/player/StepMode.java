package com.dirplayer.player;

/**
 * Step modes for debugger stepping.
 * Port of Rust StepMode enum.
 */
public enum StepMode {
    None,
    StepInto,
    StepOver,
    StepOut,
    StepIntoLine,
    StepOverLine
}
