package su.openwifi.openwlanmap.service;

import static su.openwifi.openwlanmap.MainActivity.R_GEO_INFO;
import static su.openwifi.openwlanmap.MainActivity.R_LIST_AP;
import static su.openwifi.openwlanmap.MainActivity.R_NEWEST_SCAN;
import static su.openwifi.openwlanmap.MainActivity.R_TOTAL_LIST;
import static su.openwifi.openwlanmap.MainActivity.R_UPDATE_ERROR;
import static su.openwifi.openwlanmap.MainActivity.R_UPDATE_LAST;
import static su.openwifi.openwlanmap.MainActivity.R_UPDATE_UI;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import su.openwifi.openwlanmap.AccessPoint;
import su.openwifi.openwlanmap.database.MyDatabase;

/**
 * Created by tali on 01.06.18.
 */

public class ScanService extends Service implements Runnable {
  private static final long SCAN_PERIOD = 2000;
  private static final String LOG_TAG = ScanService.class.getSimpleName();
  private static final int BUFFER_ENTRY_MAX = 10;
  private boolean running = true;
  private WifiLocator simpleWifiLocator;
  private double lastLat;
  private double lastLon;
  private Thread scanThread = null;
  private ArrayList<AccessPoint> listAp = new ArrayList<>();
  private Intent intent;
  private Thread storageAccess = null;
  private DataQueue buffer = new DataQueue();
  private boolean getLocation = false;

  @Override
  public void run() {
    while (running) {
      if (Config.getUploadStatus()) {
        Log.i(LOG_TAG, "Uploading...");
        while (!getLocation) {
          Log.i(LOG_TAG, "Waiting for wlocator to finish job");
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        buffer.putNextData(listAp);
        while (!buffer.isEmpty()) {
          Log.i(LOG_TAG, "Waiting for storage to finish job");
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        Log.i(LOG_TAG, "Now uploading......");
        //TODO move uploading to Uploader
        MyDatabase.getInstance(this)
            .getAccessPointDao()
            .deleteAll();
        Log.i(LOG_TAG, "READING DATABASE..................................................");
        List<AccessPoint> allDataEntries = MyDatabase.getInstance(this)
            .getAccessPointDao()
            .getAllDataEntries();

        Log.i(LOG_TAG, "db = " + allDataEntries.size() + "----------------------------------");
        Log.i(LOG_TAG, "Done uploading......");

        Config.setUploadStatus(false);
      } else {
        Log.i(LOG_TAG, "Scanning thread is running...");
        simpleWifiLocator.wlocRequestPosition();
      }
      try {
        Thread.sleep(SCAN_PERIOD);
      } catch (InterruptedException e) {
        Log.i(LOG_TAG, "Do next request...");
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void onCreate() {
    Log.i(LOG_TAG, "create service");
    while (simpleWifiLocator == null) {
      simpleWifiLocator = new SimpleWifiLocator(this);
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(LOG_TAG, "on start command service");
    running = true;
    scanThread = new Thread(this);
    scanThread.start();
    storageAccess = new WifiStorer(this, buffer);
    storageAccess.start();
    Log.i(LOG_TAG, "starting scan thread " + scanThread.isAlive());
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    Log.i(LOG_TAG, "destroy service");
    running = false;
    if (simpleWifiLocator != null) {
      simpleWifiLocator.doUnregister();
      if (simpleWifiLocator.getNetThread() != null && simpleWifiLocator.getNetThread().isAlive()) {
        try {
          simpleWifiLocator.getNetThread().join();
        } catch (InterruptedException e) {
          Log.i(LOG_TAG, "Error join scan thread");
          Thread.currentThread().interrupt();
        }
        simpleWifiLocator.setNetThread(null);
      }
    }
    if (scanThread != null && scanThread.isAlive()) {
      scanThread.interrupt();
      try {
        scanThread.join();
      } catch (InterruptedException e) {
        Log.i(LOG_TAG, "Error join scan thread");
        Thread.currentThread().interrupt();
      }
      scanThread = null;
    }
    //TODO check quality and save all the unsaved APs
    intent = new Intent();
    intent.setAction(R_UPDATE_LAST);
    intent.putExtra(R_TOTAL_LIST, (long) listAp.size());
    Log.i(LOG_TAG, "Saving " + listAp.size() + " before destroying service");
    sendBroadcast(intent);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private boolean qualityCheck(double lat, double lon) {
    lastLat = lat;
    lastLon = lon;
    return true;
  }

  public class SimpleWifiLocator extends WifiLocator {

    public SimpleWifiLocator(Context context) {
      super(context);
    }

    @Override
    protected void wlocReturnPosition(int ret, double lat, double lon, float radius, short ccode) {
      Log.i(LOG_TAG, "Getting back lat-lon = " + lat + "-" + lon);
      getLocation = false;
      intent = new Intent();
      if (ret == WLOC_OK && qualityCheck(lat, lon)) {
        AccessPoint ap;
        List<ScanResult> resultList = simpleWifiLocator.getLocationInfo().wifiScanResult;
        for (ScanResult result : resultList) {
          int channel = 0;
          try {
            channel = result.channelWidth;
          } catch (NoSuchFieldError e) {
            //Some old version can not read channelwidth field
          }
          ap = new AccessPoint(
              result.BSSID.toUpperCase().replace(":", "").replace(".", ""),
              result.SSID,
              result.level,
              System.currentTimeMillis() - SystemClock.elapsedRealtime()
                  + (result.timestamp / 1000),
              result.frequency / 1000.0,
              channel,
              result.capabilities,
              lastLat,
              lastLon,
              WifiFilterer.isToUpdate(result)
          );
          if (listAp.contains(ap)) {
            int i = listAp.indexOf(ap);
            AccessPoint accessPoint = listAp.get(i);
            if (ap.getRssid() > accessPoint.getRssid()) {
              //update locate
              accessPoint.setRssid(ap.getRssid());
              accessPoint.setLat(lastLat);
              accessPoint.setLon(lastLon);
            }
          } else {
            listAp.add(ap);
          }
          if (listAp.size() >= BUFFER_ENTRY_MAX) {
            buffer.putNextData(new ArrayList<>(listAp));
            listAp.clear();
          }

        }
        intent.setAction(R_UPDATE_UI);
        intent.putExtra(R_GEO_INFO, lastLat + "-" + lastLon);
        intent.putExtra(R_NEWEST_SCAN, (long) resultList.size());
        intent.putExtra(R_TOTAL_LIST, (long) listAp.size());
        intent.putParcelableArrayListExtra(R_LIST_AP, listAp);
        Log.i(LOG_TAG, "Notify ui update");
      } else {
        intent.setAction(R_UPDATE_ERROR);
        lastLon = 0.0;
        lastLon = 0.0;
      }
      sendBroadcast(intent);
      Log.i(LOG_TAG, "Getting result###################################");
      getLocation = true;
      //scanThread.interrupt();
    }
  }
}
