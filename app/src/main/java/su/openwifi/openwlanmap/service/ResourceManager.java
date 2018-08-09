package su.openwifi.openwlanmap.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class ResourceManager extends Thread {
  private static final String LOG_TAG = ResourceManager.class.getName();
  private static int RESOURCE_WATCH_INTERVAL = 30000;
  public static int allowBattery;
  public static int allowNoLocation;
  public static long lastLocationTime;
  public boolean running = true;
  private Context context;

  /**
   * Constructor.
   * @param context : app context
   */
  public ResourceManager(Context context) {
    this.context = context;
    final SharedPreferences sharedPreferences = PreferenceManager
        .getDefaultSharedPreferences(this.context);
    final String pref_battery = sharedPreferences.getString("pref_battery", "");
    allowBattery = Integer.parseInt(pref_battery);
    final String pref_kill_ap_no_gps = sharedPreferences.getString("pref_kill_ap_no_gps", "");
    allowNoLocation = Integer.parseInt(pref_kill_ap_no_gps);
  }

  @Override
  public void run() {
    while (running) {
      Log.e(LOG_TAG, "allowBattery="+allowBattery+"allowLocation="+allowNoLocation);
      IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
      Intent batteryStatus = context.registerReceiver(null, ifilter);
      int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
      int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
      int batteryPct = (int) (level / (float) scale * 100);
      boolean existOnLowBattery = allowBattery != 0 && batteryPct < allowBattery;
      boolean existOnNoLocation = allowNoLocation != 0
          && (SystemClock.elapsedRealtime() - lastLocationTime) > allowNoLocation * 60 * 1000;
      if (existOnLowBattery || existOnNoLocation) {
        Config.setMode(Config.MODE.KILL_MODE);
      }
      try {
        Thread.sleep(RESOURCE_WATCH_INTERVAL);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
