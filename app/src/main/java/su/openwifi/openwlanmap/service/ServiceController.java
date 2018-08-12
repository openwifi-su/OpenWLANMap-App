package su.openwifi.openwlanmap.service;

import static su.openwifi.openwlanmap.MainActivity.ACTION_ASK_PERMISSION;
import static su.openwifi.openwlanmap.MainActivity.ACTION_AUTO_RANK;
import static su.openwifi.openwlanmap.MainActivity.ACTION_KILL_APP;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPDATE_DB;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPDATE_ERROR;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPDATE_RANKING;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPDATE_UI;
import static su.openwifi.openwlanmap.MainActivity.ACTION_UPLOAD_ERROR;
import static su.openwifi.openwlanmap.MainActivity.PREF_OWN_BSSID;
import static su.openwifi.openwlanmap.MainActivity.PREF_RANKING;
import static su.openwifi.openwlanmap.MainActivity.PREF_TOTAL_AP;
import static su.openwifi.openwlanmap.MainActivity.R_GEO_INFO;
import static su.openwifi.openwlanmap.MainActivity.R_LIST_AP;
import static su.openwifi.openwlanmap.MainActivity.R_NEWEST_SCAN;
import static su.openwifi.openwlanmap.MainActivity.R_PERMISSION;
import static su.openwifi.openwlanmap.MainActivity.R_SPEED;
import static su.openwifi.openwlanmap.MainActivity.R_TOTAL_LIST;
import static su.openwifi.openwlanmap.MainActivity.R_UPLOAD_MSG;
import static su.openwifi.openwlanmap.service.Config.MODE.SCAN_MODE;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import su.openwifi.openwlanmap.AccessPoint;
import su.openwifi.openwlanmap.MainActivity;
import su.openwifi.openwlanmap.R;
import su.openwifi.openwlanmap.Utils;
import su.openwifi.openwlanmap.utils.RankingObject;

/**
 * Created by tali on 01.06.18.
 */

public class ServiceController extends Service implements Runnable, Observer {
  //constant
  private static final String CHANNEL_ID = "1234";
  private static long SCAN_PERIOD = 2000;
  private static final String LOG_TAG = ServiceController.class.getSimpleName();
  private static final int BUFFER_ENTRY_MAX = 50;
  private static final float MAX_RADIUS = 98;
  private static final double OVER = 180;
  //variables
  private WifiLocator simpleWifiLocator;
  private Thread controller = null;
  private ArrayList<AccessPoint> listAp = new ArrayList<>();
  private Intent intent;
  private WifiStorer storer;
  private DataQueue buffer = new DataQueue();
  private boolean getLocation = true;
  private WifiUploader uploader;
  private TotalApWrapper totalAps;
  private SharedPreferences sharedPreferences;
  private HudView overlayView;
  private ConnectivityManager connectivityManager;
  private NotificationCompat.Builder notificationBuilder;
  private WifiLocator.LOC_METHOD lastLocMethod = WifiLocator.LOC_METHOD.NOT_DEFINE;
  private LocalBroadcastManager broadcaster;
  //pref data and static data
  public static ResourceManager resourceManager;
  public static int allowBattery;
  public static int allowNoLocation;
  public static int numberOfApToUpload;
  public static ShowCounterWrapper showCounterWrapper;
  public static String ownId;
  public static String ranking;
  public static String teamId;
  public static String tag;
  public static int mode;
  public static long totalApsCount;
  public static long lastTime = SystemClock.elapsedRealtime();
  public static boolean running = false;
  public static double lastLat = 190;
  public static double lastLon = 190;
  public static float lastSpeed = -1f;
  public static int newest = 0;

  @Override
  public void run() {
    while (running) {
      switch (Config.getMode()) {
        case UPLOAD_MODE:
          Log.i(LOG_TAG, "Uploading...");
          cleanUpData();
          notificationBuilder.setSmallIcon(R.drawable.upload_icon);
          startForeground(1, notificationBuilder.build());
          Log.e(LOG_TAG, "upload data="
              + ownId + "-team=" + teamId + "mode=" + mode + "tag=" + tag);
          if (!checkConnection()) {
            intent = new Intent();
            intent.setAction(ACTION_UPLOAD_ERROR);
            intent.putExtra(R_UPLOAD_MSG, getString(R.string.connect_error));
            broadcaster.sendBroadcast(intent);
          } else {
            boolean uploaded = uploader.upload();
            if (uploaded) {
              //update ranking
              RankingObject rankingObject = uploader.getRanking();
              Log.e(LOG_TAG, "Getting ranking ob=" + rankingObject.toString());
              ranking = rankingObject.uploadedRank
                  + "(" + rankingObject.uploadedCount + " "
                  + getString(R.string.point) + ")";
              intent = new Intent();
              intent.setAction(ACTION_UPDATE_RANKING);
              String msg = uploader.getMsg()
                  + "\n" + getString(R.string.newRank)
                  + "\n" + getString(R.string.upCount) + rankingObject.uploadedCount
                  + "\n" + getString(R.string.upRank) + rankingObject.uploadedRank
                  + "\n" + getString(R.string.upNewAp) + rankingObject.newAps
                  + "\n" + getString(R.string.upUpdAp) + rankingObject.updAps
                  + "\n" + getString(R.string.upDelAp) + rankingObject.delAps
                  + "\n" + getString(R.string.upNewPoint) + rankingObject.newPoints;
              intent.putExtra(R_UPLOAD_MSG, msg);
              broadcaster.sendBroadcast(intent);
            } else {
              intent = new Intent();
              intent.setAction(ACTION_UPLOAD_ERROR);
              intent.putExtra(R_UPLOAD_MSG, uploader.getMsg());
              broadcaster.sendBroadcast(intent);
            }
          }
          Config.setMode(SCAN_MODE);
          notificationBuilder.setSmallIcon(R.drawable.scan_icon);
          startForeground(1, notificationBuilder.build());
          break;
        case AUTO_UPLOAD_MODE:
          Log.i(LOG_TAG, "Auto Uploading...");
          //trigger upload
          //clean up unsaved data
          cleanUpData();
          notificationBuilder.setSmallIcon(R.drawable.upload_icon);
          startForeground(1, notificationBuilder.build());
          final long start = System.currentTimeMillis();
          Log.e(LOG_TAG, "trigger=" + numberOfApToUpload + "/" + totalAps.getTotalAps());
          while (totalAps.getTotalAps() >= numberOfApToUpload
              && (System.currentTimeMillis() - start) < 30 * 1000) {
            if (canTrigger()) {
              //do uploading
              if (uploader.upload()) {
                RankingObject rankingObject = uploader.getRanking();
                Log.e(LOG_TAG, "Getting ranking ob=" + rankingObject.toString());
                ranking = rankingObject.uploadedRank
                    + "(" + rankingObject.uploadedCount + " "
                    + getString(R.string.point) + ")";
                intent = new Intent();
                intent.setAction(ACTION_AUTO_RANK);
                broadcaster.sendBroadcast(intent);
              }
            }
            Log.e(LOG_TAG, "time=" + (System.currentTimeMillis() - start));
            try {
              Thread.sleep(5000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          Config.setMode(SCAN_MODE);
          notificationBuilder.setSmallIcon(R.drawable.scan_icon);
          startForeground(1, notificationBuilder.build());
          Log.e(LOG_TAG, "finish autoupload and reset scan mode");
          break;
        case SCAN_MODE:
          if (getLocation) {
            getLocation = false;
            Log.i(LOG_TAG, "Scanning thread is running...");
            simpleWifiLocator.wlocRequestPosition();
            try {
              Thread.sleep(SCAN_PERIOD);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
          break;
        case KILL_MODE:
          Log.e(LOG_TAG, "killing service because lack of resource....");
          intent = new Intent();
          intent.setAction(ACTION_KILL_APP);
          broadcaster.sendBroadcast(intent);
          stopSelf();
          running = false;
          break;
        default:
          break;
      }
    }
  }

  private boolean checkConnection() {
    ConnectivityManager manager = (ConnectivityManager)
        getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo info = manager.getActiveNetworkInfo();
    return (info != null && info.isConnected());
  }

  private void cleanUpData() {
    while (!getLocation) {
      Log.i(LOG_TAG, "Waiting for wlocator to finish job = " + getLocation);
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    buffer.putNextData(listAp);
    while (!buffer.isEmpty()) {
      Log.i(LOG_TAG, "Waiting for storage to finish job");
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Log.i(LOG_TAG, "Now uploading......");
  }

  @Override
  public void onCreate() {
    Log.i(LOG_TAG, "create service");
    while (simpleWifiLocator == null) {
      simpleWifiLocator = new SimpleWifiLocator(this);
    }
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    connectivityManager = (ConnectivityManager)
        getSystemService(Context.CONNECTIVITY_SERVICE);
    iniNotification();
    //get from  pref
    ownId = sharedPreferences.getString(PREF_OWN_BSSID, "");
    totalApsCount = sharedPreferences.getLong(PREF_TOTAL_AP, 0L);
    ranking = sharedPreferences.getString(PREF_RANKING, "");
    teamId = "";
    final boolean pref_in_team = sharedPreferences.getBoolean("pref_in_team", false);
    if (pref_in_team) {
      teamId = sharedPreferences.getString("pref_team", "");
    }
    tag = sharedPreferences.getString("pref_team_tag", "");
    //final Set<String> pref_support_project = sharedPreferences
    //  .getStringSet("pref_support_project", new HashSet<String>());
    mode = 0;
    if (sharedPreferences.getBoolean("pref_public_data", true)) {
      mode = 1;
    }
    if (sharedPreferences.getBoolean("pref_publish_map", false)) {
      mode |= 2;
    }
    allowBattery = Integer.parseInt(sharedPreferences.getString("pref_battery", ""));
    allowNoLocation = Integer.parseInt(sharedPreferences.getString("pref_kill_ap_no_gps", ""));
    final int pref_upload = Integer.parseInt(sharedPreferences.getString("pref_upload_mode", ""));
    final boolean pref_show_counter = sharedPreferences.getBoolean("pref_show_counter", false);
    final int pref_upload_entry = Integer.parseInt(
        sharedPreferences.getString("pref_upload_entry", "5000"));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      while (!Settings.canDrawOverlays(this)) {
        Intent intent = new Intent();
        intent.setAction(ACTION_ASK_PERMISSION);
        intent.putExtra(R_PERMISSION, Utils.REQUEST_OVERLAY);
        broadcaster.sendBroadcast(intent);
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      iniOverlayView(totalApsCount);
    } else {
      iniOverlayView(totalApsCount);
    }
    totalAps = new TotalApWrapper();
    totalAps.addObserver(this);
    showCounterWrapper = new ShowCounterWrapper(pref_show_counter);
    showCounterWrapper.addObserver(this);
    controller = new Thread(this);
    storer = new WifiStorer(this, buffer, totalAps);
    uploader = new WifiUploader(this, totalAps);
    if (allowNoLocation != 0 || allowBattery != 0) {
      resourceManager = new ResourceManager(this);
      resourceManager.start();
    }
    if (pref_upload != 0) {
      numberOfApToUpload = pref_upload_entry;
    } else {
      numberOfApToUpload = -1;
    }
    broadcaster = LocalBroadcastManager.getInstance(this);
    running = true;
    controller.start();
    storer.start();
  }

  private void iniNotification() {
    Intent intent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
    notificationBuilder =
        new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.scan_icon)
            .setContentTitle(getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE);
    startForeground(1, notificationBuilder.build());
  }

  private void iniOverlayView(long value) {
    WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    overlayView = new HudView(this, sharedPreferences);
    overlayView.setValue(value);
    overlayView.invalidate();

    //TODO most of flags are deprecated --> find alternatives
    WindowManager.LayoutParams params = new WindowManager
        .LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
        PixelFormat.TRANSLUCENT);
    params.gravity = Gravity.LEFT | Gravity.TOP;
    windowManager.addView(overlayView, params);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(LOG_TAG, "on start command service");
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
    if (storer != null && storer.isAlive()) {
      storer.running = false;
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
    if (resourceManager != null && resourceManager.isAlive()) {
      resourceManager.running = false;
      resourceManager.interrupt();
      try {
        resourceManager.join();
        Log.i(LOG_TAG, "Join resource thread");
      } catch (InterruptedException e) {
        Log.i(LOG_TAG, "Error join resource thread");
        Thread.currentThread().interrupt();
      }
    }
    resourceManager = null;
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
    if (overlayView != null) {
      ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(overlayView);
      overlayView = null;
    }
    Utils.addPreference(sharedPreferences, PREF_RANKING, ranking);
    Utils.addPreferenceLong(sharedPreferences, PREF_TOTAL_AP, totalApsCount);
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
          if (lastSpeed > 140) {
            //>500km/h --> error
            lastSpeed = -1f;
          }
        } else {
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
          Log.e(LOG_TAG, "get last speed" + lastSpeed);
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
    Log.e(LOG_TAG, "update");
    if (arg == null) {
      Log.e(LOG_TAG, "update ap count");
      totalApsCount = totalAps.getTotalAps();
      intent = new Intent();
      intent.setAction(ACTION_UPDATE_DB);
      intent.putExtra(R_TOTAL_LIST, totalApsCount);
      broadcaster.sendBroadcast(intent);
    }
    overlayView.setValue(totalApsCount);
    overlayView.setMode(lastLocMethod);
    overlayView.postInvalidate();
  }

  private boolean canTrigger() {
    NetworkInfo info = connectivityManager.getActiveNetworkInfo();
    if (info != null && info.isConnected()) {
      final String pref_upload_mode = sharedPreferences.getString("pref_upload_mode", "0");
      if (pref_upload_mode.equalsIgnoreCase("1")
          || (pref_upload_mode.equalsIgnoreCase("2")
          && info.getType() == ConnectivityManager.TYPE_WIFI)
          ) {
        return true;
      }
    }
    return false;
  }

  public class SimpleWifiLocator extends WifiLocator {

    public SimpleWifiLocator(Context context) {
      super(context);
    }

    @Override
    protected void wlocReturnPosition(WLOC_REPONSE_CODE ret,
                                      double lat,
                                      double lon,
                                      float radius,
                                      short ccode) {
      Log.i(LOG_TAG, "Getting back lat-lon = " + lat + "-" + lon);
      if (lastLocMethod != simpleWifiLocator.getLastLocMethod()) {
        //change color of overlay if loc method changes
        lastLocMethod = simpleWifiLocator.getLastLocMethod();
        overlayView.setMode(lastLocMethod);
        overlayView.postInvalidate();
      }
      intent = new Intent();
      if (ret == WLOC_REPONSE_CODE.OK && qualityCheck(lat, lon, radius)) {
        final String pref_min_rssi = sharedPreferences.getString("pref_min_rssi", "-1000");
        double limit = Double.valueOf(pref_min_rssi);
        Log.e(LOG_TAG, "rssid=" + limit);
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
        newest = resultList.size();
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
      broadcaster.sendBroadcast(intent);
      //set up next scan period
      if (!sharedPreferences.getBoolean("pref_adaptive_scanning", true)) {
        SCAN_PERIOD = 2000;
      } else if (lastSpeed > 15) {
        //from  about 55km/h
        SCAN_PERIOD = 750;
      } else if (lastSpeed < 0) {
        // no speed information, may be because of WLAN localisation
        SCAN_PERIOD = 2000;
      } else if (lastSpeed < 2) {
        // user seems to walk
        SCAN_PERIOD = 3000;
      } else if (lastSpeed < 0.5) {
        //user seems to stay
        SCAN_PERIOD = 5000;
      }
      //TODO detect not moving and extend scan period
      Log.e(LOG_TAG, "print out=" + numberOfApToUpload + "/" + totalApsCount);
      if (totalApsCount >= numberOfApToUpload && canTrigger() && Config.getMode() == SCAN_MODE) {
        Config.setMode(Config.MODE.AUTO_UPLOAD_MODE);
      }
      getLocation = true;
      Log.i(LOG_TAG, "Getting result###################################");
    }

  }

  private void doAutoConnect(ScanResult result) {
    ConnectivityManager manager = (ConnectivityManager)
        getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo info = manager.getActiveNetworkInfo();
    if (info == null
        || !info.isConnected()
        || info.getType() != ConnectivityManager.TYPE_WIFI) {
      //no wifi connection yet
      final boolean pref_autoconnect_freifunk = sharedPreferences
          .getBoolean("pref_autoconnect_freifunk", false);
      final boolean pref_autoconnect_openwifi = sharedPreferences
          .getBoolean("pref_autoconnect_openwifi", false);
      if ((pref_autoconnect_freifunk && WifiFilterer.isFreifunk(result))
          || (pref_autoconnect_openwifi) && WifiFilterer.isOpenWifi(result)) {
        //connection now
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = "\"" + result.SSID + "\"";
        configuration.hiddenSSID = true;
        configuration.status = WifiConfiguration.Status.ENABLED;
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        WifiManager wifiManager = (WifiManager) getApplicationContext()
            .getSystemService(Context.WIFI_SERVICE);
        final int id = wifiManager.addNetwork(configuration);
        if (id != -1) {
          //successfully add network
          wifiManager.disconnect();
          wifiManager.enableNetwork(id, true);
          wifiManager.reconnect();
        }
      }
    }
  }
}
