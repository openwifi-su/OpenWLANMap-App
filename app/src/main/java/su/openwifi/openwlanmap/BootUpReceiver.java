package su.openwifi.openwlanmap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;
import su.openwifi.openwlanmap.MainActivity;

public class BootUpReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    Toast.makeText(context, "Is share="+(sharedPreferences==null), Toast.LENGTH_SHORT).show();
    Toast.makeText(context, "pref_start="+sharedPreferences.getString("pref_start_mode", "0"), Toast.LENGTH_SHORT).show();
    if(sharedPreferences == null || sharedPreferences.getString("pref_start_mode", "0").equalsIgnoreCase("0")){
      Intent i = new Intent(context, MainActivity.class);
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(i);
    }
  }
}
