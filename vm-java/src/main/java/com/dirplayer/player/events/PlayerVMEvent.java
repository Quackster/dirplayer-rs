package com.dirplayer.player.events;

import com.dirplayer.player.script.ScriptInstanceRef;

import java.util.List;

/**
 * Event types that can be dispatched to the player VM.
 * Port of Rust PlayerVMEvent enum.
 */
public abstract class PlayerVMEvent {

    /**
     * Global event dispatched to all active scripts.
     */
    public static class Global extends PlayerVMEvent {
        public final String handlerName;
        public final List<Integer> args;

        public Global(String handlerName, List<Integer> args) {
            this.handlerName = handlerName;
            this.args = args;
        }
    }

    /**
     * Targeted event dispatched to specific script instances.
     */
    public static class Targeted extends PlayerVMEvent {
        public final String handlerName;
        public final List<Integer> args;
        public final List<ScriptInstanceRef> instanceRefs;

        public Targeted(String handlerName, List<Integer> args, List<ScriptInstanceRef> instanceRefs) {
            this.handlerName = handlerName;
            this.args = args;
            this.instanceRefs = instanceRefs;
        }
    }

    /**
     * Callback event dispatched to a specific datum receiver.
     */
    public static class Callback extends PlayerVMEvent {
        public final int receiverRef;
        public final String handlerName;
        public final List<Integer> args;

        public Callback(int receiverRef, String handlerName, List<Integer> args) {
            this.receiverRef = receiverRef;
            this.handlerName = handlerName;
            this.args = args;
        }
    }
}
