package de.tudarmstadt.informatik.bp.bonfirechat.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.tudarmstadt.informatik.bp.bonfirechat.R;
import de.tudarmstadt.informatik.bp.bonfirechat.data.BonfireAPI;
import de.tudarmstadt.informatik.bp.bonfirechat.data.BonfireData;
import de.tudarmstadt.informatik.bp.bonfirechat.helper.DateHelper;
import de.tudarmstadt.informatik.bp.bonfirechat.helper.StreamHelper;
import de.tudarmstadt.informatik.bp.bonfirechat.network.BluetoothProtocol;
import de.tudarmstadt.informatik.bp.bonfirechat.network.GcmProtocol;
import de.tudarmstadt.informatik.bp.bonfirechat.network.WifiProtocol;
import de.tudarmstadt.informatik.bp.bonfirechat.routing.TracerouteSegment;

/**
 * Created by johannes on 05.05.15.
 */
public class Message implements Serializable {

    public final UUID uuid;

    public final List<Contact> recipients;
    public final IPublicIdentity sender;
    public String body;
    public final Date sentTime;
    public String error;
    public int flags;
    public int retransmissionCount;

    public List<TracerouteSegment> traceroute;

    public static final int FLAG_IS_FILE = 1;
    public static final int FLAG_ACKNOWLEDGED = 2;
    public static final int FLAG_ENCRYPTED = 4;
    public static final int FLAG_PROTO_BT = 16;
    public static final int FLAG_PROTO_WIFI = 32;
    public static final int FLAG_PROTO_CLOUD = 64;
    public static final int FLAG_FAILED = 128;
    public static final int FLAG_IS_LOCATION = 256;
    public static final int FLAG_ROUTING_DSR = 512;
    public static final int FLAG_ROUTING_FLOODING = 1024;


    public Message(String body, IPublicIdentity sender, Date sentTime, int flags, Contact recipient) {
        this(body, sender, sentTime, flags, UUID.randomUUID(), recipient, new ArrayList<TracerouteSegment>());
    }
    public Message(String body, IPublicIdentity sender, Date sentTime, int flags, UUID uuid) {
        this(body, sender, sentTime, flags, uuid, null, new ArrayList<TracerouteSegment>());
    }
    public Message(String body, IPublicIdentity sender, Date sentTime, int flags, UUID uuid, Contact recipient, List<TracerouteSegment> traceroute) {
        this.sender = sender; this.recipients = new ArrayList<>();
        this.body = body; this.sentTime = sentTime; this.uuid = uuid;
        this.flags = flags;
        if (recipient != null) {
            this.recipients.add(recipient);
        }
        this.traceroute = traceroute;
    }

    public MessageDirection direction() {
        return (sender instanceof Identity) ? MessageDirection.Sent : MessageDirection.Received;
    }

    /**
     * returns placeholder descriptions for images and locations
     * @param context
     * @return
     */
    public String getDisplayBody(Context context) {
        if (hasFlag(Message.FLAG_IS_FILE)) {
            return context.getString(R.string.image);
        } else if (hasFlag(Message.FLAG_IS_LOCATION)) {
            return context.getString(R.string.location);
        } else {
            return body;
        }
    }

    @Override
    public String toString() {
        return body;
    }

    public File getImageFile() {
        return Message.getImageFile(this.uuid);
    }
    public static File getImageFile(UUID uuid) {
        Log.d("xxx", "storage dir:" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                BonfireAPI.DOWNLOADS_DIRECTORY + uuid.toString() + ".jpg");
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        if (this.sender != null && this.sender instanceof Contact) {
            values.put("sender", ((Contact) this.sender).rowid);
        }
        if (this.sender != null && this.sender instanceof Identity) {
            values.put("sender", -1);
        }
        values.put("body", body);
        values.put("sentDate", DateHelper.formatDateTime(this.sentTime));
        values.put("uuid", uuid.toString());
        values.put("flags", flags);
        values.put("traceroute", StreamHelper.serialize(((ArrayList<TracerouteSegment>) traceroute)));
        values.put("retries", retransmissionCount);
        values.put("error", error);
        return values;
    }

    public void setTransferProtocol(Class theClass) {
        flags &= ~(FLAG_PROTO_BT | FLAG_PROTO_WIFI | FLAG_PROTO_CLOUD);
        if (theClass.equals(GcmProtocol.class)) {
            flags |= FLAG_PROTO_CLOUD;
        }
        if (theClass.equals(BluetoothProtocol.class)) {
            flags |= FLAG_PROTO_BT;
        }
        if (theClass.equals(WifiProtocol.class)) {
            flags |= FLAG_PROTO_WIFI;
        }
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
        Conversation conversation = db.getConversationById(cursor.getInt(cursor.getColumnIndex("conversation")));
        ArrayList<TracerouteSegment> traceroute = StreamHelper.deserialize(cursor.getBlob(cursor.getColumnIndex("traceroute")));
        Message message = new Message(cursor.getString(cursor.getColumnIndex("body")),
                peer,
                date,
                cursor.getInt(cursor.getColumnIndex("flags")),
                UUID.fromString(cursor.getString(cursor.getColumnIndex("uuid"))),
                conversation.getPeer(),
                traceroute);
        message.retransmissionCount = cursor.getInt(cursor.getColumnIndex("retries"));
        message.error = cursor.getString(cursor.getColumnIndex("error"));
        return message;
    }

    public boolean hasFlag(int flag) {
        return (flags & flag) != 0;
    }

    public void addTracerouteSegment(TracerouteSegment segment) {
        traceroute.add(segment);
    }

    public enum MessageDirection {
        Unknown,
        Sent,
        MessageDirection, Received
    }

}
