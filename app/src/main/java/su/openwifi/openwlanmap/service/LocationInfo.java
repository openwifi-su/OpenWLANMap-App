package su.openwifi.openwlanmap.service;

import android.net.wifi.ScanResult;
import java.util.List;

/**
 * Created by tali on 07.06.18.
 */

public class LocationInfo {
  public static final int LOC_METHOD_NONE = 0;
  public static final int LOC_METHOD_LIBWLOCATE = 1;
  public static final int LOC_METHOD_GPS = 2;

  /**
   * describes based on which method the last location was performed with.
   */
  public int lastLocMethod = LOC_METHOD_NONE;

  /**
   * request data that is used for wifi-based location request,
   * its member bssids is filled with valid BSSIDs also in case of GPS location.
   */
  public RequestData requestData;

  /**
   * result of last WiFi-scan, this list is filled with valid data also in case of GPS location.
   */
  public List<ScanResult> wifiScanResult;

  /**
   * last movement speed in km per hour, if no speed could be evaluated the value is smaller than 0.
   */
  public float lastSpeed = -1;
}
