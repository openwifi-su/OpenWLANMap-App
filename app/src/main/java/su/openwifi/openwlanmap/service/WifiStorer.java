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
  private Context context;

  public WifiStorer(Context context, DataQueue buffer) {
    this.context = context;
    this.buffer = buffer;
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
        readWholeDatabase();
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void readWholeDatabase() {
    Log.i(TAG, "READING DATABASE..................................................");
    List<AccessPoint> allDataEntries = MyDatabase.getInstance(context)
        .getAccessPointDao()
        .getAllDataEntries();

    Log.i(TAG, "db = " + allDataEntries.size() + "-------------------------------");

    /* print out all entries
        for(AccessPoint entry: allDataEntries){
            Log.i("ENTRY=",entry.toString());
        }
    */

    Log.i(TAG, "Ending READING DATABASE............................................");

  }
}
