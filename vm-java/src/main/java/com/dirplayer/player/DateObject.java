package com.dirplayer.player;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Date object for Lingo date() function.
 * Port of Rust DateObject struct.
 */
public class DateObject {
    public int id;
    public LocalDateTime dateTime;

    public DateObject() {
        this.id = 0;
        this.dateTime = LocalDateTime.now();
    }

    public DateObject(int id) {
        this.id = id;
        this.dateTime = LocalDateTime.now();
    }

    public DateObject(int id, LocalDateTime dateTime) {
        this.id = id;
        this.dateTime = dateTime;
    }

    public int getYear() {
        return dateTime.getYear();
    }

    public int getMonth() {
        return dateTime.getMonthValue();
    }

    public int getDay() {
        return dateTime.getDayOfMonth();
    }

    public int getHour() {
        return dateTime.getHour();
    }

    public int getMinute() {
        return dateTime.getMinute();
    }

    public int getSecond() {
        return dateTime.getSecond();
    }

    public long getSeconds() {
        return dateTime.toEpochSecond(ZoneOffset.UTC);
    }

    public String getShortDate() {
        return String.format("%d/%d/%02d",
            dateTime.getMonthValue(),
            dateTime.getDayOfMonth(),
            dateTime.getYear() % 100);
    }

    public String getLongDate() {
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

        return String.format("%s, %s %d, %d",
            dayNames[dateTime.getDayOfWeek().getValue() - 1],
            monthNames[dateTime.getMonthValue() - 1],
            dateTime.getDayOfMonth(),
            dateTime.getYear());
    }

    public String getShortTime() {
        int hour = dateTime.getHour();
        String ampm = hour >= 12 ? "PM" : "AM";
        if (hour > 12) hour -= 12;
        if (hour == 0) hour = 12;
        return String.format("%d:%02d %s", hour, dateTime.getMinute(), ampm);
    }

    public String getLongTime() {
        int hour = dateTime.getHour();
        String ampm = hour >= 12 ? "PM" : "AM";
        if (hour > 12) hour -= 12;
        if (hour == 0) hour = 12;
        return String.format("%d:%02d:%02d %s", hour, dateTime.getMinute(), dateTime.getSecond(), ampm);
    }
}
