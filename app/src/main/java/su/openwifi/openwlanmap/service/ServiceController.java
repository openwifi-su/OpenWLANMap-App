package su.openwifi.openwlanmap.service;

import static su.openwifi.openwlanmap.MainActivity.R_GEO_INFO;
import static su.openwifi.openwlanmap.MainActivity.R_LIST_AP;
import static su.openwifi.openwlanmap.MainActivity.R_NEWEST_SCAN;
import static su.openwifi.openwlanmap.MainActivity.R_RANK;
import static su.openwifi.openwlanmap.MainActivity.R_SPEED;
import static su.openwifi.openwlanmap.MainActivity.R_TOTAL_LIST;
import static su.openwifi.openwlanmap.MainActivity.R_UPDATE_DB;
import static su.openwifi.openwlanmap.MainActivity.R_UPDATE_ERROR;
import static su.openwifi.openwlanmap.MainActivity.R_UPDATE_RANKING;
import static su.openwifi.openwlanmap.MainActivity.R_UPDATE_UI;
import static su.openwifi.openwlanmap.MainActivity.R_UPLOAD_ERROR;
import static su.openwifi.openwlanmap.MainActivity.R_UPLOAD_MSG;
import static su.openwifi.openwlanmap.MainActivity.R_UPLOAD_UNDER_LIMIT;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import su.openwifi.openwlanmap.AccessPoint;
import su.openwifi.openwlanmap.QueryUtils;

/**
 * Created by tali on 01.06.18.
 */

public class ServiceController extends Service implements Runnable, Observer {
  private static final long SCAN_PERIOD = 2000;
  private static final String LOG_TAG = ServiceController.class.getSimpleName();
  private static final int BUFFER_ENTRY_MAX = 50;
  private static final long MIN_UPLOAD_ALLOWED = 250;
  private static final float MAX_RADIUS = 98;
  private static final double OVER = 180;
  private boolean running = true;
  private WifiLocator simpleWifiLocator;
  private double lastLat = 190;
  private double lastLon = 190;
  private float lastSpeed = -1f;
  private long lastTime = SystemClock.elapsedRealtime();
  private Thread controller = null;
  private ArrayList<AccessPoint> listAp = new ArrayList<>();
  private Intent intent;
  private Thread storer = null;
  private DataQueue buffer = new DataQueue();
  private boolean getLocation = true;
  private WifiUploader uploader;
  private TotalApWrapper totalAps = new TotalApWrapper();
  private SharedPreferences sharedPreferences;

  @Override
  public void run() {
    while (running) {
      switch (Config.getMode()) {
        case UPLOAD_MODE:
          Log.i(LOG_TAG, "Uploading...");
          while (!getLocation) {
            Log.i(LOG_TAG, "Waiting for wlocator to finish job = " + getLocation);
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
          if (totalAps.getTotalAps() < MIN_UPLOAD_ALLOWED) {
            intent = new Intent();
            intent.setAction(R_UPLOAD_UNDER_LIMIT);
            sendBroadcast(intent);
          } else {
            String id = "";
            String tag = "";
            final boolean pref_privacy = sharedPreferences.getBoolean("pref_privacy", false);
            final boolean pref_in_team = sharedPreferences.getBoolean("pref_in_team", false);
            if(!pref_privacy){
              if(pref_in_team){
                id = sharedPreferences.getString("pref_team","");
              }else{
                id = sharedPreferences.getString("own_bssid","");
              }
              tag = sharedPreferences.getString("pref_team_tag","");
            }
            final Set<String> pref_support_project = sharedPreferences.getStringSet("pref_support_project", new HashSet<String>());
            boolean uploaded = uploader.upload(id, tag, pref_support_project);
            if (uploaded) {
              //update ranking
              QueryUtils.RankingObject ranking = uploader.getRanking();
              Log.e(LOG_TAG, "Getting ranking ob=" + ranking.toString());
              intent = new Intent();
              intent.setAction(R_UPDATE_RANKING);
              intent.putExtra(R_RANK, ranking);
              sendBroadcast(intent);
            } else {
              intent = new Intent();
              intent.setAction(R_UPLOAD_ERROR);
              intent.putExtra(R_UPLOAD_MSG, uploader.getError());
              sendBroadcast(intent);
            }
          }
          Config.setMode(Config.MODE.SCAN_MODE);
          break;
        case SCAN_MODE:
          if(getLocation){
            getLocation = false;
            Log.i(LOG_TAG, "Scanning thread is running...");
            simpleWifiLocator.wlocRequestPosition();
            try {
              Thread.sleep(SCAN_PERIOD);
            } catch (InterruptedException e) {
              Log.i(LOG_TAG, "Do next request...");
              Thread.currentThread().interrupt();
            }
          }
          break;
        default:
          break;
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
    totalAps.addObserver(this);
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    controller = new Thread(this);
    controller.start();
    storer = new WifiStorer(this, buffer, totalAps);
    storer.start();
    uploader = new WifiUploader(this, totalAps);
    Log.i(LOG_TAG, "starting scan thread " + controller.isAlive());
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
    if (controller != null && controller.isAlive()) {
      controller.interrupt();
      try {
        controller.join();
      } catch (InterruptedException e) {
        Log.i(LOG_TAG, "Error join scan thread");
        Thread.currentThread().interrupt();
      }
      controller = null;
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
    Log.i(LOG_TAG, "Saving " + listAp.size() + " before destroying service");
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private boolean qualityCheck(double lat, double lon, float radius) {
    Log.e(LOG_TAG, "check quality");
    switch (simpleWifiLocator.getLastLocMethod()){
      case LIBWLOCATE:
        if(lastLon <OVER && lastLat < OVER ){
          float[] dist = new float[1];
          Location.distanceBetween(lastLat,lastLon,lat, lon,dist);
          lastSpeed = dist[0]*1000 / (SystemClock.elapsedRealtime() - lastTime);
        }else{
          lastSpeed = -1f;
        }
        lastLat = lat;
        lastLon = lon;
        lastTime = SystemClock.elapsedRealtime();
        return true;
      case GPS:
        Log.e(LOG_TAG, "in gps");
        if (radius < MAX_RADIUS) {
          lastLat = lat;
          lastLon = lon;
          lastSpeed = simpleWifiLocator.getLastSpeed();
          Log.e(LOG_TAG, "get last speed"+lastSpeed);
          lastTime = SystemClock.elapsedRealtime();
          return true;
        }
        break;
      default:
        break;
    }
    return false;
  }

  @Override
  public void update(Observable o, Object arg) {
    intent = new Intent();
    intent.setAction(R_UPDATE_DB);
    intent.putExtra(R_TOTAL_LIST, totalAps.getTotalAps());
    sendBroadcast(intent);
  }

  public class SimpleWifiLocator extends WifiLocator {

    public SimpleWifiLocator(Context context) {
      super(context);
    }

    @Override
    protected void wlocReturnPosition(WLOC_REPONSE_CODE ret, double lat, double lon, float radius, short ccode) {
      Log.i(LOG_TAG, "Getting back lat-lon = " + lat + "-" + lon);
      intent = new Intent();
      if (ret == WLOC_REPONSE_CODE.OK && qualityCheck(lat, lon, radius)) {
        AccessPoint ap;
        List<ScanResult> resultList = simpleWifiLocator.getWifiScanResult();
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
        intent.putExtra(R_SPEED, lastSpeed);
        intent.putParcelableArrayListExtra(R_LIST_AP, listAp);
        Log.i(LOG_TAG, "Notify ui update");
      } else {
        intent.setAction(R_UPDATE_ERROR);
        lastLon = 0.0;
        lastLon = 0.0;
      }
      sendBroadcast(intent);
      getLocation = true;
      Log.i(LOG_TAG, "Getting result###################################");
      //controller.interrupt();
    }
  }
}
