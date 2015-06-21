package de.tudarmstadt.informatik.bp.bonfirechat.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.tudarmstadt.informatik.bp.bonfirechat.data.BonfireData;
import de.tudarmstadt.informatik.bp.bonfirechat.helper.DateHelper;
import de.tudarmstadt.informatik.bp.bonfirechat.network.BluetoothProtocol;
import de.tudarmstadt.informatik.bp.bonfirechat.network.GcmProtocol;

/**
 * Created by johannes on 05.05.15.
 */
public class Message implements Serializable {
    public enum MessageDirection {
        Unknown,
        Sent,
        MessageDirection, Received
    }
    public List<Contact> recipients;
    public IPublicIdentity sender;
    public String body;
    public Date sentTime;
    public UUID uuid;
    public String transferProtocol;
    public String error;
    public int flags;

    public static final int FLAG_ENCRYPTED = 4;
    public static final int FLAG_PROTO_BT = 16;
    public static final int FLAG_PROTO_WIFI = 32;
    public static final int FLAG_PROTO_CLOUD = 64;

    public Message(String body, IPublicIdentity sender, Date sentTime, int flags, Contact recipient) {
        this(body, sender, sentTime, flags, UUID.randomUUID());
        this.recipients.add(recipient);
    }

    public Message(String body, IPublicIdentity sender, Date sentTime, int flags, UUID rowid) {
        this.sender = sender; this.recipients = new ArrayList<>();
        this.body = body; this.sentTime = sentTime; this.uuid = rowid;
        this.flags = flags;
    }

    public MessageDirection direction() {
        return (sender instanceof Identity) ? MessageDirection.Sent : MessageDirection.Received;
    }

    @Override
    public String toString() {
        return body;
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        if (this.sender != null && this.sender instanceof Contact) values.put("sender", ((Contact)this.sender).rowid);
        if (this.sender != null && this.sender instanceof Identity) values.put("sender", -1);
        values.put("body", body);
        values.put("sentDate", DateHelper.formatDateTime(this.sentTime));
        values.put("uuid", uuid.toString());
        values.put("flags", flags);
        return values;
    }

    public void setTransferProtocol(Class theClass) {
        flags &= ~(FLAG_PROTO_BT | FLAG_PROTO_WIFI | FLAG_PROTO_CLOUD);
        if (theClass.equals(GcmProtocol.class)) flags |= FLAG_PROTO_CLOUD;
        if (theClass.equals(BluetoothProtocol.class)) flags |= FLAG_PROTO_BT;
        //if (theClass.equals(WifiProtocol.class)) flags |= FLAG_PROTO_WIFI;
    }

    public static Message fromCursor(Cursor cursor, BonfireData db) {
        Long contactId = cursor.getLong(cursor.getColumnIndex("sender"));
        IPublicIdentity peer = (contactId == -1) ? db.getDefaultIdentity() : db.getContactById(contactId);
        Date date;
        try {
            date = DateHelper.parseDateTime(cursor.getString(cursor.getColumnIndex("sentDate")));
        } catch (ParseException e) {
            date = new Date();
        }
        return new Message(cursor.getString(cursor.getColumnIndex("body")),
                peer,
                date,
                cursor.getInt(cursor.getColumnIndex("flags")),
                UUID.fromString(cursor.getString(cursor.getColumnIndex("uuid"))));
    }

    public boolean hasFlag(int flag) {
        return (flags & flag) != 0;
    }
}
