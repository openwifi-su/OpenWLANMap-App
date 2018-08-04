package su.openwifi.openwlanmap.service;

import static su.openwifi.openwlanmap.MainActivity.ACTION_KILL_APP;
import static su.openwifi.openwlanmap.MainActivity.R_GEO_INFO;
import static su.openwifi.openwlanmap.MainActivity.R_LIST_AP;
import static su.openwifi.openwlanmap.MainActivity.R_NEWEST_SCAN;
import static su.openwifi.openwlanmap.MainActivity.R_RANK;
import static su.openwifi.openwlanmap.MainActivity.R_SPEED;
import static su.openwifi.openwlanmap.MainActivity.R_TOTAL_LIST;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPDATE_DB;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPDATE_ERROR;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPDATE_RANKING;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPDATE_UI;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPLOAD_ERROR;
import static su.openwifi.openwlanmap.MainActivity.R_UPLOAD_MSG;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPLOAD_UNDER_LIMIT;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import su.openwifi.openwlanmap.AccessPoint;
import su.openwifi.openwlanmap.utils.RankingObject;

/**
 * Created by tali on 01.06.18.
 */

public class ServiceController extends Service implements Runnable, Observer {
  private static long SCAN_PERIOD = 2000;
  private static final String LOG_TAG = ServiceController.class.getSimpleName();
  private static final int BUFFER_ENTRY_MAX = 100;
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
  private WifiStorer storer = null;
  private DataQueue buffer = new DataQueue();
  private boolean getLocation = true;
  private WifiUploader uploader;
  private TotalApWrapper totalAps = new TotalApWrapper();
  private SharedPreferences sharedPreferences;
  private ResourceManager resourceManager;

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
            intent.setAction(ACTION_UPLOAD_UNDER_LIMIT);
            sendBroadcast(intent);
          } else {
            String id = "";
            String tag = "";
            final boolean pref_privacy = sharedPreferences.getBoolean("pref_privacy", false);
            final boolean pref_in_team = sharedPreferences.getBoolean("pref_in_team", false);
            if (!pref_privacy) {
              if (pref_in_team) {
                id = sharedPreferences.getString("pref_team", "");
              } else {
                id = sharedPreferences.getString("own_bssid", "");
              }
              tag = sharedPreferences.getString("pref_team_tag", "");
            }
            final Set<String> pref_support_project = sharedPreferences.getStringSet("pref_support_project", new HashSet<String>());
            int mode = 0;
            if (sharedPreferences.getBoolean("pref_public_data", true)) {
              mode = 1;
            }
            if (sharedPreferences.getBoolean("pref_publish_map", false)) {
              mode |= 2;
            }
            boolean uploaded = uploader.upload(id, tag, mode, pref_support_project);
            if (uploaded) {
              //update ranking
              RankingObject ranking = uploader.getRanking();
              Log.e(LOG_TAG, "Getting ranking ob=" + ranking.toString());
              intent = new Intent();
              intent.setAction(ACTION_UPDATE_RANKING);
              intent.putExtra(R_RANK, ranking);
              sendBroadcast(intent);
            } else {
              intent = new Intent();
              intent.setAction(ACTION_UPLOAD_ERROR);
              intent.putExtra(R_UPLOAD_MSG, uploader.getError());
              sendBroadcast(intent);
            }
          }
          Config.setMode(Config.MODE.SCAN_MODE);
          break;
        case SCAN_MODE:
          if (getLocation) {
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
        case KILL_MODE:
          Log.e(LOG_TAG, "killing service because lack of resource....");
          intent = new Intent();
          intent.setAction(ACTION_KILL_APP);
          sendBroadcast(intent);
          stopSelf();
          running = false;
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
    resourceManager = new ResourceManager(this);
    ResourceManager.lastLocationTime = lastTime;
    resourceManager.start();
    Log.i(LOG_TAG, "starting scan thread " + controller.isAlive());
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    Log.i(LOG_TAG, "destroy service");
    running = false;
    //clean up scanner
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
    //clean up storer
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
    storer.running = false;
    if (storer != null && storer.isAlive()) {
      storer.interrupt();
      try {
        storer.join();
        Log.i(LOG_TAG, "Join store thread");
      } catch (InterruptedException e) {
        Log.i(LOG_TAG, "Error join store thread");
        Thread.currentThread().interrupt();
      }
      storer = null;
    }
    //clean up resource manager
    resourceManager.running = false;
    if(resourceManager !=null && resourceManager.isAlive()){
      resourceManager.interrupt();
      try {
        resourceManager.join();
        Log.i(LOG_TAG, "Join resource thread");
      } catch (InterruptedException e) {
        Log.i(LOG_TAG, "Error join resource thread");
        Thread.currentThread().interrupt();
      }
    }
    //clean up controller
    if (controller != null && controller.isAlive()) {
      controller.interrupt();
      try {
        controller.join();
        Log.i(LOG_TAG, "Join controller thread");
      } catch (InterruptedException e) {
        Log.i(LOG_TAG, "Error join controller thread");
        Thread.currentThread().interrupt();
      }
      controller = null;
    }
    Log.e(LOG_TAG, "Finish clean up");
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private boolean qualityCheck(double lat, double lon, float radius) {
    switch (simpleWifiLocator.getLastLocMethod()) {
      case LIBWLOCATE:
        if (lastLon < OVER && lastLat < OVER) {
          float[] dist = new float[1];
          Location.distanceBetween(lastLat, lastLon, lat, lon, dist);
          lastSpeed = dist[0] * 1000 / (SystemClock.elapsedRealtime() - lastTime);
        } else {
          lastSpeed = -1f;
        }
        lastLat = lat;
        lastLon = lon;
        lastTime = SystemClock.elapsedRealtime();
        ResourceManager.lastLocationTime = lastTime;
        return true;
      case GPS:
        Log.e(LOG_TAG, "in gps");
        if (radius < MAX_RADIUS) {
          lastLat = lat;
          lastLon = lon;
          lastSpeed = simpleWifiLocator.getLastSpeed();
          Log.e(LOG_TAG, "get last speed" + lastSpeed);
          lastTime = SystemClock.elapsedRealtime();
          ResourceManager.lastLocationTime = lastTime;
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
    intent.setAction(ACTION_UPDATE_DB);
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
        final String pref_min_rssi = sharedPreferences.getString("pref_min_rssi", "");
        double limit = Double.valueOf(pref_min_rssi);
        List<ScanResult> resultList = simpleWifiLocator.getWifiScanResult();
        for (ScanResult result : resultList) {
          if (result.level > limit) {
            int channel = 0;
            try {
              channel = result.channelWidth;
            } catch (NoSuchFieldError e) {
              //Some old version can not read channelwidth field
            }
            //do Autoconnect if setting
            doAutoConnect(result);
            AccessPoint ap = new AccessPoint(
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

        }
        intent.setAction(ACTION_UPDATE_UI);
        intent.putExtra(R_GEO_INFO, lastLat + "-" + lastLon);
        intent.putExtra(R_NEWEST_SCAN, (long) resultList.size());
        intent.putExtra(R_SPEED, lastSpeed);
        intent.putParcelableArrayListExtra(R_LIST_AP, listAp);
        Log.i(LOG_TAG, "Notify ui update");
      } else {
        intent.setAction(ACTION_UPDATE_ERROR);
        lastLon = 0.0;
        lastLon = 0.0;
      }
      sendBroadcast(intent);
      //set up next scan period
      if (!sharedPreferences.getBoolean("pref_adaptive_scanning", true)) {
        SCAN_PERIOD = 2000;
      } else if (lastSpeed > 15) {
        //from  about 55km/h
        SCAN_PERIOD = 750;
      } else if (lastSpeed < 0) {
        // no speed information, may be because of WLAN localisation
        SCAN_PERIOD = 2000;
      }else if (lastSpeed < 2) {
        // user seems to walk
        SCAN_PERIOD = 3000;
      }
      //TODO detect not moving and extend scan period
      getLocation = true;
      Log.i(LOG_TAG, "Getting result###################################");
      //controller.interrupt();
    }
  }

  private void doAutoConnect(ScanResult result) {
    //TODO test + ask mentor for more information
    ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo info = manager.getActiveNetworkInfo();
    if (info == null
        || !info.isConnected()
        || info.getType()!=ConnectivityManager.TYPE_WIFI) {
      //no wifi connection yet --> do autoconnect if possible and if configured
      final boolean pref_autoconnect_freifunk = sharedPreferences.getBoolean("pref_autoconnect_freifunk", false);
      final boolean pref_autoconnect_openwifi = sharedPreferences.getBoolean("pref_autoconnect_openwifi", false);
      if( (pref_autoconnect_freifunk && WifiFilterer.isFreifunk(result))
          || (pref_autoconnect_openwifi) && WifiFilterer.isOpenWifi(result)){
        //connection now
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = result.SSID;
        configuration.hiddenSSID = true;
        configuration.status = WifiConfiguration.Status.ENABLED;
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        WifiManager wifiManager = (WifiManager) getApplicationContext()
            .getSystemService(Context.WIFI_SERVICE);
        final int id = wifiManager.addNetwork(configuration);
        if(id != -1){
          //successfully add network
          wifiManager.disconnect();
          wifiManager.enableNetwork(id, true);
          wifiManager.reconnect();
        }
      }
    }
  }
}
