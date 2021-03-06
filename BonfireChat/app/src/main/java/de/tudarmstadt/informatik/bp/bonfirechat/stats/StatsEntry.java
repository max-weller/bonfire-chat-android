package de.tudarmstadt.informatik.bp.bonfirechat.stats;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.text.ParseException;
import java.util.Date;

import de.tudarmstadt.informatik.bp.bonfirechat.helper.DateHelper;

/**
 * Created by johannes on 10.07.15.
 */
public class StatsEntry {
    public Date timestamp;
    public int batteryLevel;
    public float powerUsage;
    public int messagesSent;
    public int messageReceived;
    public float lat, lng;

    public StatsEntry() {
        this(new Date(), 0, 0, 0, 0, 0, 0);
    }
    public StatsEntry(Date timestamp, int batteryLevel, float powerUsage, int messagesSent, int messageReceived, float lat, float lng) {
        this.timestamp = timestamp;
        this.batteryLevel = batteryLevel;
        this.powerUsage = powerUsage;
        this.messagesSent = messagesSent;
        this.messageReceived = messageReceived;
        this.lat = lat;
        this.lng = lng;
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put("timestamp", DateHelper.formatDateTime(timestamp));
        values.put("batterylevel", batteryLevel);
        values.put("powerusage", powerUsage);
        values.put("messages_sent", messagesSent);
        values.put("messages_received", messageReceived);
        values.put("lat", lat);
        values.put("lng", lng);
        return values;
    }

    public static StatsEntry fromCursor(Cursor cursor) {
        try {
            StatsEntry stats = new StatsEntry(
                    DateHelper.parseDateTime(cursor.getString(cursor.getColumnIndex("timestamp"))),
                    cursor.getInt(cursor.getColumnIndex("batterylevel")),
                    cursor.getFloat(cursor.getColumnIndex("powerusage")),
                    cursor.getInt(cursor.getColumnIndex("messages_sent")),
                    cursor.getInt(cursor.getColumnIndex("messages_received")),
                    cursor.getFloat(cursor.getColumnIndex("lat")),
                    cursor.getFloat(cursor.getColumnIndex("lng"))
            );
            return stats;
        } catch (ParseException e) {
            Log.e("StatsEntry", "error parsing stats entry from database");
            return new StatsEntry();
        }
    }
}
