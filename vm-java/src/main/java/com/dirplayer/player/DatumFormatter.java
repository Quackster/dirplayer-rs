package com.dirplayer.player;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.bitmap.Bitmap;
import com.dirplayer.player.bitmap.BitmapRef;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Datum formatting utilities for debug output.
 * Port of Rust datum_formatting.rs
 */
public class DatumFormatter {

    /**
     * Format a datum value as a debug string.
     */
    public static String formatDatum(int datumRef, DirPlayer player) {
        Datum datum = player.allocator.get(datumRef);
        return formatConcreteDatum(datum, player);
    }

    /**
     * Format a concrete datum value.
     */
    public static String formatConcreteDatum(Datum datum, DirPlayer player) {
        if (datum == null) {
            return "Void";
        }

        DatumType type = datum.getType();

        switch (type) {
            case Void:
                return "Void";
            case Null:
                return "<Null>";
            case Int:
                try {
                    return String.valueOf(datum.intValue());
                } catch (ScriptError e) {
                    return "<error: " + e.getMessage() + ">";
                }
            case Float:
                try {
                    return formatFloatWithPrecision(datum.floatValue(), player);
                } catch (ScriptError e) {
                    return "<error: " + e.getMessage() + ">";
                }
            case String:
                try {
                    return "\"" + datum.stringValue() + "\"";
                } catch (ScriptError e) {
                    return "<error: " + e.getMessage() + ">";
                }
            case Symbol:
                try {
                    return "#" + datum.stringValue();
                } catch (ScriptError e) {
                    return "<error: " + e.getMessage() + ">";
                }
            case List:
                return formatList(datum, player);
            case PropList:
                return formatPropList(datum, player);
            case Point:
                return formatPoint(datum, player);
            case Rect:
                return formatRect(datum, player);
            case CastMemberRef:
                try {
                    CastMemberRef memberRef = datum.toMemberRef();
                    return "(member " + memberRef.castMember + " of castLib " + memberRef.castLib + ")";
                } catch (ScriptError e) {
                    return "<error: " + e.getMessage() + ">";
                }
            case SpriteRef:
                try {
                    return "(sprite " + datum.intValue() + ")";
                } catch (ScriptError e) {
                    return "<error: " + e.getMessage() + ">";
                }
            case ColorRef:
                try {
                    return formatColorRef(datum.toColorRef());
                } catch (ScriptError e) {
                    return "<error: " + e.getMessage() + ">";
                }
            case BitmapRef:
                return formatBitmapRef(datum, player);
            case CastLibRef:
                try {
                    return "castLib(" + datum.intValue() + ")";
                } catch (ScriptError e) {
                    return "<error: " + e.getMessage() + ">";
                }
            case StageRef:
                return "the stage";
            case ScriptRef:
                return "(script)";
            case ScriptInstanceRef:
                return "<offspring _ _ _>";
            case TimeoutRef:
                return "timeout(\"" + datum.getTimeoutName() + "\")";
            case CursorRef:
                return "<cursor>";
            case SoundChannel:
                return "<soundChannel>";
            case Xtra:
                return "<Xtra \"" + datum.getXtraName() + "\" _ _______>";
            case XtraInstance:
                return "<Xtra child \"" + datum.getXtraName() + "\" #" + datum.getXtraInstanceId() + ">";
            case XmlRef:
                try {
                    return "<xml:" + datum.intValue() + ">";
                } catch (ScriptError e) {
                    return "<error: " + e.getMessage() + ">";
                }
            case DateRef:
                return "<date>";
            case MathRef:
                return "<math>";
            case Vector:
                try {
                    double[] vec = datum.toVector();
                    return "vector(" + formatFloatWithPrecision(vec[0], player) + ", " +
                           formatFloatWithPrecision(vec[1], player) + ", " +
                           formatFloatWithPrecision(vec[2], player) + ")";
                } catch (ScriptError e) {
                    return "<error: " + e.getMessage() + ">";
                }
            case PaletteRef:
                return formatPaletteRef(datum);
            case PlayerRef:
                return "<_player>";
            case MovieRef:
                return "<_movie>";
            case SoundRef:
                return "<_sound>";
            case Matte:
                return "<mask:0000000>";
            default:
                return "<" + type + ">";
        }
    }

    /**
     * Format a float with the player's precision setting.
     */
    public static String formatFloatWithPrecision(double value, DirPlayer player) {
        int precision = player != null ? player.floatPrecision : 4;

        // Check if it's a whole number
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.format("%." + precision + "f", value);
        }

        // Format with precision
        String formatted = String.format("%." + precision + "f", value);

        // Remove trailing zeros but keep at least one decimal
        if (formatted.contains(".")) {
            formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", ".0");
        }

        return formatted;
    }

    /**
     * Format numeric value.
     */
    public static String formatNumericValue(Datum datum, DirPlayer player) {
        try {
            if (datum.getType() == DatumType.Int) {
                return String.valueOf(datum.intValue());
            } else if (datum.getType() == DatumType.Float) {
                return formatFloatWithPrecision(datum.floatValue(), player);
            }
        } catch (ScriptError e) {
            return "<error: " + e.getMessage() + ">";
        }
        return datum.toString();
    }

    /**
     * Convert datum to string for concatenation.
     */
    public static String datumToStringForConcat(Datum datum, DirPlayer player) {
        if (datum == null) {
            return "";
        }

        DatumType type = datum.getType();

        try {
            switch (type) {
                case Void:
                case Null:
                    return "";
                case String:
                    return datum.stringValue();
                case Symbol:
                    return datum.stringValue();
                case Int:
                    return String.valueOf(datum.intValue());
                case Float:
                    return formatFloatWithPrecision(datum.floatValue(), player);
                default:
                    return formatConcreteDatum(datum, player);
            }
        } catch (ScriptError e) {
            return "";
        }
    }

    private static String formatList(Datum datum, DirPlayer player) {
        try {
            java.util.List<Integer> items = datum.toList();
            if (items == null || items.isEmpty()) {
                return "[]";
            }

            String elements = items.stream()
                .map(ref -> formatDatum(ref, player))
                .collect(Collectors.joining(", "));

            return "[" + elements + "]";
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String formatPropList(Datum datum, DirPlayer player) {
        try {
            java.util.List<int[]> pairs = datum.toPropList();
            if (pairs == null || pairs.isEmpty()) {
                return "[:]";
            }

            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < pairs.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                int[] pair = pairs.get(i);
                String keyStr = formatDatum(pair[0], player);
                String valueStr = formatDatum(pair[1], player);
                sb.append(keyStr).append(": ").append(valueStr);
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "[:]";
        }
    }

    private static String formatPoint(Datum datum, DirPlayer player) {
        try {
            int[] point = datum.toPoint();
            return "point(" + point[0] + ", " + point[1] + ")";
        } catch (Exception e) {
            return "point(0, 0)";
        }
    }

    private static String formatRect(Datum datum, DirPlayer player) {
        try {
            int[] rect = datum.toRect();
            return "rect(" + rect[0] + ", " + rect[1] + ", " + rect[2] + ", " + rect[3] + ")";
        } catch (Exception e) {
            return "rect(0, 0, 0, 0)";
        }
    }

    private static String formatColorRef(ColorRef color) {
        if (color.isRgb()) {
            return "rgb(" + color.getR() + ", " + color.getG() + ", " + color.getB() + ")";
        } else {
            return "color(" + color.getPaletteIndex() + ")";
        }
    }

    private static String formatBitmapRef(Datum datum, DirPlayer player) {
        try {
            BitmapRef bitmapRef = datum.toBitmapRef();
            Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef.bitmapId);
            if (bitmap != null) {
                return "<bitmap " + bitmap.getWidth() + "x" + bitmap.getHeight() + "x" + bitmap.getBitDepth() + ">";
            }
            return "<bitmap>";
        } catch (Exception e) {
            return "<bitmap>";
        }
    }

    private static String formatPaletteRef(Datum datum) {
        try {
            // Try to get the palette reference info
            int paletteId = datum.intValue();
            if (paletteId < 0) {
                // Built-in palette
                return "palette(" + paletteId + ")";
            } else {
                // Cast member palette
                return "palette(member " + paletteId + ")";
            }
        } catch (Exception e) {
            return "<palette>";
        }
    }
}
