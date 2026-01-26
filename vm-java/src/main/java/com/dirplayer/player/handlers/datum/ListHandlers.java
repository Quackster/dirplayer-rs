package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Handlers for list datum operations.
 * Port of Rust ListDatumHandlers and ListDatumUtils.
 */
public class ListHandlers {

    /**
     * Get a built-in property from a list.
     */
    public static Datum getBuiltInProp(List<Integer> listVec, String propName) throws ScriptError {
        switch (propName.toLowerCase()) {
            case "count":
            case "length":
                return Datum.ofInt(listVec.size());
            case "ilk":
                return Datum.ofSymbol("list");
            default:
                throw new ScriptError("No property " + propName + " for list datum");
        }
    }

    /**
     * Get a property from a list datum.
     */
    public static int getProp(DirPlayer player, int datumRef, String propName) throws ScriptError {
        List<Integer> listVec = player.getDatum(datumRef).toList();
        Datum result = getBuiltInProp(listVec, propName);
        return player.allocDatum(result);
    }

    /**
     * Get item at index (1-based).
     */
    public static int getAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        List<Integer> listVec = datum.toList();
        int indexValue = player.getDatum(args.get(0)).intValue();

        // Regular Lingo lists use 1-based indexing
        int position = indexValue - 1;

        if (position < 0 || position >= listVec.size()) {
            throw new ScriptError("Index out of bounds: " + position + " (list length: " + listVec.size() + ")");
        }

        return listVec.get(position);
    }

    /**
     * Set item at index (1-based).
     */
    public static int setAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int position = player.getDatum(args.get(0)).intValue();
        Datum datum = player.getDatum(datumRef);
        List<Integer> listVec = datum.toListMut();
        int index = position - 1;
        int itemRef = args.get(1);

        if (index < listVec.size()) {
            listVec.set(index, itemRef);
        } else {
            // Pad with void values
            int paddingSize = index - listVec.size();
            for (int i = 0; i < paddingSize; i++) {
                listVec.add(0); // Void
            }
            listVec.add(itemRef);
        }
        return 0; // Void
    }

    /**
     * Get item count.
     */
    public static int count(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        List<Integer> listVec = player.getDatum(datumRef).toList();
        return player.allocDatum(Datum.ofInt(listVec.size()));
    }

    /**
     * Get last item.
     */
    public static int getLast(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        List<Integer> listVec = player.getDatum(datumRef).toList();
        if (listVec.isEmpty()) {
            return 0; // Void
        }
        return listVec.get(listVec.size() - 1);
    }

    /**
     * Find position of item in list (returns 1-based index or 0 if not found).
     */
    public static int getOne(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum findDatum = player.getDatum(args.get(0));
        List<Integer> listVec = player.getDatum(datumRef).toList();

        for (int i = 0; i < listVec.size(); i++) {
            Datum itemDatum = player.getDatum(listVec.get(i));
            if (datumEquals(player, itemDatum, findDatum)) {
                return player.allocDatum(Datum.ofInt(i + 1));
            }
        }
        return player.allocDatum(Datum.ofInt(0));
    }

    /**
     * Find position of item (same as getOne).
     */
    public static int findPos(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        return getOne(player, datumRef, args);
    }

    /**
     * Add item to list (at sorted position if sorted, else at end).
     */
    public static int add(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (datum.isVoid()) {
            return 0; // Void
        }

        int itemRef = args.get(0);
        List<Integer> listVec = datum.toListMut();
        boolean isSorted = datum.isSorted();

        if (isSorted) {
            int indexToAdd = findIndexToAdd(player, listVec, itemRef);
            listVec.add(indexToAdd, itemRef);
        } else {
            listVec.add(itemRef);
        }
        return 0; // Void
    }

    /**
     * Append item to end of list.
     */
    public static int append(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (datum.isVoid()) {
            return 0; // Void
        }

        int itemRef = args.get(0);
        List<Integer> listVec = datum.toListMut();
        listVec.add(itemRef);
        return 0; // Void
    }

    /**
     * Add item at specific position (1-based).
     */
    public static int addAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum datum = player.getDatum(datumRef);
        if (datum.isVoid() || !datum.isList()) {
            return 0; // Void
        }

        int position = player.getDatum(args.get(0)).intValue() - 1;
        int itemRef = args.get(1);
        List<Integer> listVec = datum.toListMut();
        listVec.add(position, itemRef);
        return 0; // Void
    }

    /**
     * Delete item by value.
     */
    public static int deleteOne(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int searchRef = args.get(0);
        Datum searchDatum = player.getDatum(searchRef);
        List<Integer> listVec = player.getDatum(datumRef).toList();

        Integer indexToRemove = null;
        for (int i = 0; i < listVec.size(); i++) {
            int listItemRef = listVec.get(i);
            // Check reference equality first
            if (listItemRef == searchRef) {
                indexToRemove = i;
                break;
            }
            // Then check value equality
            Datum listItemDatum = player.getDatum(listItemRef);
            if (datumEquals(player, listItemDatum, searchDatum)) {
                indexToRemove = i;
                break;
            }
        }

        if (indexToRemove != null) {
            List<Integer> mutableList = player.getDatum(datumRef).toListMut();
            mutableList.remove((int) indexToRemove);
            return player.allocDatum(Datum.ofInt(1)); // True
        }
        return player.allocDatum(Datum.ofInt(0)); // False
    }

    /**
     * Delete item at position (1-based).
     */
    public static int deleteAt(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        int position = player.getDatum(args.get(0)).intValue();
        List<Integer> listVec = player.getDatum(datumRef).toListMut();

        if (position <= listVec.size() && position >= 1) {
            listVec.remove(position - 1);
            return 0; // Void
        } else {
            throw new ScriptError("Index out of bounds");
        }
    }

    /**
     * Sort the list.
     */
    public static int sort(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        List<Integer> listVec = player.getDatum(datumRef).toListMut();

        listVec.sort((a, b) -> {
            try {
                Datum left = player.getDatum(a);
                Datum right = player.getDatum(b);
                return datumCompare(player, left, right);
            } catch (ScriptError e) {
                return 0;
            }
        });

        player.getDatum(datumRef).setSorted(true);
        return 0; // Void
    }

    /**
     * Duplicate the list.
     */
    public static int duplicate(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        Datum original = player.getDatum(datumRef);
        Datum copy = original.clone();
        return player.allocDatum(copy);
    }

    /**
     * Join list items into a string with delimiter.
     */
    public static int join(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        List<Integer> listVec = player.getDatum(datumRef).toList();

        String delimiter = "&";
        if (!args.isEmpty()) {
            Datum delimDatum = player.getDatum(args.get(0));
            if (delimDatum.isString()) {
                delimiter = delimDatum.stringValue();
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < listVec.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            Datum item = player.getDatum(listVec.get(i));
            if (item.isString()) {
                sb.append(item.stringValue());
            } else if (item.isSymbol()) {
                sb.append(item.symbolValue());
            } else if (item.isInt()) {
                sb.append(item.intValue());
            } else {
                sb.append(player.formatDatum(item));
            }
        }

        return player.allocDatum(Datum.ofString(sb.toString()));
    }

    /**
     * Get property reference.
     */
    public static int getPropRef(DirPlayer player, int datumRef, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("getPropRef requires at least one argument");
        }

        List<Integer> items = player.getDatum(datumRef).toList();
        int index = player.getDatum(args.get(0)).intValue();

        int actualIndex = index >= 1 ? index - 1 : 0;
        if (actualIndex >= items.size()) {
            throw new ScriptError("Index out of bounds: " + index);
        }

        return items.get(actualIndex);
    }

    /**
     * Call handler on list datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "count":
                return count(player, datumRef, args);
            case "getat":
                return getAt(player, datumRef, args);
            case "setat":
                return setAt(player, datumRef, args);
            case "sort":
                return sort(player, datumRef, args);
            case "getone":
                return getOne(player, datumRef, args);
            case "add":
                return add(player, datumRef, args);
            case "duplicate":
                return duplicate(player, datumRef, args);
            case "addat":
                return addAt(player, datumRef, args);
            case "getlast":
                return getLast(player, datumRef, args);
            case "append":
                return append(player, datumRef, args);
            case "deleteone":
                return deleteOne(player, datumRef, args);
            case "deleteat":
                return deleteAt(player, datumRef, args);
            case "findpos":
            case "getpos":
                return findPos(player, datumRef, args);
            case "join":
                return join(player, datumRef, args);
            case "getpropref":
                return getPropRef(player, datumRef, args);
            default:
                throw new ScriptError("No handler " + handlerName + " for list datum");
        }
    }

    // Helper methods

    private static int findIndexToAdd(DirPlayer player, List<Integer> listVec, int itemRef) throws ScriptError {
        int low = 0;
        int high = listVec.size();
        Datum item = player.getDatum(itemRef);

        while (low < high) {
            int mid = (low + high) / 2;
            Datum left = player.getDatum(listVec.get(mid));
            if (datumLessThan(player, left, item)) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        return low;
    }

    private static boolean datumEquals(DirPlayer player, Datum left, Datum right) throws ScriptError {
        if (left.getType() != right.getType()) {
            // Handle some cross-type comparisons
            if (left.isNumber() && right.isNumber()) {
                return left.floatValue() == right.floatValue();
            }
            return false;
        }

        switch (left.getType()) {
            case Int:
                return left.intValue() == right.intValue();
            case Float:
                return left.floatValue() == right.floatValue();
            case String:
                return left.stringValue().equalsIgnoreCase(right.stringValue());
            case Symbol:
                return left.symbolValue().equalsIgnoreCase(right.symbolValue());
            case Void:
                return true;
            default:
                return false;
        }
    }

    private static boolean datumLessThan(DirPlayer player, Datum left, Datum right) throws ScriptError {
        if (left.isNumber() && right.isNumber()) {
            return left.floatValue() < right.floatValue();
        }
        if (left.isString() && right.isString()) {
            return left.stringValue().compareToIgnoreCase(right.stringValue()) < 0;
        }
        return false;
    }

    private static int datumCompare(DirPlayer player, Datum left, Datum right) throws ScriptError {
        if (datumEquals(player, left, right)) {
            return 0;
        } else if (datumLessThan(player, left, right)) {
            return -1;
        } else {
            return 1;
        }
    }
}
