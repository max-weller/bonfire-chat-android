package de.tudarmstadt.informatik.bp.bonfirechat.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.informatik.bp.bonfirechat.models.Conversation;
import de.tudarmstadt.informatik.bp.bonfirechat.R;

/**
 * Created by johannes on 05.05.15.
 */
public class ConversationsAdapter extends ArrayAdapter<Conversation> {
    private final Context context;
    private List<Conversation> objects;

    boolean[] itemSelected;
    public List<Conversation> getObjects(){
        return objects;
    }

    public ConversationsAdapter(Context context, List<Conversation> objects) {
        super(context, R.layout.conversations_layout, objects);
        this.context = context;
        this.objects = new ArrayList<>(objects);
        itemSelected = new boolean[this.objects.size()];
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (itemSelected.length != this.getCount())
            itemSelected = new boolean[this.getCount()];
    }

    class ViewHolder {
        ImageView icon;
        TextView name;
        TextView lastMessage;
        TextView lastMessageDate;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder v;
        if (convertView == null) {
            v = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.conversations_layout, parent, false);
            v.icon = (ImageView) convertView.findViewById(R.id.icon);
            v.name = (TextView) convertView.findViewById(R.id.name);
            v.lastMessage = (TextView) convertView.findViewById(R.id.lastMessage);
            v.lastMessageDate = (TextView) convertView.findViewById(R.id.lastMessageDate);
        } else {
            v = (ViewHolder) convertView.getTag();
        }

        v.icon.setImageResource(R.mipmap.ic_launcher);
        v.name.setText(getItem(position).getName());
        v.lastMessage.setText(getItem(position).getLastMessage());
        v.lastMessageDate.setText(getItem(position).getLastMessageDate());
        Log.d("ConversationsAdapter", "getview position=" + position + "   selected=" + itemSelected[position]);
        convertView.setSelected(itemSelected[position]);
        convertView.setBackgroundColor(itemSelected[position] ? Color.parseColor("#ffbbff") : Color.TRANSPARENT);

        return convertView;
    }
}
