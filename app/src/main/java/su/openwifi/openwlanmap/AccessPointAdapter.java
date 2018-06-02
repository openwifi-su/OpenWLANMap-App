package su.openwifi.openwlanmap;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by tali on 18.05.18.
 */

public class AccessPointAdapter extends ArrayAdapter<AccessPoint> {
    public AccessPointAdapter(@NonNull Activity activity,ArrayList<AccessPoint> list) {
        super(activity, 0, list);
    }

    private int getColor(int strength) {
        if (strength > -30) {
            return ContextCompat.getColor(getContext(), R.color.signal_level_0);
        } else if (strength > -40) {
            return ContextCompat.getColor(getContext(), R.color.signal_level_1);
        } else if (strength > -60) {
            return ContextCompat.getColor(getContext(), R.color.signal_level_2);
        } else if (strength > -80) {
            return ContextCompat.getColor(getContext(), R.color.signal_level_3);
        } else {
            return ContextCompat.getColor(getContext(), R.color.signal_level_4);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext())
                    .inflate(R.layout.access_point_layout, parent, false);
        }

        AccessPoint ap = getItem(position);

        TextView rssi = view.findViewById(R.id.rssi);
        TextView bssid = view.findViewById(R.id.bssid);
        TextView ssid = view.findViewById(R.id.ssid);
        TextView freq = view.findViewById(R.id.frequency);
        TextView timestamp = view.findViewById(R.id.timestamp);

        rssi.setText(String.valueOf(ap.getRssid()));
        GradientDrawable circle = (GradientDrawable) rssi.getBackground();
        circle.setColor(getColor(ap.getRssid()));
        bssid.setText(ap.getBssid());
        ssid.setText(ap.getSsid());
        freq.setText(String.valueOf(ap.getFrequency())+"GHz");
        timestamp.setText(Utils.SIMPLE_DATE_FORMAT.format(
                new Date(ap.getTimestamp())
        ));
        return view;
    }
}
