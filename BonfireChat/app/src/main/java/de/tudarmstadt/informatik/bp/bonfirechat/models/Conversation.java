package de.tudarmstadt.informatik.bp.bonfirechat.models;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by johannes on 05.05.15.
 */
public class Conversation {
    private final Contact peer;
    private final ArrayList<Message> messages;
    public long rowid;
    public String title;
    public ConversationType conversationType = ConversationType.Single;

    public enum ConversationType {
        Single,
        GroupChat
    }

    public Conversation(Contact peer, String title, int rowid, List<Message> messages) {
        this.peer = peer;
        this.title = title;
        this.messages = new ArrayList<>(messages);
        this.rowid = rowid;
    }

    public Conversation(Contact peer, String title, int rowid){
        this.peer = peer;
        this.title = title;
        this.messages = new ArrayList<>();
        this.rowid = rowid;
    }

    public void addMessages(ArrayList<Message> messages){
        this.messages.addAll(messages);
    }

    public String getLastMessage() {
        if(messages.size() > 0)
            return messages.get(messages.size() - 1).toString();
        else
            return "";
    }
    public String getName() {
        if (title != null && !title.equals("")) return title;
        if (peer != null) return peer.getNickname();
        return "(unnamed)";
    }
    public Contact getPeer() {
        return peer;
    }

    public ContentValues getContentValues(){
        ContentValues values = new ContentValues();
        if (peer != null) values.put("peer", peer.rowid);
        values.put("conversationType", conversationType.ordinal());
        values.put("title", title);
        return values;
    }

    public static Conversation fromCursor(Contact contact, Cursor cursor){
        Conversation conversation = new Conversation(contact,
               cursor.getString(cursor.getColumnIndex("title")),
               cursor.getInt(cursor.getColumnIndex("rowid")));
        conversation.conversationType = ConversationType.values()[cursor.getInt(cursor.getColumnIndex("conversationType"))];
        return conversation;
    }

}
