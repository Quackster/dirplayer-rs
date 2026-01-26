package com.dirplayer.director;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;

/**
 * Static datum for storing compile-time values (property defaults, etc.).
 * Port of Rust StaticDatum struct.
 */
public class StaticDatum {
    private final DatumType type;
    private int intValue;
    private double floatValue;
    private String stringValue;

    private StaticDatum(DatumType type) {
        this.type = type;
    }

    public static StaticDatum from(Datum datum) {
        StaticDatum result = new StaticDatum(datum.getType());
        switch (datum.getType()) {
            case Int:
                try {
                    result.intValue = datum.intValue();
                } catch (Exception e) {
                    result.intValue = 0;
                }
                break;
            case Float:
                try {
                    result.floatValue = datum.floatValue();
                } catch (Exception e) {
                    result.floatValue = 0.0;
                }
                break;
            case String:
            case Symbol:
                try {
                    result.stringValue = datum.stringValue();
                } catch (Exception e) {
                    result.stringValue = "";
                }
                break;
            default:
                break;
        }
        return result;
    }

    public static StaticDatum ofVoid() {
        return new StaticDatum(DatumType.Void);
    }

    public static StaticDatum ofInt(int value) {
        StaticDatum result = new StaticDatum(DatumType.Int);
        result.intValue = value;
        return result;
    }

    public static StaticDatum ofFloat(double value) {
        StaticDatum result = new StaticDatum(DatumType.Float);
        result.floatValue = value;
        return result;
    }

    public static StaticDatum ofString(String value) {
        StaticDatum result = new StaticDatum(DatumType.String);
        result.stringValue = value;
        return result;
    }

    public DatumType getType() {
        return type;
    }

    public Datum toDatum() {
        switch (type) {
            case Int:
                return Datum.ofInt(intValue);
            case Float:
                return Datum.ofFloat(floatValue);
            case String:
                return Datum.ofString(stringValue);
            case Symbol:
                return Datum.ofSymbol(stringValue);
            case Void:
            default:
                return Datum.VOID;
        }
    }

    public int getIntValue() {
        return intValue;
    }

    public double getFloatValue() {
        return floatValue;
    }

    public String getStringValue() {
        return stringValue;
    }
}
