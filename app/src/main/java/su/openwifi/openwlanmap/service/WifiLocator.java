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
import java.util.List;
import java.util.Locale;
import su.openwifi.openwlanmap.QueryUtils;

/**
 * Created by tali on 01.06.18.
 */

public class WifiLocator implements Runnable {
  public static final int WLOC_OK = 0;
  /**
   * Result code for position request, given position information are OK.
   */
  public static final int WLOC_REQUEST_ERROR = 1;
  /**
   * Result code for position request,
   * a connection error occured, no position information are available.
   */
  public static final int WLOC_LOCATION_ERROR = 2;
  private static final String LOG_TAG = WifiLocator.class.getSimpleName();
  /**
   * Result code for position request,
   * the position could not be evaluated, no position information are available.
   */
  private static final long WAIT_FOR_SIGNAL = 7500;
  private static final long GPS_PERIOD = 100;

  private Context context;
  private LocationManager locationManager;
  private LocationListener gpsLocationListener;
  private WifiManager wifiManager;
  private WifiScanReceiver wifiScanReceiver;
  private double lat;
  private double lon;
  private float speed;
  private long lastLocationMillis;
  private Thread netThread;
  private boolean gpsAvailable;
  private WifiLocator me;
  private LocationInfo locationInfo;
  private boolean scanStarted;
  private float radius;

  /**
   * Constructor of WifiLocator object.
   * @param context : context for WifiScanListener and LocationListener object
   */
  @SuppressLint("MissingPermission")
  //Permission is checked at starting app
  public WifiLocator(Context context) {
    this.lastLocationMillis = 0;
    this.netThread = null;
    this.locationInfo = new LocationInfo();
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
      wlocReturnPosition(WLOC_LOCATION_ERROR, 0.0, 0.0, 0.0f, (short) 0);
      return;
    }
    scanStarted = true;
    Log.i(LOG_TAG, "Starting scan...");
    wifiManager.startScan();
  }

  public LocationInfo getLocationInfo() {
    return this.locationInfo;
  }

  public Thread getNetThread() {
    return netThread;
  }

  public void setNetThread(Thread netThread) {
    this.netThread = netThread;
  }

  @Override
  public void run() {
    Log.i(LOG_TAG, "Request started");
    QueryUtils.LocationObject locationObject = QueryUtils.fetchLocationOld(
        locationInfo.requestData.bssids);
    locationInfo.lastSpeed = LocationInfo.LOC_METHOD_LIBWLOCATE;
    Log.i(LOG_TAG, "Finish request with response=" + locationObject);
    if (locationObject != null && isValidLatLng(locationObject.lat, locationObject.lon)) {
      wlocReturnPosition(WLOC_OK, locationObject.lat, locationObject.lon, 10000.0f, (short) 0);
    } else {
      wlocReturnPosition(WLOC_REQUEST_ERROR, 0.0, 0.0, (float) 0.0, (short) 0);
    }
  }

  /**
   * This method checks if the given latitude and longitude is valid or not.
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
   *               successfully or not. Only in case this parameter is equal to WLOC_OK all
   *               the other ones can be used, elsewhere no position information could be retrieved.
   * @param lat    the latitude of the current position
   * @param lon    the latitude of the current position
   * @param radius the accuracy of the position information, this radius specifies the range around
   *               the given latitude and longitude information of the real position.
   *               The smaller this value is the more accurate the given position information is.
   * @param ccode  code of the country where the current position is located within, in case the
   *               country is not known, 0 is returned. The country code can be converted
   *               to a text that specifies the country by calling wloc_get_country_from_code()
   */
  protected void wlocReturnPosition(int ret, double lat, double lon, float radius, short ccode) {
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
          locationInfo.wifiScanResult = wifiScanList;
          locationInfo.requestData = new RequestData();
          for (ScanResult scanResult : wifiScanList) {
            // some strange devices use a dot instead of :
            String bssid = scanResult.BSSID
                .toUpperCase(Locale.US)
                .replace(".", "")
                .replace(":", "");
            locationInfo.requestData.bssids.add(bssid);
          }
          locationInfo.lastLocMethod = LocationInfo.LOC_METHOD_NONE;
          locationInfo.lastSpeed = -1.0f;
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
            if (isValidLatLng(lat, lon)) {
              Log.i(LOG_TAG, "Use gps signal");
              locationInfo.lastSpeed = speed;
              locationInfo.lastLocMethod = LocationInfo.LOC_METHOD_GPS;
              wlocReturnPosition(WLOC_OK, lat, lon, radius, (short) 0);
            } else {
              Log.i(LOG_TAG, "Waiting for gps signal ...");
              wlocReturnPosition(WLOC_LOCATION_ERROR, 0.0, 0.0, (float) 0.0, (short) 0);
            }
          }

        } else {
          Log.i(LOG_TAG, "No ap seen");
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
        lat = location.getLatitude();
        lon = location.getLongitude();
        if (location.hasSpeed()) {
          speed = location.getSpeed();
        } else {
          speed = -1.0f;
        }
        if (location.hasAccuracy()) {
          radius = location.getAccuracy();
        } else {
          radius = -1.0f;
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
