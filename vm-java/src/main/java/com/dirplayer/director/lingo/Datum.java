package com.dirplayer.director.lingo;

import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.bitmap.BitmapRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.CursorRef;

import java.util.ArrayList;
import java.util.List;

/**
 * The Datum class represents values in the Lingo runtime.
 * Port of Rust Datum enum.
 */
public class Datum {
    public static final Datum TRUE = new Datum(DatumType.Int, 1);
    public static final Datum FALSE = new Datum(DatumType.Int, 0);
    public static final Datum VOID = new Datum(DatumType.Void);
    public static final Datum NULL = new Datum(DatumType.Null);

    private final DatumType type;

    // Value storage - only one is used depending on type
    private int intValue;
    private double floatValue;
    private String stringValue;
    private List<Integer> listValue;  // List of DatumRef (represented as int IDs)
    private List<PropListPair> propListValue;
    private boolean sorted;
    private DatumType listType;

    // Special types
    private CastMemberRef castMemberRef;
    private int spriteRef;
    private int soundChannel;
    private int[] rectValue;
    private int[] pointValue;
    private double[] vectorValue;
    private ColorRef colorRef;
    private BitmapRef bitmapRef;
    private CursorRef cursorRef;
    private int paletteRef;

    // String chunk specific
    private StringChunkExpr stringChunkExpr;
    private int stringChunkSourceRef;
    private CastMemberRef stringChunkMemberRef;
    private boolean stringChunkSourceIsMember;

    // Timeout instance
    private String timeoutName;
    private int timeoutDuration;
    private int timeoutCallback;
    private int timeoutTarget;
    private Integer timeoutScriptInstance;

    // Xtra
    private String xtraName;
    private int xtraInstanceId;

    // Refs
    private int xmlRef;
    private int dateRef;
    private int mathRef;
    private int scriptInstanceRef;
    private int castLib;

    public Datum(DatumType type) {
        this.type = type;
    }

    public Datum(DatumType type, int value) {
        this.type = type;
        this.intValue = value;
    }

    public Datum(DatumType type, double value) {
        this.type = type;
        this.floatValue = value;
    }

    public Datum(DatumType type, String value) {
        this.type = type;
        this.stringValue = value;
    }

    // Factory methods
    public static Datum ofInt(int value) {
        return new Datum(DatumType.Int, value);
    }

    public static Datum ofFloat(double value) {
        return new Datum(DatumType.Float, value);
    }

    public static Datum ofString(String value) {
        return new Datum(DatumType.String, value);
    }

    public static Datum ofSymbol(String value) {
        return new Datum(DatumType.Symbol, value);
    }

    public static Datum ofList(List<Integer> items, boolean sorted) {
        Datum d = new Datum(DatumType.List);
        d.listValue = new ArrayList<>(items);
        d.sorted = sorted;
        d.listType = DatumType.List;
        return d;
    }

    public static Datum ofPropList(List<PropListPair> items, boolean sorted) {
        Datum d = new Datum(DatumType.PropList);
        d.propListValue = new ArrayList<>(items);
        d.sorted = sorted;
        return d;
    }

    public static Datum ofCastMember(CastMemberRef ref) {
        Datum d = new Datum(DatumType.CastMemberRef);
        d.castMemberRef = ref;
        return d;
    }

    public static Datum ofSpriteRef(int spriteNum) {
        Datum d = new Datum(DatumType.SpriteRef);
        d.spriteRef = spriteNum;
        return d;
    }

    public static Datum ofRect(int[] values) {
        Datum d = new Datum(DatumType.Rect);
        d.rectValue = values;
        return d;
    }

    public static Datum ofPoint(int[] values) {
        Datum d = new Datum(DatumType.Point);
        d.pointValue = values;
        return d;
    }

    public static Datum ofVector(double[] values) {
        Datum d = new Datum(DatumType.Vector);
        d.vectorValue = values;
        return d;
    }

    public static Datum ofSoundChannel(int channel) {
        Datum d = new Datum(DatumType.SoundChannel);
        d.soundChannel = channel;
        return d;
    }

    public static Datum ofColorRef(ColorRef color) {
        Datum d = new Datum(DatumType.ColorRef);
        d.colorRef = color;
        return d;
    }

    public static Datum ofBitmapRef(BitmapRef ref) {
        Datum d = new Datum(DatumType.BitmapRef);
        d.bitmapRef = ref;
        return d;
    }

    public static Datum ofBool(boolean value) {
        return value ? TRUE : FALSE;
    }

    public static Datum fromF64(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return ofInt((int) value);
        }
        return ofFloat(value);
    }

    // Type checking methods
    public DatumType getType() {
        return type;
    }

    public String getTypeName() {
        return type.getTypeName();
    }

    public boolean isNumber() {
        return type == DatumType.Int || type == DatumType.Float;
    }

    public boolean isInt() {
        return type == DatumType.Int;
    }

    public boolean isString() {
        return type == DatumType.String || type == DatumType.StringChunk;
    }

    public boolean isSymbol() {
        return type == DatumType.Symbol;
    }

    public boolean isList() {
        return type == DatumType.List;
    }

    public boolean isVoid() {
        return type == DatumType.Void;
    }

    public boolean isNull() {
        return type == DatumType.Null;
    }

    // Value extraction methods
    public int intValue() throws ScriptError {
        switch (type) {
            case Int:
                return intValue;
            case Float:
                return (int) floatValue;
            case String:
            case StringChunk:
                try {
                    return Integer.parseInt(stringValue);
                } catch (NumberFormatException e) {
                    return 0;
                }
            case SpriteRef:
                return spriteRef;
            case CastMemberRef:
                return castMemberRef != null ? castMemberRef.castMember : 0;
            case Symbol:
            case PaletteRef:
            case Void:
                return 0;
            default:
                throw new ScriptError("Cannot convert datum of type " + getTypeName() + " to int");
        }
    }

    public double floatValue() throws ScriptError {
        switch (type) {
            case Float:
                return floatValue;
            case Int:
                return intValue;
            case String:
            case StringChunk:
                try {
                    return Double.parseDouble(stringValue);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            case SpriteRef:
                return spriteRef;
            case Void:
                return 0.0;
            default:
                throw new ScriptError("Cannot convert datum of type " + getTypeName() + " to float");
        }
    }

    public String stringValue() throws ScriptError {
        switch (type) {
            case String:
            case StringChunk:
                return stringValue;
            case Int:
                return String.valueOf(intValue);
            case Symbol:
                return stringValue;
            case Vector:
                return String.format("[%f,%f,%f]", vectorValue[0], vectorValue[1], vectorValue[2]);
            case Rect:
                return String.format("(%d, %d, %d, %d)", rectValue[0], rectValue[1], rectValue[2], rectValue[3]);
            case ColorRef:
                return colorRef != null ? colorRef.toString() : "color(0, 0, 0)";
            case Void:
                return "VOID";
            default:
                throw new ScriptError("Cannot convert datum type " + getTypeName() + " to string");
        }
    }

    public String symbolValue() throws ScriptError {
        if (type != DatumType.Symbol) {
            throw new ScriptError("Cannot convert datum type " + getTypeName() + " to symbol");
        }
        return stringValue;
    }

    public boolean boolValue() throws ScriptError {
        switch (type) {
            case Int:
                return intValue != 0;
            case Float:
                return floatValue != 0.0;
            case Symbol:
                return true;
            case String:
            case StringChunk:
                return stringValue != null && !stringValue.isEmpty();
            case Void:
                return false;
            default:
                throw new ScriptError("Cannot convert datum of type " + getTypeName() + " to bool");
        }
    }

    public List<Integer> toList() throws ScriptError {
        if (type != DatumType.List && type != DatumType.ArgList && type != DatumType.ArgListNoRet) {
            throw new ScriptError("Cannot convert datum to list");
        }
        return listValue;
    }

    public List<PropListPair> toMap() throws ScriptError {
        if (type != DatumType.PropList) {
            throw new ScriptError("Cannot convert datum to map");
        }
        return propListValue;
    }

    public int[] toRect() throws ScriptError {
        if (type != DatumType.Rect) {
            throw new ScriptError("Cannot convert datum to rect");
        }
        return rectValue;
    }

    public int[] toPoint() throws ScriptError {
        if (type != DatumType.Point) {
            throw new ScriptError("Cannot convert datum to point");
        }
        return pointValue;
    }

    public double[] toVector() throws ScriptError {
        if (type != DatumType.Vector) {
            throw new ScriptError("Expected Vector, got " + getTypeName());
        }
        return vectorValue;
    }

    public ColorRef toColorRef() throws ScriptError {
        if (type != DatumType.ColorRef) {
            throw new ScriptError("Cannot convert datum to color ref");
        }
        return colorRef;
    }

    public int toSpriteRef() throws ScriptError {
        if (type != DatumType.SpriteRef) {
            throw new ScriptError("Cannot convert datum to sprite ref");
        }
        return spriteRef;
    }

    public CastMemberRef toMemberRef() throws ScriptError {
        if (type != DatumType.CastMemberRef) {
            throw new ScriptError("Cannot convert datum to cast member ref");
        }
        return castMemberRef;
    }

    public BitmapRef toBitmapRef() throws ScriptError {
        if (type != DatumType.BitmapRef) {
            throw new ScriptError("Cannot convert datum to bitmap ref");
        }
        return bitmapRef;
    }

    public int getScriptInstanceRef() throws ScriptError {
        if (type != DatumType.ScriptInstanceRef) {
            throw new ScriptError("Cannot convert datum to script instance id");
        }
        return scriptInstanceRef;
    }

    public int getDateRef() throws ScriptError {
        if (type != DatumType.DateRef) {
            throw new ScriptError("Cannot convert datum to date ref");
        }
        return dateRef;
    }

    public int getMathRef() throws ScriptError {
        if (type != DatumType.MathRef) {
            throw new ScriptError("Cannot convert datum to math ref");
        }
        return mathRef;
    }

    // Setters for mutable operations
    public void setListValue(List<Integer> value) {
        this.listValue = value;
    }

    public void setPropListValue(List<PropListPair> value) {
        this.propListValue = value;
    }

    public void setStringValue(String value) {
        this.stringValue = value;
    }

    public void setRectValue(int[] value) {
        this.rectValue = value;
    }

    public void setPointValue(int[] value) {
        this.pointValue = value;
    }

    public boolean isSorted() {
        return sorted;
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    public DatumType getListType() {
        return listType;
    }

    public void setListType(DatumType listType) {
        this.listType = listType;
    }

    /**
     * Property list key-value pair.
     */
    public static class PropListPair {
        public int key;
        public int value;

        public PropListPair(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    @Override
    public String toString() {
        switch (type) {
            case Int:
                return String.valueOf(intValue);
            case Float:
                return String.valueOf(floatValue);
            case String:
                return "\"" + stringValue + "\"";
            case Symbol:
                return "#" + stringValue;
            case Void:
                return "VOID";
            case Null:
                return "NULL";
            case List:
                return "[list with " + (listValue != null ? listValue.size() : 0) + " items]";
            case PropList:
                return "[propList with " + (propListValue != null ? propListValue.size() : 0) + " items]";
            default:
                return "<" + getTypeName() + ">";
        }
    }
}
