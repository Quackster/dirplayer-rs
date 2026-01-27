package com.dirplayer.director.lingo;

import com.dirplayer.player.CastMemberRef;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;
import com.dirplayer.player.bitmap.BitmapRef;
import com.dirplayer.player.bitmap.PaletteRef;
import com.dirplayer.player.ColorRef;
import com.dirplayer.player.CursorRef;
import com.dirplayer.player.script.ScriptInstanceRef;

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
    private PaletteRef paletteRefValue;

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

    // Direct proplist (for inline prop lists)
    private List<DirectPropListPair> directPropListValue;

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
    public static Datum ofVoid() {
        return VOID;
    }

    public static Datum ofNull() {
        return NULL;
    }

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

    /**
     * Create a proplist from int[] pairs (used by handlers).
     */
    public static Datum ofPropList(List<int[]> items, boolean sorted, boolean _unused) {
        Datum d = new Datum(DatumType.PropList);
        d.propListValue = new ArrayList<>();
        for (int[] pair : items) {
            d.propListValue.add(new PropListPair(pair[0], pair[1]));
        }
        d.sorted = sorted;
        return d;
    }

    /**
     * Create a list with a specific type.
     */
    public static Datum ofList(DatumType listType, List<Integer> items, boolean sorted) {
        Datum d = new Datum(listType);
        d.listValue = new ArrayList<>(items);
        d.sorted = sorted;
        d.listType = listType;
        return d;
    }

    /**
     * Create a script reference.
     */
    public static Datum ofScriptRef(CastMemberRef ref) {
        Datum d = new Datum(DatumType.ScriptRef);
        d.castMemberRef = ref;
        return d;
    }

    /**
     * Create a script instance reference.
     */
    public static Datum ofScriptInstanceRef(int instanceId) {
        Datum d = new Datum(DatumType.ScriptInstanceRef);
        d.scriptInstanceRef = instanceId;
        return d;
    }

    /**
     * Create a script instance reference from a ScriptInstanceRef object.
     */
    public static Datum ofScriptInstanceRef(ScriptInstanceRef ref) {
        Datum d = new Datum(DatumType.ScriptInstanceRef);
        d.scriptInstanceRef = ref != null ? ref.instanceId : 0;
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

    /**
     * Create a rect from individual datumRefs (left, top, right, bottom).
     */
    public static Datum ofRect(int left, int top, int right, int bottom) {
        Datum d = new Datum(DatumType.Rect);
        d.rectValue = new int[] { left, top, right, bottom };
        return d;
    }

    public static Datum ofPoint(int[] values) {
        Datum d = new Datum(DatumType.Point);
        d.pointValue = values;
        return d;
    }

    /**
     * Create a point from individual datumRefs (x, y).
     */
    public static Datum ofPoint(int x, int y) {
        Datum d = new Datum(DatumType.Point);
        d.pointValue = new int[] { x, y };
        return d;
    }

    public static Datum ofVector(double[] values) {
        Datum d = new Datum(DatumType.Vector);
        d.vectorValue = values;
        return d;
    }

    /**
     * Create a vector from individual components (x, y, z).
     */
    public static Datum ofVector(double x, double y, double z) {
        Datum d = new Datum(DatumType.Vector);
        d.vectorValue = new double[] { x, y, z };
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

    /**
     * Create an RGB color from individual components.
     */
    public static Datum ofColorRef(int r, int g, int b) {
        Datum d = new Datum(DatumType.ColorRef);
        d.colorRef = ColorRef.ofRgb(r, g, b);
        return d;
    }

    /**
     * Create a palette index color.
     */
    public static Datum ofPaletteIndexColor(int index) {
        Datum d = new Datum(DatumType.ColorRef);
        d.colorRef = ColorRef.ofPaletteIndex(index);
        return d;
    }

    public static Datum ofBitmapRef(BitmapRef ref) {
        Datum d = new Datum(DatumType.BitmapRef);
        d.bitmapRef = ref;
        return d;
    }

    /**
     * Create a bitmap reference from a bitmap ID.
     */
    public static Datum ofBitmapRef(int bitmapId) {
        Datum d = new Datum(DatumType.BitmapRef);
        d.bitmapRef = new BitmapRef(bitmapId);
        return d;
    }

    /**
     * Create a palette reference datum.
     */
    public static Datum ofPaletteRef(PaletteRef ref) {
        Datum d = new Datum(DatumType.PaletteRef);
        d.paletteRefValue = ref;
        return d;
    }

    /**
     * Create a timeout factory datum.
     */
    public static Datum ofTimeoutFactory() {
        return new Datum(DatumType.TimeoutFactory);
    }

    /**
     * Create a timeout reference datum.
     */
    public static Datum ofTimeoutRef(String name) {
        Datum d = new Datum(DatumType.TimeoutRef);
        d.timeoutName = name;
        return d;
    }

    /**
     * Create a timeout instance datum.
     */
    public static Datum ofTimeoutInstance(String name, int duration, int callback, int target, Integer scriptInstance) {
        Datum d = new Datum(DatumType.TimeoutInstance);
        d.timeoutName = name;
        d.timeoutDuration = duration;
        d.timeoutCallback = callback;
        d.timeoutTarget = target;
        d.timeoutScriptInstance = scriptInstance;
        return d;
    }

    /**
     * Create an Xtra datum.
     */
    public static Datum ofXtra(String name) {
        Datum d = new Datum(DatumType.Xtra);
        d.xtraName = name;
        return d;
    }

    /**
     * Create an XML reference datum.
     */
    public static Datum ofXmlRef(int id) {
        Datum d = new Datum(DatumType.XmlRef);
        d.xmlRef = id;
        return d;
    }

    /**
     * Create a date reference datum.
     */
    public static Datum ofDateRef(int id) {
        Datum d = new Datum(DatumType.DateRef);
        d.dateRef = id;
        return d;
    }

    /**
     * Create a math reference datum.
     */
    public static Datum ofMathRef(int id) {
        Datum d = new Datum(DatumType.MathRef);
        d.mathRef = id;
        return d;
    }

    /**
     * Create a cast lib reference datum.
     */
    public static Datum ofCastLibRef(int castLibNum) {
        Datum d = new Datum(DatumType.CastLibRef);
        d.intValue = castLibNum;
        return d;
    }

    /**
     * Create a cast member reference datum.
     */
    public static Datum ofCastMemberRef(CastMemberRef ref) {
        Datum d = new Datum(DatumType.CastMemberRef);
        d.castMemberRef = ref;
        return d;
    }

    /**
     * Create a proplist datum from PropListPair list.
     */
    public static Datum ofPropListPairs(List<PropListPair> pairs, boolean sorted) {
        Datum d = new Datum(DatumType.PropList);
        d.propListValue = pairs;
        d.sorted = sorted;
        return d;
    }

    /**
     * Create a string chunk datum.
     */
    public static Datum ofStringChunk(int sourceRef, StringChunkExpr chunkExpr, String resolvedValue) {
        Datum d = new Datum(DatumType.StringChunk);
        d.stringChunkSourceRef = sourceRef;
        d.stringChunkExpr = chunkExpr;
        d.stringValue = resolvedValue;
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
        return type == DatumType.List ||
               type == DatumType.ArgList ||
               type == DatumType.ArgListNoRet;
    }

    public boolean isVoid() {
        return type == DatumType.Void;
    }

    public boolean isNull() {
        return type == DatumType.Null;
    }

    public boolean isFloat() {
        return type == DatumType.Float;
    }

    public boolean isPropList() {
        return type == DatumType.PropList;
    }

    public boolean isStringChunk() {
        return type == DatumType.StringChunk;
    }

    public boolean isSpriteRef() {
        return type == DatumType.SpriteRef;
    }

    public boolean isRect() {
        return type == DatumType.Rect;
    }

    public boolean isPoint() {
        return type == DatumType.Point;
    }

    public boolean isObject() {
        return type == DatumType.ScriptInstanceRef;
    }

    public boolean isScriptInstanceRef() {
        return type == DatumType.ScriptInstanceRef;
    }

    public boolean isScriptRef() {
        return type == DatumType.ScriptRef;
    }

    public boolean isCastMemberRef() {
        return type == DatumType.CastMemberRef;
    }

    public boolean isVector() {
        return type == DatumType.Vector;
    }

    public boolean isColorRef() {
        return type == DatumType.ColorRef;
    }

    public boolean isArgList() {
        return type == DatumType.ArgList || type == DatumType.ArgListNoRet;
    }

    /**
     * Check if this is a valid cast member reference (non-zero).
     */
    public boolean isCastMemberValid() {
        return type == DatumType.CastMemberRef && castMemberRef != null && castMemberRef.castMember > 0;
    }

    /**
     * Get the type as a human-readable string.
     */
    public String typeStr() {
        return type.getTypeName();
    }

    /**
     * Convert to float value (for math operations).
     */
    public double toFloat() throws ScriptError {
        return floatValue();
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

    /**
     * Get proplist as List<int[]> for handler compatibility.
     */
    public List<int[]> toPropList() throws ScriptError {
        if (type != DatumType.PropList) {
            throw new ScriptError("Cannot convert datum to prop list");
        }
        List<int[]> result = new ArrayList<>();
        for (PropListPair pair : propListValue) {
            result.add(new int[] { pair.key, pair.value });
        }
        return result;
    }

    /**
     * Get mutable proplist as List<int[]>.
     */
    public List<int[]> toPropListMut() throws ScriptError {
        if (type != DatumType.PropList) {
            throw new ScriptError("Cannot convert datum to prop list");
        }
        // Return a wrapper that modifies the underlying propListValue
        return new PropListIntArrayWrapper(propListValue);
    }

    /**
     * Get mutable list.
     */
    public List<Integer> toListMut() throws ScriptError {
        if (type != DatumType.List && type != DatumType.ArgList && type != DatumType.ArgListNoRet) {
            throw new ScriptError("Cannot convert datum to list");
        }
        return listValue;
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

    public CastMemberRef getScriptRef() throws ScriptError {
        if (type != DatumType.ScriptRef) {
            throw new ScriptError("Cannot get script ref from non-script datum");
        }
        return castMemberRef;
    }

    public BitmapRef toBitmapRef() throws ScriptError {
        if (type != DatumType.BitmapRef) {
            throw new ScriptError("Cannot convert datum to bitmap ref");
        }
        return bitmapRef;
    }

    public int toSoundChannel() throws ScriptError {
        if (type != DatumType.SoundChannel) {
            throw new ScriptError("Cannot convert datum to sound channel");
        }
        return soundChannel;
    }

    public int getScriptInstanceRef() throws ScriptError {
        if (type != DatumType.ScriptInstanceRef) {
            throw new ScriptError("Cannot convert datum to script instance id");
        }
        return scriptInstanceRef;
    }

    /**
     * Convert to a ScriptInstanceRef object.
     */
    public ScriptInstanceRef toScriptInstanceRef() throws ScriptError {
        if (type != DatumType.ScriptInstanceRef) {
            throw new ScriptError("Cannot convert datum to script instance ref");
        }
        return new ScriptInstanceRef(scriptInstanceRef);
    }

    /**
     * Convert to script reference (CastMemberRef).
     */
    public CastMemberRef toScriptRef() throws ScriptError {
        if (type != DatumType.ScriptRef) {
            throw new ScriptError("Cannot convert datum to script ref");
        }
        return castMemberRef;
    }

    /**
     * Convert to boolean.
     */
    public boolean toBool() throws ScriptError {
        return boolValue();
    }

    /**
     * Get the CastMemberRef (for CastMemberRef type).
     */
    public CastMemberRef toCastMemberRef() throws ScriptError {
        if (type != DatumType.CastMemberRef && type != DatumType.CastMember) {
            throw new ScriptError("Cannot convert datum to cast member ref");
        }
        return castMemberRef;
    }

    /**
     * Get point coordinates as int array [x, y].
     * Resolves datum refs using the provided player.
     */
    public int[] toPointCoords(DirPlayer player) throws ScriptError {
        if (type != DatumType.Point) {
            throw new ScriptError("Cannot get point coords from non-point");
        }
        int x = player.getDatum(pointValue[0]).intValue();
        int y = player.getDatum(pointValue[1]).intValue();
        return new int[] { x, y };
    }

    /**
     * Get rect coordinates as int array [left, top, right, bottom].
     * Resolves datum refs using the provided player.
     */
    public int[] toRectCoords(DirPlayer player) throws ScriptError {
        if (type != DatumType.Rect) {
            throw new ScriptError("Cannot get rect coords from non-rect");
        }
        int left = player.getDatum(rectValue[0]).intValue();
        int top = player.getDatum(rectValue[1]).intValue();
        int right = player.getDatum(rectValue[2]).intValue();
        int bottom = player.getDatum(rectValue[3]).intValue();
        return new int[] { left, top, right, bottom };
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

    public int getXmlRef() throws ScriptError {
        if (type != DatumType.XmlRef) {
            throw new ScriptError("Cannot convert datum to xml ref");
        }
        return xmlRef;
    }

    public PaletteRef getPaletteRefValue() throws ScriptError {
        if (type != DatumType.PaletteRef) {
            throw new ScriptError("Cannot get palette ref from non-palette-ref datum");
        }
        return paletteRefValue;
    }

    public StringChunkExpr getStringChunkExpr() throws ScriptError {
        if (type != DatumType.StringChunk) {
            throw new ScriptError("Cannot get string chunk expr from non-string-chunk");
        }
        return stringChunkExpr;
    }

    public int getStringChunkSourceRef() throws ScriptError {
        if (type != DatumType.StringChunk) {
            throw new ScriptError("Cannot get string chunk source from non-string-chunk");
        }
        return stringChunkSourceRef;
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

    public void setVectorValue(double[] value) {
        this.vectorValue = value;
    }

    /**
     * Set a specific index in a point.
     */
    public void setPointAt(int index, int value) throws ScriptError {
        if (type != DatumType.Point) {
            throw new ScriptError("Cannot setPointAt on non-point");
        }
        if (index < 0 || index >= 2) {
            throw new ScriptError("Point index out of bounds: " + index);
        }
        pointValue[index] = value;
    }

    /**
     * Set a specific index in a rect.
     */
    public void setRectAt(int index, int value) throws ScriptError {
        if (type != DatumType.Rect) {
            throw new ScriptError("Cannot setRectAt on non-rect");
        }
        if (index < 0 || index >= 4) {
            throw new ScriptError("Rect index out of bounds: " + index);
        }
        rectValue[index] = value;
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

    // Timeout instance getters/setters
    public String getTimeoutName() {
        return timeoutName;
    }

    public void setTimeoutName(String timeoutName) {
        this.timeoutName = timeoutName;
    }

    public int getTimeoutDuration() {
        return timeoutDuration;
    }

    public void setTimeoutDuration(int timeoutDuration) {
        this.timeoutDuration = timeoutDuration;
    }

    public int getTimeoutCallback() {
        return timeoutCallback;
    }

    public void setTimeoutCallback(int timeoutCallback) {
        this.timeoutCallback = timeoutCallback;
    }

    public int getTimeoutTarget() {
        return timeoutTarget;
    }

    public void setTimeoutTarget(int timeoutTarget) {
        this.timeoutTarget = timeoutTarget;
    }

    public Integer getTimeoutScriptInstance() {
        return timeoutScriptInstance;
    }

    public void setTimeoutScriptInstance(Integer timeoutScriptInstance) {
        this.timeoutScriptInstance = timeoutScriptInstance;
    }

    // Xtra instance getters/setters
    public String getXtraName() {
        return xtraName;
    }

    public void setXtraName(String xtraName) {
        this.xtraName = xtraName;
    }

    public int getXtraInstanceId() {
        return xtraInstanceId;
    }

    public void setXtraInstanceId(int xtraInstanceId) {
        this.xtraInstanceId = xtraInstanceId;
    }

    /**
     * Create an Xtra instance datum.
     */
    public static Datum ofXtraInstance(String xtraName, int instanceId) {
        Datum d = new Datum(DatumType.XtraInstance);
        d.xtraName = xtraName;
        d.xtraInstanceId = instanceId;
        return d;
    }

    /**
     * Create a property list directly from key-value pairs.
     * This is a convenience method for creating prop lists with inline Datum values.
     * Keys are strings, values are Datums.
     */
    public static Datum ofPropListDirect(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide key-value pairs");
        }

        List<DirectPropListPair> pairs = new ArrayList<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = (String) keyValuePairs[i];
            Datum value = (Datum) keyValuePairs[i + 1];
            pairs.add(new DirectPropListPair(key, value));
        }

        Datum d = new Datum(DatumType.PropList);
        d.directPropListValue = pairs;
        return d;
    }

    /**
     * Get direct proplist value (for inline prop lists).
     */
    public List<DirectPropListPair> getDirectPropListValue() {
        return directPropListValue;
    }

    /**
     * Direct property list pair with string key and Datum value.
     * Used for inline prop lists that don't go through the allocator.
     */
    public static class DirectPropListPair {
        public String key;
        public Datum value;

        public DirectPropListPair(String key, Datum value) {
            this.key = key;
            this.value = value;
        }
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

    /**
     * Clone this datum.
     */
    public Datum clone() {
        Datum d = new Datum(type);
        d.intValue = intValue;
        d.floatValue = floatValue;
        d.stringValue = stringValue;
        d.sorted = sorted;
        d.listType = listType;

        if (listValue != null) {
            d.listValue = new ArrayList<>(listValue);
        }
        if (propListValue != null) {
            d.propListValue = new ArrayList<>();
            for (PropListPair pair : propListValue) {
                d.propListValue.add(new PropListPair(pair.key, pair.value));
            }
        }
        if (rectValue != null) {
            d.rectValue = rectValue.clone();
        }
        if (pointValue != null) {
            d.pointValue = pointValue.clone();
        }
        if (vectorValue != null) {
            d.vectorValue = vectorValue.clone();
        }

        d.castMemberRef = castMemberRef;
        d.spriteRef = spriteRef;
        d.soundChannel = soundChannel;
        d.colorRef = colorRef;
        d.bitmapRef = bitmapRef;
        d.cursorRef = cursorRef;
        d.paletteRefValue = paletteRefValue;
        d.stringChunkExpr = stringChunkExpr;
        d.stringChunkSourceRef = stringChunkSourceRef;
        d.stringChunkMemberRef = stringChunkMemberRef;
        d.stringChunkSourceIsMember = stringChunkSourceIsMember;
        d.timeoutName = timeoutName;
        d.timeoutDuration = timeoutDuration;
        d.timeoutCallback = timeoutCallback;
        d.timeoutTarget = timeoutTarget;
        d.timeoutScriptInstance = timeoutScriptInstance;
        d.xtraName = xtraName;
        d.xtraInstanceId = xtraInstanceId;
        d.xmlRef = xmlRef;
        d.dateRef = dateRef;
        d.mathRef = mathRef;
        d.scriptInstanceRef = scriptInstanceRef;
        d.castLib = castLib;

        return d;
    }

    /**
     * Wrapper that allows List<int[]> access to PropListPair list.
     */
    private static class PropListIntArrayWrapper extends ArrayList<int[]> {
        private final List<PropListPair> backing;

        public PropListIntArrayWrapper(List<PropListPair> backing) {
            this.backing = backing;
        }

        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public int[] get(int index) {
            PropListPair pair = backing.get(index);
            return new int[] { pair.key, pair.value };
        }

        @Override
        public int[] set(int index, int[] element) {
            PropListPair old = backing.get(index);
            int[] oldArray = new int[] { old.key, old.value };
            backing.set(index, new PropListPair(element[0], element[1]));
            return oldArray;
        }

        @Override
        public void add(int index, int[] element) {
            backing.add(index, new PropListPair(element[0], element[1]));
        }

        @Override
        public boolean add(int[] element) {
            return backing.add(new PropListPair(element[0], element[1]));
        }

        @Override
        public int[] remove(int index) {
            PropListPair old = backing.remove(index);
            return new int[] { old.key, old.value };
        }
    }
}
