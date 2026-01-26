package com.dirplayer.player.handlers.datum;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DateObject;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.ScriptError;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Handlers for date datum operations.
 * Port of Rust DateDatumHandlers.
 */
public class DateHandlers {

    /**
     * Get a property from a date object.
     */
    public static int getProp(DirPlayer player, int datumRef, String prop) throws ScriptError {
        switch (prop.toLowerCase()) {
            case "ilk":
                return player.allocDatum(Datum.ofSymbol("date"));
            default:
                throw new ScriptError("Cannot get date property " + prop);
        }
    }

    /**
     * Set a property on a date object.
     */
    public static void setProp(DirPlayer player, int datumRef, String prop, int valueRef) throws ScriptError {
        throw new ScriptError("Cannot set date property " + prop);
    }

    /**
     * Call handler on date datum.
     */
    public static int call(DirPlayer player, int datumRef, String handlerName, List<Integer> args) throws ScriptError {
        int dateId = player.getDatum(datumRef).getDateRef();
        DateObject dateObj = player.dateObjects.get(dateId);

        if (dateObj == null) {
            throw new ScriptError("Date object " + dateId + " not found");
        }

        LocalDateTime dateTime = Instant.ofEpochMilli(dateObj.timestampMs)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        switch (handlerName.toLowerCase()) {
            case "gettime":
                return player.allocDatum(Datum.ofInt((int) dateObj.timestampMs));

            case "settime": {
                if (args.isEmpty()) {
                    throw new ScriptError("setTime requires a time argument");
                }
                long time = player.getDatum(args.get(0)).intValue();
                dateObj.timestampMs = time;
                return 0; // Void
            }

            case "getfullyear":
                return player.allocDatum(Datum.ofInt(dateTime.getYear()));

            case "getmonth":
                return player.allocDatum(Datum.ofInt(dateTime.getMonthValue() - 1)); // 0-based

            case "getdate":
                return player.allocDatum(Datum.ofInt(dateTime.getDayOfMonth()));

            case "gethours":
                return player.allocDatum(Datum.ofInt(dateTime.getHour()));

            case "getminutes":
                return player.allocDatum(Datum.ofInt(dateTime.getMinute()));

            case "getseconds":
                return player.allocDatum(Datum.ofInt(dateTime.getSecond()));

            case "setfullyear": {
                if (args.isEmpty()) {
                    throw new ScriptError("setFullYear requires a year argument");
                }
                int year = player.getDatum(args.get(0)).intValue();
                LocalDateTime newDateTime = dateTime.withYear(year);
                dateObj.timestampMs = newDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return 0; // Void
            }

            case "setmonth": {
                if (args.isEmpty()) {
                    throw new ScriptError("setMonth requires a month argument");
                }
                int month = player.getDatum(args.get(0)).intValue() + 1; // Convert 0-based to 1-based
                LocalDateTime newDateTime = dateTime.withMonth(month);
                dateObj.timestampMs = newDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return 0; // Void
            }

            case "setdate": {
                if (args.isEmpty()) {
                    throw new ScriptError("setDate requires a date argument");
                }
                int day = player.getDatum(args.get(0)).intValue();
                LocalDateTime newDateTime = dateTime.withDayOfMonth(day);
                dateObj.timestampMs = newDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return 0; // Void
            }

            case "sethours": {
                if (args.isEmpty()) {
                    throw new ScriptError("setHours requires an hours argument");
                }
                int hours = player.getDatum(args.get(0)).intValue();
                LocalDateTime newDateTime = dateTime.withHour(hours);
                dateObj.timestampMs = newDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return 0; // Void
            }

            case "setminutes": {
                if (args.isEmpty()) {
                    throw new ScriptError("setMinutes requires a minutes argument");
                }
                int minutes = player.getDatum(args.get(0)).intValue();
                LocalDateTime newDateTime = dateTime.withMinute(minutes);
                dateObj.timestampMs = newDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return 0; // Void
            }

            case "setseconds": {
                if (args.isEmpty()) {
                    throw new ScriptError("setSeconds requires a seconds argument");
                }
                int seconds = player.getDatum(args.get(0)).intValue();
                LocalDateTime newDateTime = dateTime.withSecond(seconds);
                dateObj.timestampMs = newDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return 0; // Void
            }

            default:
                throw new ScriptError("No handler " + handlerName + " for date");
        }
    }
}
