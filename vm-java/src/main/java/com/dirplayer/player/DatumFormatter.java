package com.dirplayer.player;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.bitmap.Bitmap;

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
        Datum datum = player.allocator.getDatum(datumRef);
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
                return String.valueOf(datum.intValue());
            case Float:
                return formatFloatWithPrecision(datum.floatValue(), player);
            case String:
                return "\"" + datum.stringValue() + "\"";
            case Symbol:
                return "#" + datum.stringValue();
            case List:
                return formatList(datum, player);
            case PropList:
                return formatPropList(datum, player);
            case Point:
                return formatPoint(datum, player);
            case Rect:
                return formatRect(datum, player);
            case CastMember:
                CastMemberRef memberRef = datum.getCastMemberRef();
                return "(member " + memberRef.castMember + " of castLib " + memberRef.castLib + ")";
            case SpriteRef:
                return "(sprite " + datum.intValue() + ")";
            case ColorRef:
                return formatColorRef(datum.getColorRef());
            case BitmapRef:
                return formatBitmapRef(datum, player);
            case CastLib:
                return "castLib(" + datum.intValue() + ")";
            case Stage:
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
            case XtraRef:
                return "<Xtra \"" + datum.getXtraName() + "\" _ _______>";
            case XtraInstance:
                return "<Xtra child \"" + datum.getXtraName() + "\" #" + datum.getXtraInstanceId() + ">";
            case XmlRef:
                return "<xml:" + datum.intValue() + ">";
            case DateRef:
                return "<date>";
            case MathRef:
                return "<math>";
            case Vector:
                double[] vec = datum.getVectorValue();
                return "vector(" + formatFloatWithPrecision(vec[0], player) + ", " +
                       formatFloatWithPrecision(vec[1], player) + ", " +
                       formatFloatWithPrecision(vec[2], player) + ")";
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
        if (datum.getType() == DatumType.Int) {
            return String.valueOf(datum.intValue());
        } else if (datum.getType() == DatumType.Float) {
            return formatFloatWithPrecision(datum.floatValue(), player);
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
    }

    private static String formatList(Datum datum, DirPlayer player) {
        int[] items = datum.getListValue();
        if (items == null || items.length == 0) {
            return "[]";
        }

        String elements = IntStream.of(items)
            .mapToObj(ref -> formatDatum(ref, player))
            .collect(Collectors.joining(", "));

        return "[" + elements + "]";
    }

    private static String formatPropList(Datum datum, DirPlayer player) {
        // TODO: Implement prop list formatting
        return "[:]";
    }

    private static String formatPoint(Datum datum, DirPlayer player) {
        int[] point = datum.getPointValue();
        return "point(" + point[0] + ", " + point[1] + ")";
    }

    private static String formatRect(Datum datum, DirPlayer player) {
        int[] rect = datum.getRectValue();
        return "rect(" + rect[0] + ", " + rect[1] + ", " + rect[2] + ", " + rect[3] + ")";
    }

    private static String formatColorRef(ColorRef color) {
        if (color.isRgb()) {
            return "rgb(" + color.getR() + ", " + color.getG() + ", " + color.getB() + ")";
        } else {
            return "color(" + color.getPaletteIndex() + ")";
        }
    }

    private static String formatBitmapRef(Datum datum, DirPlayer player) {
        int bitmapRef = datum.getBitmapRefValue();
        Bitmap bitmap = player.bitmapManager.getBitmap(bitmapRef);
        if (bitmap != null) {
            return "<bitmap " + bitmap.getWidth() + "x" + bitmap.getHeight() + "x" + bitmap.getBitDepth() + ">";
        }
        return "<bitmap>";
    }

    private static String formatPaletteRef(Datum datum) {
        // TODO: Implement palette ref formatting
        return "<palette>";
    }
}
