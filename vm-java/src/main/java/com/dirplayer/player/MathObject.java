package com.dirplayer.player;

/**
 * Math object for Lingo math operations.
 * Port of Rust MathObject struct.
 */
public class MathObject {
    public int id;

    public MathObject() {
        this.id = 0;
    }

    public MathObject(int id) {
        this.id = id;
    }

    // Static math methods
    public static double sin(double angle) {
        return Math.sin(Math.toRadians(angle));
    }

    public static double cos(double angle) {
        return Math.cos(Math.toRadians(angle));
    }

    public static double tan(double angle) {
        return Math.tan(Math.toRadians(angle));
    }

    public static double asin(double value) {
        return Math.toDegrees(Math.asin(value));
    }

    public static double acos(double value) {
        return Math.toDegrees(Math.acos(value));
    }

    public static double atan(double value) {
        return Math.toDegrees(Math.atan(value));
    }

    public static double atan2(double y, double x) {
        return Math.toDegrees(Math.atan2(y, x));
    }

    public static double sqrt(double value) {
        return Math.sqrt(value);
    }

    public static double pow(double base, double exponent) {
        return Math.pow(base, exponent);
    }

    public static double exp(double value) {
        return Math.exp(value);
    }

    public static double log(double value) {
        return Math.log(value);
    }

    public static double log10(double value) {
        return Math.log10(value);
    }

    public static double abs(double value) {
        return Math.abs(value);
    }

    public static int abs(int value) {
        return Math.abs(value);
    }

    public static double floor(double value) {
        return Math.floor(value);
    }

    public static double ceil(double value) {
        return Math.ceil(value);
    }

    public static double round(double value) {
        return Math.round(value);
    }

    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    public static double random() {
        return Math.random();
    }

    public static int randomInt(int max) {
        return (int) (Math.random() * max);
    }

    public static final double PI = Math.PI;
    public static final double E = Math.E;
}
