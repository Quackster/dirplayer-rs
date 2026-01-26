package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.MathObject;
import com.dirplayer.player.ScriptError;

import java.util.ArrayList;
import java.util.List;

/**
 * Handlers for math datum operations.
 * Port of Rust MathDatumHandlers.
 */
public class MathHandlers {

    /**
     * Get a property from a math object.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("math"));
            case "pi":
                return player.allocDatum(Datum.ofFloat(Math.PI));
            default:
                throw new ScriptError("Unknown math property '" + prop + "'");
        }
    }

    /**
     * Set a property on a math object.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        throw new ScriptError("Cannot set math property '" + prop + "'");
    }

    /**
     * Call handler on math datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        int mathId = player.getDatum(datumRef).getMathRef();
        MathObject mathObj = player.mathObjects.get(mathId);

        if (mathObj == null) {
            throw new ScriptError("Math object " + mathId + " not found");
        }

        // Convert args to float values
        List<Double> argValues = new ArrayList<>();
        for (int argRef : args) {
            try {
                argValues.add(player.getDatum(argRef).floatValue());
            } catch (ScriptError e) {
                // Skip non-numeric arguments
            }
        }

        String name = handlerName.toLowerCase();
        double result;

        switch (name) {
            // Basic functions
            case "abs":
                result = Math.abs(getArg(argValues, 0, 0.0));
                break;
            case "ceil":
                result = Math.ceil(getArg(argValues, 0, 0.0));
                break;
            case "floor":
                result = Math.floor(getArg(argValues, 0, 0.0));
                break;
            case "round":
                result = Math.round(getArg(argValues, 0, 0.0));
                break;

            // Trigonometric functions
            case "sin":
                result = Math.sin(getArg(argValues, 0, 0.0));
                break;
            case "cos":
                result = Math.cos(getArg(argValues, 0, 0.0));
                break;
            case "tan":
                result = Math.tan(getArg(argValues, 0, 0.0));
                break;
            case "asin":
                result = Math.asin(getArg(argValues, 0, 0.0));
                break;
            case "acos":
                result = Math.acos(getArg(argValues, 0, 0.0));
                break;
            case "atan":
                result = Math.atan(getArg(argValues, 0, 0.0));
                break;

            // Power and logarithm functions
            case "sqrt":
                result = Math.sqrt(getArg(argValues, 0, 0.0));
                break;
            case "exp":
                result = Math.exp(getArg(argValues, 0, 0.0));
                break;
            case "log":
                result = Math.log(getArg(argValues, 0, 0.0));
                break;
            case "pow":
            case "power": {
                double base = getArg(argValues, 0, 0.0);
                double exp = getArg(argValues, 1, 1.0);
                result = Math.pow(base, exp);
                break;
            }

            // Min/Max
            case "min":
                result = Double.POSITIVE_INFINITY;
                for (double v : argValues) {
                    result = Math.min(result, v);
                }
                if (argValues.isEmpty()) {
                    result = 0.0;
                }
                break;
            case "max":
                result = Double.NEGATIVE_INFINITY;
                for (double v : argValues) {
                    result = Math.max(result, v);
                }
                if (argValues.isEmpty()) {
                    result = 0.0;
                }
                break;

            default:
                throw new ScriptError("Unknown math function '" + handlerName + "'");
        }

        return player.allocDatum(Datum.ofFloat(result));
    }

    private static double getArg(List<Double> args, int index, double defaultValue) {
        if (index < args.size()) {
            return args.get(index);
        }
        return defaultValue;
    }
}
