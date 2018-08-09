package su.openwifi.openwlanmap.service;

import static su.openwifi.openwlanmap.service.ServiceController.allowBattery;
import static su.openwifi.openwlanmap.service.ServiceController.allowNoLocation;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResourceManager extends Thread {
  private static final String LOG_TAG = ResourceManager.class.getName();
  private static int RESOURCE_WATCH_INTERVAL = 30000;
  public boolean running = true;
  private Context context;
  /**
   * Constructor.
   * @param context : app context
   */
  public ResourceManager(Context context) {
    this.context = context;
  }

  @Override
  public void run() {
    Log.e(LOG_TAG, "running...");
    while (running && (allowNoLocation != 0 || allowBattery !=0))  {
        Log.e(LOG_TAG, "allowBattery="+allowBattery+"allowLocation="+allowNoLocation);
        boolean existOnLowBattery = false;
        boolean existOnNoLocation = false;
        if(allowBattery !=0){
          IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
          Intent batteryStatus = context.registerReceiver(null, ifilter);
          int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
          int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
          int batteryPct = (int) (level / (float) scale * 100);
          existOnLowBattery = allowBattery != 0 && batteryPct < allowBattery;
        }
        if(allowNoLocation !=0){
          existOnNoLocation = allowNoLocation != 0
              && (SystemClock.elapsedRealtime() - ServiceController.lastTime) > allowNoLocation * 60 * 1000;
        }
        if (existOnLowBattery || existOnNoLocation) {
          Config.setMode(Config.MODE.KILL_MODE);
        }
        try {
          Thread.sleep(RESOURCE_WATCH_INTERVAL);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    Log.e(LOG_TAG, "stopping");
  }
}
