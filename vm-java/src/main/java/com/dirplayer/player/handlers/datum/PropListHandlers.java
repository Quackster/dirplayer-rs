package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.Datum.PropListPair;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.List;

/**
 * Handlers for property list (propList) datum operations.
 * Port of Rust PropListDatumHandlers and PropListUtils.
 */
public class PropListHandlers {

    /**
     * Get a built-in property from a proplist.
     */
    public static Datum getBuiltInProp(List<PropListPair> propList, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "count":
                return Datum.ofInt(propList.size());
            case "ilk":
                return Datum.ofSymbol("propList");
            default:
                throw new ScriptError("Invalid prop list built-in property " + prop);
        }
    }

    /**
     * Get index of key in proplist (-1 if not found).
     */
    private static int getKeyIndex(DirPlayer player, List<PropListPair> propList, Datum key) throws ScriptError {
        for (int i = 0; i < propList.size(); i++) {
            PropListPair pair = propList.get(i);
            Datum k = player.getDatum(pair.key);
            if (datumEqualsForLookup(player, k, key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Comparison for key lookup (handles string/symbol interchangeability).
     */
    private static boolean datumEqualsForLookup(DirPlayer player, Datum left, Datum right) throws ScriptError {
        // String-Symbol comparison
        if (left.isString() && right.isString()) {
            return left.stringValue().equals(right.stringValue());
        }
        if (left.isString() && right.isSymbol()) {
            return left.stringValue().equals(right.symbolValue());
        }
        if (left.isSymbol() && right.isString()) {
            return left.symbolValue().equals(right.stringValue());
        }
        if (left.isSymbol() && right.isSymbol()) {
            return left.symbolValue().equalsIgnoreCase(right.symbolValue());
        }
        // Symbol-Int comparison (e.g., #2 should match key 2)
        if (left.isSymbol() && right.isInt()) {
            try {
                return Integer.parseInt(left.symbolValue()) == right.intValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (left.isInt() && right.isSymbol()) {
            try {
                return left.intValue() == Integer.parseInt(right.symbolValue());
            } catch (NumberFormatException e) {
                return false;
            }
        }
        // Standard comparison
        if (left.isInt() && right.isInt()) {
            return left.intValue() == right.intValue();
        }
        if (left.isNumber() && right.isNumber()) {
            return left.floatValue() == right.floatValue();
        }
        return false;
    }

    /**
     * Get property value by key, or built-in property.
     */
    public static int getPropOrBuiltIn(DirPlayer player, List<PropListPair> propList, String key) throws ScriptError {
        // Try string key
        int keyIndex = getKeyIndex(player, propList, Datum.ofString(key));
        if (keyIndex >= 0) {
            return propList.get(keyIndex).value;
        }
        // Try symbol key
        keyIndex = getKeyIndex(player, propList, Datum.ofSymbol(key));
        if (keyIndex >= 0) {
            return propList.get(keyIndex).value;
        }
        // Try built-in properties, return void if not found
        try {
            Datum datum = getBuiltInProp(propList, key);
            return player.allocDatum(datum);
        } catch (ScriptError e) {
            return 0; // Void
        }
    }

    /**
     * Get property by key reference.
     */
    public static int getPropByKey(DirPlayer player, List<PropListPair> propList, int keyRef) throws ScriptError {
        Datum key = player.getDatum(keyRef);
        int keyIndex = getKeyIndex(player, propList, key);
        if (keyIndex < 0) {
            return 0; // Void
        }
        return propList.get(keyIndex).value;
    }

    /**
     * Set property value.
     */
    public static void setProp(DirPlayer player, int propListRef, int keyRef, int valueRef, boolean isRequired) throws ScriptError {
        Datum key = player.getDatum(keyRef);
        List<PropListPair> propList = player.getDatum(propListRef).toMap();
        int keyIndex = getKeyIndex(player, propList, key);

        if (isRequired && keyIndex < 0) {
            throw new ScriptError("Prop not found: " + player.formatDatum(key));
        }

        List<PropListPair> mutablePropList = player.getDatum(propListRef).toMap();
        if (keyIndex >= 0) {
            mutablePropList.get(keyIndex).value = valueRef;
        } else {
            boolean isSorted = player.getDatum(propListRef).isSorted();
            if (isSorted) {
                int indexToAdd = findIndexToAdd(player, mutablePropList, keyRef);
                mutablePropList.add(indexToAdd, new PropListPair(keyRef, valueRef));
            } else {
                mutablePropList.add(new PropListPair(keyRef, valueRef));
            }
        }
    }

    /**
     * Get item at index (1-based) or by key.
     */
    public static int getAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        List<PropListPair> propList = player.getDatum(datumRef).toMap();
        Datum key = player.getDatum(args.get(0));

        if (key.isInt()) {
            int index = key.intValue() - 1;
            if (index >= 0 && index < propList.size()) {
                return propList.get(index).value;
            } else {
                throw new ScriptError("Index out of range: " + index);
            }
        } else {
            return getPropByKey(player, propList, args.get(0));
        }
    }

    /**
     * Set item at index or by key.
     */
    public static int setAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum key = player.getDatum(args.get(0));
        int valueRef = args.get(1);
        List<PropListPair> propList = player.getDatum(datumRef).toMap();

        if (key.isInt()) {
            int index = key.intValue() - 1;
            if (index >= 0 && index < propList.size()) {
                propList.get(index).value = valueRef;
            } else {
                throw new ScriptError("Index out of range: " + index);
            }
        } else {
            setProp(player, datumRef, args.get(0), valueRef, false);
        }
        return 0; // Void
    }

    /**
     * Get property at index (returns key).
     */
    public static int getPropAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        List<PropListPair> propList = player.getDatum(datumRef).toMap();
        int position = player.getDatum(args.get(0)).intValue();
        return propList.get(position - 1).key;
    }

    /**
     * Add a property.
     */
    public static int addProp(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int propNameRef = args.get(0);
        int valueRef = args.get(1);

        Datum datum = player.getDatum(datumRef);
        List<PropListPair> propList = datum.toMap();
        boolean isSorted = datum.isSorted();

        if (isSorted) {
            int indexToAdd = findIndexToAdd(player, propList, propNameRef);
            propList.add(indexToAdd, new PropListPair(propNameRef, valueRef));
        } else {
            propList.add(new PropListPair(propNameRef, valueRef));
        }
        return 0; // Void
    }

    /**
     * Set optional property (setaProp).
     */
    public static int setOptProp(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        setProp(player, datumRef, args.get(0), args.get(1), false);
        return 0; // Void
    }

    /**
     * Set required property (setProp with 2 args).
     */
    public static int setRequiredProp(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        setProp(player, datumRef, args.get(0), args.get(1), true);
        return 0; // Void
    }

    /**
     * Get property (getProp).
     */
    public static int getProp(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum key = player.getDatum(args.get(0));
        if (key.isVoid()) {
            return 0; // Void
        }

        List<PropListPair> propList = player.getDatum(datumRef).toMap();
        int keyIndex = getKeyIndex(player, propList, key);

        if (keyIndex >= 0) {
            return propList.get(keyIndex).value;
        } else {
            throw new ScriptError("Unknown prop " + player.formatDatum(key) + " in prop list");
        }
    }

    /**
     * Get optional property (getaProp).
     */
    public static int getAProp(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum key = player.getDatum(args.get(0));
        List<PropListPair> propList = player.getDatum(datumRef).toMap();
        int keyIndex = getKeyIndex(player, propList, key);

        if (keyIndex >= 0) {
            return propList.get(keyIndex).value;
        }
        return 0; // Void
    }

    /**
     * Delete property by key.
     */
    public static int deleteProp(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum propName = player.getDatum(args.get(0));
        if (propName.isVoid()) {
            return player.allocDatum(Datum.ofInt(0)); // False
        }

        List<PropListPair> propList = player.getDatum(datumRef).toMap();

        if (propName.isString() || propName.isSymbol()) {
            int index = getKeyIndex(player, propList, propName);
            if (index >= 0) {
                propList.remove(index);
                return player.allocDatum(Datum.ofInt(1)); // True
            }
            return player.allocDatum(Datum.ofInt(0)); // False
        } else if (propName.isInt()) {
            int keyIndex = getKeyIndex(player, propList, propName);
            if (keyIndex >= 0) {
                propList.remove(keyIndex);
                return player.allocDatum(Datum.ofInt(1)); // True
            } else {
                // Try positional
                int position = propName.intValue();
                if (position >= 1 && position <= propList.size()) {
                    propList.remove(position - 1);
                    return player.allocDatum(Datum.ofInt(1)); // True
                }
                return player.allocDatum(Datum.ofInt(0)); // False
            }
        } else {
            throw new ScriptError("deleteProp: Prop name must be a string, int or symbol (is " + propName.typeStr() + ")");
        }
    }

    /**
     * Delete at index.
     */
    public static int deleteAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int position = player.getDatum(args.get(0)).intValue();
        List<PropListPair> propList = player.getDatum(datumRef).toMap();
        propList.remove(position - 1);
        return 0; // Void
    }

    /**
     * Find position of value (getOne).
     */
    public static int getOne(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum find = player.getDatum(args.get(0));
        List<PropListPair> propList = player.getDatum(datumRef).toMap();

        for (int i = 0; i < propList.size(); i++) {
            Datum v = player.getDatum(propList.get(i).value);
            if (datumEqualsForLookup(player, v, find)) {
                return player.allocDatum(Datum.ofInt(i + 1));
            }
        }
        return player.allocDatum(Datum.ofInt(0));
    }

    /**
     * Find position of key (findPos).
     */
    public static int findPos(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum find = player.getDatum(args.get(0));
        List<PropListPair> propList = player.getDatum(datumRef).toMap();

        for (int i = 0; i < propList.size(); i++) {
            Datum k = player.getDatum(propList.get(i).key);
            if (datumEqualsForLookup(player, k, find)) {
                return player.allocDatum(Datum.ofInt(i + 1));
            }
        }
        return 0; // Void
    }

    /**
     * Find position of value (getPos).
     */
    public static int getPos(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum find = player.getDatum(args.get(0));
        List<PropListPair> propList = player.getDatum(datumRef).toMap();

        for (int i = 0; i < propList.size(); i++) {
            Datum v = player.getDatum(propList.get(i).value);
            if (datumEqualsForLookup(player, v, find)) {
                return player.allocDatum(Datum.ofInt(i + 1));
            }
        }
        return player.allocDatum(Datum.ofInt(0));
    }

    /**
     * Get last value.
     */
    public static int getLast(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        List<PropListPair> propList = player.getDatum(datumRef).toMap();
        if (propList.isEmpty()) {
            return 0; // Void
        }
        return propList.get(propList.size() - 1).value;
    }

    /**
     * Duplicate the proplist.
     */
    public static int duplicate(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum original = player.getDatum(datumRef);
        Datum copy = original.clone();
        return player.allocDatum(copy);
    }

    /**
     * Get item count.
     */
    public static int count(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        List<PropListPair> propList = player.getDatum(datumRef).toMap();

        if (args.isEmpty()) {
            return player.allocDatum(Datum.ofInt(propList.size()));
        } else if (args.size() == 1) {
            int propValueRef = getPropByKey(player, propList, args.get(0));
            Datum propValue = player.getDatum(propValueRef);
            if (propValue.isList()) {
                return player.allocDatum(Datum.ofInt(propValue.toList().size()));
            } else if (propValue.isPropList()) {
                return player.allocDatum(Datum.ofInt(propValue.toMap().size()));
            } else {
                throw new ScriptError("Cannot get count of non-list");
            }
        } else {
            throw new ScriptError("Invalid number of arguments for count");
        }
    }

    /**
     * Sort the proplist by keys.
     */
    public static int sort(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        List<PropListPair> propList = player.getDatum(datumRef).toMap();

        propList.sort((a, b) -> {
            try {
                Datum leftKey = player.getDatum(a.key);
                Datum rightKey = player.getDatum(b.key);
                return datumCompare(player, leftKey, rightKey);
            } catch (ScriptError e) {
                return 0;
            }
        });

        player.getDatum(datumRef).setSorted(true);
        return 0; // Void
    }

    /**
     * Get property reference.
     */
    public static int getPropRef(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("getPropRef requires at least one argument");
        }

        List<PropListPair> propList = player.getDatum(datumRef).toMap();
        int propValueRef = getPropByKey(player, propList, args.get(0));

        if (args.size() >= 2) {
            Datum propValue = player.getDatum(propValueRef);
            if (propValue.isList()) {
                int index = player.getDatum(args.get(1)).intValue();
                List<Integer> items = propValue.toList();
                int actualIndex = index >= 1 ? index - 1 : 0;
                if (actualIndex >= items.size()) {
                    throw new ScriptError("Index out of bounds: " + index);
                }
                return items.get(actualIndex);
            } else {
                throw new ScriptError("Second argument to getPropRef requires first property to be a list");
            }
        }

        return propValueRef;
    }

    /**
     * Call handler on proplist datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "getat":
                return getAt(player, datumRef, args);
            case "setat":
                return setAt(player, datumRef, args);
            case "sort":
                return sort(player, datumRef, args);
            case "getpropat":
                return getPropAt(player, datumRef, args);
            case "addprop":
                return addProp(player, datumRef, args);
            case "setaprop":
                return setOptProp(player, datumRef, args);
            case "setprop":
                if (args.size() == 2) {
                    return setRequiredProp(player, datumRef, args);
                } else if (args.size() == 3) {
                    // setProp with nested list/proplist
                    int propKeyRef = args.get(0);
                    int indexRef = args.get(1);
                    int valueRef = args.get(2);

                    List<PropListPair> propList = player.getDatum(datumRef).toMap();
                    int listRef = getPropByKey(player, propList, propKeyRef);
                    Datum listDatum = player.getDatum(listRef);

                    if (listDatum.isList()) {
                        int index = player.getDatum(indexRef).intValue();
                        int adjustedIndex = index == 0 ? 0 : index - 1;
                        List<Integer> listVec = listDatum.toListMut();

                        while (listVec.size() <= adjustedIndex) {
                            listVec.add(0); // Add void values
                        }
                        listVec.set(adjustedIndex, valueRef);
                        return 0; // Void
                    } else if (listDatum.isPropList()) {
                        setProp(player, listRef, indexRef, valueRef, false);
                        return 0; // Void
                    } else {
                        throw new ScriptError("Property is not a list or propList");
                    }
                } else {
                    throw new ScriptError("Invalid number of arguments for setProp: " + args.size());
                }
            case "getprop":
                return getProp(player, datumRef, args);
            case "getaprop":
                return getAProp(player, datumRef, args);
            case "deleteprop":
                return deleteProp(player, datumRef, args);
            case "deleteat":
                return deleteAt(player, datumRef, args);
            case "getone":
                return getOne(player, datumRef, args);
            case "findpos":
                return findPos(player, datumRef, args);
            case "getpos":
                return getPos(player, datumRef, args);
            case "duplicate":
                return duplicate(player, datumRef, args);
            case "getlast":
                return getLast(player, datumRef, args);
            case "count":
                return count(player, datumRef, args);
            case "getpropref":
                return getPropRef(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for prop list datum");
        }
    }

    // Helper methods

    private static int findIndexToAdd(DirPlayer player, List<PropListPair> propList, int keyRef) throws ScriptError {
        int low = 0;
        int high = propList.size();
        Datum key = player.getDatum(keyRef);

        while (low < high) {
            int mid = (low + high) / 2;
            Datum leftKey = player.getDatum(propList.get(mid).key);
            if (datumLessThan(player, leftKey, key)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        return low;
    }

    private static boolean datumLessThan(DirPlayer player, Datum left, Datum right) throws ScriptError {
        if (left.isNumber() && right.isNumber()) {
            return left.floatValue() < right.floatValue();
        }
        if (left.isString() && right.isString()) {
            return left.stringValue().compareToIgnoreCase(right.stringValue()) < 0;
        }
        if (left.isSymbol() && right.isSymbol()) {
            return left.symbolValue().compareToIgnoreCase(right.symbolValue()) < 0;
        }
        return false;
    }

    private static int datumCompare(DirPlayer player, Datum left, Datum right) throws ScriptError {
        if (datumEqualsForLookup(player, left, right)) {
            return 0;
        } else if (datumLessThan(player, left, right)) {
            return -1;
        } else {
            return 1;
        }
    }
}
