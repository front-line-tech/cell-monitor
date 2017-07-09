package com.flt.cellmonitor.ui;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.flt.cellmonitor.R;
import com.flt.cellmonitor.data.Descriptions;
import com.flt.cellmonitor.data.NetworkUpdate;

import java.util.List;

public class NetworkUpdateListAdapter extends ArrayAdapter<NetworkUpdate> {

  private Descriptions d;

  public NetworkUpdateListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<NetworkUpdate> objects) {
    super(context, resource, objects);
    d = new Descriptions(context);
  }

  @Override
  public NetworkUpdate getItem(int position) {
    return super.getItem(getCount() - position - 1); // reverse order!
  }

  @NonNull
  @Override
  public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    NetworkUpdate item = getItem(position);

    View view;
    if (convertView == null) {
      LayoutInflater inflater = LayoutInflater.from(getContext());
      view = inflater.inflate(R.layout.entry_update, null);
    } else {
      view = convertView;
    }

    TextView message = (TextView)view.findViewById(R.id.entry_message);
    TextView time = (TextView)view.findViewById(R.id.entry_time);

    message.setText(d.describeEntryChange(item));
    time.setText(d.describeEntryTime(item));

    message.setTextColor(item.isNegative() ? Color.RED : item.isApp() ? Color.BLUE : Color.BLACK);
    time.setTextColor(item.isNegative() ? Color.RED : Color.GRAY);

    return view;
  }

}
