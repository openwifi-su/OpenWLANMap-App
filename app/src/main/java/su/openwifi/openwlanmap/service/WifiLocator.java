package su.openwifi.openwlanmap.service;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import su.openwifi.openwlanmap.QueryUtils;

/**
 * Created by tali on 01.06.18.
 */

public class WifiLocator implements Runnable {
  public enum WLOC_REPONSE_CODE  {
    OK, REQUEST_ERROR, ERROR
  }

  public enum LOC_METHOD {
    LIBWLOCATE, GPS, NOT_DEFINE
  }

  private static final String LOG_TAG = WifiLocator.class.getSimpleName();
  private static final long WAIT_FOR_SIGNAL = 7500;
  private static final long GPS_PERIOD = 100;

  private Context context;
  private LocationManager locationManager;
  private LocationListener gpsLocationListener;
  private WifiManager wifiManager;
  private WifiScanReceiver wifiScanReceiver;
  private double lastLat;
  private double lastLon;
  private float lastSpeed;
  private long lastLocationMillis;
  private Thread netThread;
  private boolean gpsAvailable;
  private WifiLocator me;
  private boolean scanStarted;
  private float lastRadius;
  private LOC_METHOD lastLocMethod;
  private Set<String> requestData;
  private List<ScanResult> wifiScanResult;

  /**
   * Constructor of WifiLocator object.
   *
   * @param context : context for WifiScanListener and LocationListener object
   */
  @SuppressLint("MissingPermission")
  //Permission is checked at starting app
  public WifiLocator(Context context) {
    this.lastLocMethod = LOC_METHOD.NOT_DEFINE;
    this.lastLocationMillis = 0;
    this.netThread = null;
    this.scanStarted = false;
    this.context = context;
    this.locationManager = (LocationManager) this.context.getSystemService(
        Context.LOCATION_SERVICE);
    this.gpsLocationListener = new GpsLocationListener();

    //TODO check permission every time
    this.locationManager.requestLocationUpdates(
        LocationManager.GPS_PROVIDER, GPS_PERIOD, 0, gpsLocationListener
    );
    this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
    this.wifiScanReceiver = new WifiScanReceiver();
    me = this;
    doRegister();
    this.gpsAvailable = this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
  }

  /**
   * This methods register the wifi scan receiver with the given context of wifi locator object.
   */
  public void doRegister() {
    this.context.registerReceiver(wifiScanReceiver,
        new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
  }

  /**
   * This methods unregister the wifi scan receiver with the given context of wifi locator object
   * in case the scan service is paused etc.
   */
  public void doUnregister() {
    try {
      context.unregisterReceiver(wifiScanReceiver);
      locationManager.removeUpdates(gpsLocationListener);
    } catch (IllegalArgumentException ex) {
      // just in case wifiReceiver is not registered yet
    }
  }

  /**
   * This methods is called by any one who want to start locating the scanned wifis
   * The result is called asynchronous by wlocReturnPosition method.
   */
  public void wlocRequestPosition() {
    if (!wifiManager.isWifiEnabled() && (!gpsAvailable)) {
      Log.i(LOG_TAG, "Can not start scan service");
      wlocReturnPosition(WLOC_REPONSE_CODE.ERROR, 0.0, 0.0, 0.0f, (short) 0);
      return;
    }
    scanStarted = true;
    Log.i(LOG_TAG, "Starting scan...");
    wifiManager.startScan();
  }

  public Thread getNetThread() {
    return netThread;
  }

  public void setNetThread(Thread netThread) {
    this.netThread = netThread;
  }

  public List<ScanResult> getWifiScanResult() {
    return wifiScanResult;
  }

  public float getLastSpeed() {
    return lastSpeed;
  }

  public LOC_METHOD getLastLocMethod() {
    return lastLocMethod;
  }

  @Override
  public void run() {
    Log.i(LOG_TAG, "Request started");
    QueryUtils.LocationObject locationObject = QueryUtils.fetchLocationOld(
        requestData);
    lastLocMethod = LOC_METHOD.LIBWLOCATE;
    Log.i(LOG_TAG, "Finish request with response=" + locationObject);
    if (locationObject != null && isValidLatLng(locationObject.lat, locationObject.lon)) {
      lastLat = locationObject.lat;
      lastLon = locationObject.lon;
      wlocReturnPosition(WLOC_REPONSE_CODE.OK, lastLat, lastLon, 0.0f, (short) 0);
    } else {
      wlocReturnPosition(WLOC_REPONSE_CODE.REQUEST_ERROR, 0.0, 0.0, (float) 0.0, (short) 0);
    }
  }

  /**
   * This method checks if the given latitude and longitude is valid or not.
   *
   * @param lat : latitude
   * @param lng : longitude
   * @return true if latitude and longitude is valid, otherwise false
   */
  public boolean isValidLatLng(double lat, double lng) {
    if (lat < -90 || lat > 90) {
      return false;
    } else if (lng < -180 || lng > 180) {
      return false;
    }
    return true;
  }

  /**
   * This method is called as soon as a result of a position evaluation request is available.
   * Thus this method should be overwritten by the inheriting class to receive the results there.
   *
   * @param ret    the return code that informs if the location evaluation request is
   *               successfully or not. Only in case this parameter is equal to OK all
   *               the other ones can be used, elsewhere no position information could be retrieved.
   * @param lat    the latitude of the current position
   * @param lon    the latitude of the current position
   * @param radius the accuracy of the position information,
   *               this lastRadius specifies the range around
   *               the given latitude and longitude information of the real position.
   *               The smaller this value is the more accurate the given position information is.
   * @param ccode  code of the country where the current position is located within, in case the
   *               country is not known, 0 is returned. The country code can be converted
   *               to a text that specifies the country by calling wloc_get_country_from_code()
   */
  protected void wlocReturnPosition(WLOC_REPONSE_CODE ret,
                                    double lat, double lon,
                                    float radius, short ccode) {
    //should be implemented by child class
  }

  private class WifiScanReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      if (scanStarted) {
        scanStarted = false;
        List<ScanResult> wifiScanList = wifiManager.getScanResults();
        if (!wifiScanList.isEmpty()) {
          Log.i(LOG_TAG, "Receiving " + wifiScanList.size() + " aps");
          wifiScanResult = wifiScanList;
          requestData = new HashSet<>();
          for (ScanResult scanResult : wifiScanList) {
            // some strange devices use a dot instead of :
            String bssid = scanResult.BSSID
                .toUpperCase(Locale.US)
                .replace(".", "")
                .replace(":", "");
            requestData.add(bssid);
          }
          lastLocMethod = LOC_METHOD.NOT_DEFINE;
          lastSpeed = -1.0f;
          if (gpsAvailable) {
            gpsAvailable = (SystemClock.elapsedRealtime() - lastLocationMillis) < WAIT_FOR_SIGNAL;
            Log.i(LOG_TAG, "Waited for " + (SystemClock.elapsedRealtime() - lastLocationMillis));
          }
          if (!gpsAvailable) {
            //no gps
            Log.i(LOG_TAG, "No gps signal. Starting request ...");
            if ((netThread != null) && (netThread.isAlive())) {
              Log.i(LOG_TAG, "Already starting request");
            }
            netThread = new Thread(me);
            netThread.start();
            Log.i(LOG_TAG, "Can starting network thread");
          } else {
            //still have/wait for gps
            if (isValidLatLng(lastLat, lastLon)) {
              Log.i(LOG_TAG, "Use gps signal");
              lastLocMethod = LOC_METHOD.GPS;
              wlocReturnPosition(WLOC_REPONSE_CODE.OK, lastLat, lastLon, lastRadius, (short) 0);
            } else {
              Log.i(LOG_TAG, "Waiting for gps signal ...");
              wlocReturnPosition(WLOC_REPONSE_CODE.ERROR, 0.0, 0.0, (float) 0.0, (short) 0);
            }
          }

        } else {
          Log.i(LOG_TAG, "No ap seen");
          wlocReturnPosition(WLOC_REPONSE_CODE.ERROR, 0.0, 0.0, (float) 0.0, (short) 0);
        }
      }
    }
  }

  private class GpsLocationListener implements LocationListener {
    @Override
    public void onLocationChanged(Location location) {
      Log.i(LOG_TAG, "Location changed");
      if (location != null) {
        lastLocationMillis = SystemClock.elapsedRealtime();
        gpsAvailable = true;
        lastLat = location.getLatitude();
        lastLon = location.getLongitude();
        if (location.hasSpeed()) {
          lastSpeed = location.getSpeed();
        } else {
          lastSpeed = -1.0f;
        }
        if (location.hasAccuracy()) {
          lastRadius = location.getAccuracy();
        } else {
          lastRadius = -1.0f;
        }
      }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle bundle) {
      if ((provider != null) && (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER))) {
        gpsAvailable = status == LocationProvider.AVAILABLE;
      }
    }

    @Override
    public void onProviderEnabled(String s) {
      gpsAvailable = true;
      Log.i(LOG_TAG, "gps available");
      Toast.makeText(context, "You now have GSP signal", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String s) {
      gpsAvailable = false;
      Log.i(LOG_TAG, "gps disable");
      Toast.makeText(context,
          "Please enable GPS. Scan service can not work without GPS",
          Toast.LENGTH_SHORT).show();
    }
  }

}
