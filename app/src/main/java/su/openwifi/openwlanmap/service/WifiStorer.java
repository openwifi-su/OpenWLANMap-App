package su.openwifi.openwlanmap.service;

import android.content.Context;
import android.util.Log;
import java.util.List;
import su.openwifi.openwlanmap.AccessPoint;
import su.openwifi.openwlanmap.database.MyDatabase;

/**
 * This class stores the access point in sqlite database.
 */

public class WifiStorer extends Thread {
  private static String TAG = WifiStorer.class.getName();
  private DataQueue buffer;
  private TotalApWrapper totalAps;
  private Context context;

  /**
   * WifiStorer Constructor.
   * @param context : app context
   * @param buffer  : buffer of access point
   * @param totalAps  : total ap in database
   */
  public WifiStorer(Context context, DataQueue buffer, TotalApWrapper totalAps) {
    this.context = context;
    this.buffer = buffer;
    this.totalAps = totalAps;
  }

  @Override
  public void run() {
    while (true) {
      List<AccessPoint> list = buffer.getNextData();
      if (list != null) {
        Log.i(TAG, "Now inserting ......");
        MyDatabase.getInstance(context)
            .getAccessPointDao()
            .insertOrUpdateIfExisted(list);
        Log.i(TAG, "Finish inserting ......");
        long i = MyDatabase.getInstance(context)
            .getAccessPointDao()
            .countEntries();
        totalAps.setTotalAps(i);
        Log.i(TAG, "db entries count=" + i);
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
