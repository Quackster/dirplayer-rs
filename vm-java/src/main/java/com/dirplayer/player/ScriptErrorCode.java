package com.dirplayer.player;

/**
 * Error codes for script errors.
 * Port of Rust ScriptErrorCode enum.
 */
public enum ScriptErrorCode {
    None,
    Generic,
    TypeError,
    IndexOutOfRange,
    PropertyNotFound,
    HandlerNotFound,
    DivisionByZero,
    VariableNotDefined,
    ObjectNotFound,
    InvalidArgument,
    OutOfMemory,
    Timeout,
    Abort,
    UserAbort,
    ScriptError,
    CompileError,
    RuntimeError,
    IOError,
    NetworkError;

    public String getMessage() {
        switch (this) {
            case TypeError: return "Type mismatch";
            case IndexOutOfRange: return "Index out of range";
            case PropertyNotFound: return "Property not found";
            case HandlerNotFound: return "Handler not found";
            case DivisionByZero: return "Division by zero";
            case VariableNotDefined: return "Variable not defined";
            case ObjectNotFound: return "Object not found";
            case InvalidArgument: return "Invalid argument";
            case OutOfMemory: return "Out of memory";
            case Timeout: return "Script timeout";
            case Abort: return "Script aborted";
            case UserAbort: return "User abort";
            case CompileError: return "Compile error";
            case RuntimeError: return "Runtime error";
            case IOError: return "I/O error";
            case NetworkError: return "Network error";
            default: return "Script error";
        }
    }
}
