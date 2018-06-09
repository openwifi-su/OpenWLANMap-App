package su.openwifi.openwlanmap;

import static android.view.View.GONE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by tali on 18.05.18.
 */

public class AccessPointAdapter extends ArrayAdapter<AccessPoint> {
  private Context context;

  public AccessPointAdapter(@NonNull Context context, List<AccessPoint> list) {
    super(context, 0, list);
    this.context = context;
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
    TextView lat = view.findViewById(R.id.lat);
    TextView lon = view.findViewById(R.id.lon);
    ssid.setVisibility(GONE);
    freq.setVisibility(GONE);
    timestamp.setVisibility(GONE);
    lat.setVisibility(GONE);
    lon.setVisibility(GONE);

    rssi.setText(String.valueOf(ap.getRssid()));
    GradientDrawable circle = (GradientDrawable) rssi.getBackground();
    circle.setColor(getColor(ap.getRssid()));
    bssid.setText(ap.getBssid());

    SharedPreferences defaultSharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(context);
    Set<String> listSet = defaultSharedPreferences.getStringSet("pref_show_list", null);
    for (String item : listSet) {
      if (item.equalsIgnoreCase(getContext().getString(R.string.l_ssid))) {
        ssid.setText(ap.getSsid());
        ssid.setVisibility(View.VISIBLE);
      } else if (item.equalsIgnoreCase(getContext().getString(R.string.l_freq))) {
        freq.setText(String.valueOf(ap.getFrequency()) + "GHz");
        freq.setVisibility(View.VISIBLE);
      } else if (item.equalsIgnoreCase(getContext().getString(R.string.l_locate))) {
        lat.setText(String.valueOf(ap.getLat()));
        lon.setText(String.valueOf(ap.getLon()));
        lat.setVisibility(View.VISIBLE);
        lon.setVisibility(View.VISIBLE);
      } else if (item.equalsIgnoreCase(getContext().getString(R.string.l_timestamp))) {
        timestamp.setText(Utils.SIMPLE_DATE_FORMAT.format(
            new Date(ap.getTimestamp())
        ));
        timestamp.setVisibility(View.VISIBLE);
      }
    }
    return view;
  }
}
