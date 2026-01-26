package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.List;

/**
 * Handlers for vector datum operations.
 * Port of Rust VectorDatumHandlers.
 */
public class VectorHandlers {

    /**
     * Call handler on vector datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "getat":
                return getAt(player, datumRef, args);
            case "setat":
                return setAt(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for vector");
        }
    }

    /**
     * Get a vector component by index (1-based).
     */
    public static int getAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        double[] vec = datumToVec(player, player.getDatum(datumRef));
        int index = player.getDatum(args.get(0)).intValue() - 1; // Convert to 0-based

        if (index < 0 || index >= 3) {
            throw new ScriptError("Index out of range for vector");
        }

        return player.allocDatum(Datum.ofFloat(vec[index]));
    }

    /**
     * Set a vector component by index (1-based).
     */
    public static int setAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (!datum.isVector()) {
            throw new ScriptError("Cannot set prop of non-vector");
        }

        double[] vec = datum.toVector();
        int index = player.getDatum(args.get(0)).intValue() - 1; // Convert to 0-based

        if (index < 0 || index >= 3) {
            throw new ScriptError("Index out of range for vector");
        }

        double value = player.getDatum(args.get(1)).floatValue();
        vec[index] = value;

        // Update the datum with the new vector values
        datum.setVectorValue(vec);

        return 0; // Void
    }

    /**
     * Get a property from a vector.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        double[] vec = datumToVec(player, player.getDatum(datumRef));

        Datum result;
        switch (prop.toLowerCase()) {
            case "x":
                result = Datum.ofFloat(vec[0]);
                break;
            case "y":
                result = Datum.ofFloat(vec[1]);
                break;
            case "z":
                result = Datum.ofFloat(vec[2]);
                break;
            case "ilk":
                result = Datum.ofSymbol("vector");
                break;
            default:
                throw new ScriptError("Cannot get vector property " + prop);
        }

        return player.allocDatum(result);
    }

    /**
     * Set a property on a vector.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (!datum.isVector()) {
            throw new ScriptError("Cannot set prop of non-vector");
        }

        double[] vec = datum.toVector();
        double value = player.getDatum(valueRef).floatValue();

        switch (prop.toLowerCase()) {
            case "x":
                vec[0] = value;
                break;
            case "y":
                vec[1] = value;
                break;
            case "z":
                vec[2] = value;
                break;
            default:
                throw new ScriptError("Cannot set vector property " + prop);
        }

        datum.setVectorValue(vec);
    }

    /**
     * Vector addition.
     */
    public static int add(DirPlayer player, int aRef, int bRef) throws ScriptError {
        double[] va = datumToVec(player, player.getDatum(aRef));
        double[] vb = datumToVec(player, player.getDatum(bRef));

        double[] result = new double[] {
            va[0] + vb[0],
            va[1] + vb[1],
            va[2] + vb[2]
        };

        return player.allocDatum(Datum.ofVector(result));
    }

    /**
     * Vector subtraction.
     */
    public static int sub(DirPlayer player, int aRef, int bRef) throws ScriptError {
        double[] va = datumToVec(player, player.getDatum(aRef));
        double[] vb = datumToVec(player, player.getDatum(bRef));

        double[] result = new double[] {
            va[0] - vb[0],
            va[1] - vb[1],
            va[2] - vb[2]
        };

        return player.allocDatum(Datum.ofVector(result));
    }

    /**
     * Vector multiplication (scalar or component-wise).
     */
    public static int mul(DirPlayer player, int aRef, int bRef) throws ScriptError {
        double[] va = datumToVec(player, player.getDatum(aRef));
        Datum bDatum = player.getDatum(bRef);

        double[] result;
        if (bDatum.isFloat() || bDatum.isInt()) {
            // Scalar multiplication
            double scalar = bDatum.floatValue();
            result = new double[] {
                va[0] * scalar,
                va[1] * scalar,
                va[2] * scalar
            };
        } else if (bDatum.isVector()) {
            // Component-wise multiplication
            double[] vb = bDatum.toVector();
            result = new double[] {
                va[0] * vb[0],
                va[1] * vb[1],
                va[2] * vb[2]
            };
        } else {
            throw new ScriptError("Invalid operand for vector multiplication");
        }

        return player.allocDatum(Datum.ofVector(result));
    }

    /**
     * Vector division (scalar or component-wise).
     */
    public static int div(DirPlayer player, int aRef, int bRef) throws ScriptError {
        double[] va = datumToVec(player, player.getDatum(aRef));
        Datum bDatum = player.getDatum(bRef);

        double[] result;
        if (bDatum.isFloat() || bDatum.isInt()) {
            // Scalar division
            double scalar = bDatum.floatValue();
            if (scalar == 0.0) {
                throw new ScriptError("Division by zero");
            }
            result = new double[] {
                va[0] / scalar,
                va[1] / scalar,
                va[2] / scalar
            };
        } else if (bDatum.isVector()) {
            // Component-wise division
            double[] vb = bDatum.toVector();
            if (vb[0] == 0.0 || vb[1] == 0.0 || vb[2] == 0.0) {
                throw new ScriptError("Division by zero in vector components");
            }
            result = new double[] {
                va[0] / vb[0],
                va[1] / vb[1],
                va[2] / vb[2]
            };
        } else {
            throw new ScriptError("Invalid operand for vector division");
        }

        return player.allocDatum(Datum.ofVector(result));
    }

    // Helper methods

    /**
     * Convert a Datum (Vector or List) into a double[3] array.
     */
    private static double[] datumToVec(DirPlayer player, Datum datum) throws ScriptError {
        if (datum.isVector()) {
            return datum.toVector();
        } else if (datum.isList()) {
            List<Integer> list = datum.toList();
            if (list.size() == 3) {
                return new double[] {
                    player.getDatum(list.get(0)).floatValue(),
                    player.getDatum(list.get(1)).floatValue(),
                    player.getDatum(list.get(2)).floatValue()
                };
            }
        }
        throw new ScriptError("Expected a vector");
    }
}
