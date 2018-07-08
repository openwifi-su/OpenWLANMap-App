package su.openwifi.openwlanmap.service;

import android.net.wifi.ScanResult;

/**
 * This class contains all the filter for access point.
 */

public class WifiFilterer {
  public static final String[] mobilHotspot = {
      "iphone",              // mobile AP
      "ipad",            // mobile AP
      "android",         // mobile AP
      "motorola",       // mobile AP
      "deinbus.de",     // WLAN network on board of German bus
      "db ic bus",      // WLAN network on board of German bus
      "fernbus",        // WLAN network on board of German bus
      "flixbus",         // WLAN network on board of German bus
      "flixmedia",       // WLAN network on board of German bus
      "postbus",        // WLAN network on board of bus line
      "megabus",        // WLAN network on board of bus line
      "ecolines",       // WLAN network on board of German bus
      "eurolines",      // WLAN network on board of German bus
      "contiki-wifi",  // WLAN network on board of bus
      "muenchenlinie",   // WLAN network on board of bus
      "guest@ms ",      // WLAN network on Hurtigruten ships
      "admin@ms ",      // WLAN network on Hurtigruten ships
      "mobile hotspot",  // e.g. BlackBerry devices
      "mobilewifi",     // e.g. some Vodafone devices
      "portable hotspot", // e.g. HTC devices
      "nokia lumia",    // portable hotspot
      "telekom_ice",    // WLAN network on DB trains
      "nsb_interakti",   // WLAN network in NSB trains)
      "mobyclick"         // hotspot in bus in Hamburg
  };

  private WifiFilterer() {
  }

  /**
   * This methods checks if the scan result is an open wifi.
   *
   * @param scanResult : ScanResult object
   * @return true if it is open wifi, otherwise false
   */
  public static boolean isOpenWifi(ScanResult scanResult) {
    return ((!scanResult.capabilities.contains("WEP"))
        && (!scanResult.capabilities.contains("WPA"))
        && (!scanResult.capabilities.contains("TKIP"))
        && (!scanResult.capabilities.contains("CCMP"))
        && (!scanResult.capabilities.contains("PSK")));
  }

  /**
   * This methods checks if the scan result is free hotspot.
   *
   * @param scanResult : ScanResult object
   * @return true if it is free hotspot, otherwise false
   */
  public static boolean isFreeHotspot(ScanResult scanResult) {
    if (isOpenWifi(scanResult)) {
      String ssid = scanResult.SSID.toLowerCase();
      if (ssid.contains("free-hotspot.com")
          || ssid.contains("the cloud")) {
        return true;
      }
    }
    return false;
  }

  /**
   * This methods checks if the scan result is from freifunk.
   *
   * @param scanResult : ScanResult object
   * @return true if it is from freifunk, otherwise false
   */
  public static boolean isFreifunk(ScanResult scanResult) {
    return scanResult.SSID.toLowerCase().contains("freifunk");
  }

  /**
   * This methods checks if the scan result is marked with _nomap.
   *
   * @param scanResult : ScanResult object
   * @return true if it is marked, otherwise false
   */
  public static boolean isNoMapMarked(ScanResult scanResult) {
    return scanResult.SSID.endsWith("_nomap");
  }

  /**
   * This methods checks if the scan result is a mobil hotspot.
   *
   * @param scanResult : ScanResult object
   * @return true if it is a mobil hotspot, otherwise false
   */
  public static boolean isMobilHotspot(ScanResult scanResult) {
    if (scanResult.SSID.startsWith("Audi") || scanResult.SSID.startsWith("Volkswagen")) {
      return true;
    }
    String lowerSsid = scanResult.SSID.toLowerCase();
    for (String hotspot : mobilHotspot) {
      if (lowerSsid.contains(hotspot)) {
        return true;
      }
    }
    return false;
  }

  /**
   * This methods checks if the scan result is a non-infrastructure wifi access point.
   *
   * @param scanResult : ScanResult object
   * @return true if it is non-infrastructure access point, otherwise false
   */
  public static boolean isNonInfrastructureNetwork(ScanResult scanResult) {
    return false;
  }

  /**
   * This methods checks if the access point should be updated or deleted from backend.
   * @param scanResult : ScanResult object.
   * @return true if it should be updated, otherwise false
   */
  public static boolean isToUpdate(ScanResult scanResult) {
    return !(isNoMapMarked(scanResult) || isMobilHotspot(scanResult)
        || isNonInfrastructureNetwork(scanResult));
  }

}
